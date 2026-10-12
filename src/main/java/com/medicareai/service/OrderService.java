package com.medicareai.service;

import com.medicareai.dto.common.PageResponse;
import com.medicareai.dto.order.CheckoutRequest;
import com.medicareai.dto.order.OrderItemResponse;
import com.medicareai.dto.order.OrderResponse;
import com.medicareai.dto.order.OrderStatusHistoryResponse;
import com.medicareai.dto.user.DeliveryAddressResponse;
import com.medicareai.entity.*;
import com.medicareai.enums.*;
import com.medicareai.exception.BadRequestException;
import com.medicareai.exception.InventoryException;
import com.medicareai.exception.ResourceNotFoundException;
import com.medicareai.exception.UnauthorizedException;
import com.medicareai.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final CustomerOrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final PrescriptionOrderLinkRepository linkRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final DeliveryAddressRepository addressRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final MedicineBatchRepository batchRepository;
    private final StockMovementRepository movementRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public OrderResponse checkout(Long userId, CheckoutRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Shopping cart is empty"));

        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());
        if (cartItems.isEmpty()) {
            throw new BadRequestException("Cannot checkout an empty shopping cart");
        }

        DeliveryAddress address = addressRepository.findByIdAndUserId(request.getDeliveryAddressId(), userId)
                .orElseThrow(() -> new BadRequestException("Invalid delivery address selected"));

        LocalDate today = LocalDate.now();
        boolean prescriptionNeeded = false;

        // 1. Verify availability and prescription requirement
        for (CartItem ci : cartItems) {
            Medicine med = ci.getMedicine();
            if (!med.isActive()) {
                throw new BadRequestException("Medicine '" + med.getName() + "' is currently unavailable");
            }

            int availableQty = batchRepository.getAvailableStockForMedicine(med.getId(), today);
            if (availableQty < ci.getQuantity()) {
                throw new InventoryException("Insufficient stock for '" + med.getName() +
                        "'. Requested: " + ci.getQuantity() + ", Available: " + availableQty);
            }

            if (med.isRequiresPrescription()) {
                prescriptionNeeded = true;
            }
        }

        // 2. Validate prescription requirements
        List<Prescription> verifiedPrescriptions = new ArrayList<>();
        if (prescriptionNeeded) {
            if (request.getPrescriptionIds() == null || request.getPrescriptionIds().isEmpty()) {
                throw new BadRequestException("One or more items require an approved prescription. Please attach an approved prescription.");
            }

            for (Long prescId : request.getPrescriptionIds()) {
                Prescription p = prescriptionRepository.findByIdAndUserId(prescId, userId)
                        .orElseThrow(() -> new BadRequestException("Invalid prescription ID: " + prescId));

                if (p.getStatus() != PrescriptionStatus.APPROVED) {
                    throw new BadRequestException("Prescription '" + p.getOriginalFileName() +
                            "' is not yet approved (Current status: " + p.getStatus() + "). Only approved prescriptions can be used for checkout.");
                }
                verifiedPrescriptions.add(p);
            }
        }

        // 3. Generate Order Number
        String orderNumber = "MED-" + System.currentTimeMillis() + "-" + (1000 + new Random().nextInt(9000));

        // 4. Create Order entity
        CustomerOrder order = CustomerOrder.builder()
                .orderNumber(orderNumber)
                .user(user)
                .deliveryAddressId(address.getId())
                .recipientName(address.getRecipientName())
                .phone(address.getPhone())
                .addressLine1(address.getAddressLine1())
                .addressLine2(address.getAddressLine2())
                .city(address.getCity())
                .state(address.getState())
                .postalCode(address.getPostalCode())
                .country(address.getCountry())
                .orderStatus(OrderStatus.PLACED)
                .paymentStatus(PaymentStatus.PENDING)
                .customerNotes(request.getCustomerNotes())
                .build();

        CustomerOrder savedOrder = orderRepository.save(order);

        // 5. Concurrency-safe FEFO batch allocation & OrderItem creation
        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> savedItems = new ArrayList<>();

        for (CartItem ci : cartItems) {
            Medicine med = ci.getMedicine();
            int remainingToAllocate = ci.getQuantity();

            List<MedicineBatch> eligibleBatches = batchRepository.findEligibleBatchesForSale(med.getId(), today);
            if (eligibleBatches.isEmpty()) {
                throw new InventoryException("Stock depleted for " + med.getName());
            }

            // Standard selling price for snapshot
            BigDecimal unitPrice = eligibleBatches.get(0).getSellingPrice();
            BigDecimal itemTotal = unitPrice.multiply(BigDecimal.valueOf(ci.getQuantity()));
            subtotal = subtotal.add(itemTotal);

            // Allocate across FEFO batches
            for (MedicineBatch b : eligibleBatches) {
                if (remainingToAllocate <= 0) break;

                int availableInBatch = b.getAvailableQuantity();
                int takeFromBatch = Math.min(availableInBatch, remainingToAllocate);

                b.setAvailableQuantity(availableInBatch - takeFromBatch);
                batchRepository.save(b);

                // Record Stock Movement (SALE / ALLOCATED)
                StockMovement movement = StockMovement.builder()
                        .batch(b)
                        .movementType(StockMovementType.SALE)
                        .quantity(takeFromBatch)
                        .referenceType("CUSTOMER_ORDER")
                        .referenceId(savedOrder.getOrderNumber())
                        .notes("Order placement allocation")
                        .createdByUserId(userId)
                        .build();
                movementRepository.save(movement);

                // Create OrderItem
                OrderItem oi = OrderItem.builder()
                        .order(savedOrder)
                        .medicine(med)
                        .batch(b)
                        .medicineNameSnapshot(med.getName())
                        .brandSnapshot(med.getBrand())
                        .unitPrice(unitPrice)
                        .quantity(takeFromBatch)
                        .lineTotal(unitPrice.multiply(BigDecimal.valueOf(takeFromBatch)))
                        .build();
                savedItems.add(orderItemRepository.save(oi));

                remainingToAllocate -= takeFromBatch;
            }

            if (remainingToAllocate > 0) {
                throw new InventoryException("Concurrent stock allocation failed for " + med.getName());
            }
        }

        // 6. Calculate delivery fee & totals
        BigDecimal deliveryFee = (subtotal.compareTo(BigDecimal.ZERO) > 0 && subtotal.compareTo(BigDecimal.valueOf(500)) < 0)
                ? BigDecimal.valueOf(50)
                : BigDecimal.ZERO;
        BigDecimal totalAmount = subtotal.add(deliveryFee);

        savedOrder.setSubtotal(subtotal);
        savedOrder.setDeliveryFee(deliveryFee);
        savedOrder.setTotalAmount(totalAmount);
        orderRepository.save(savedOrder);

        // 7. Link Prescriptions
        for (Prescription p : verifiedPrescriptions) {
            PrescriptionOrderLink link = PrescriptionOrderLink.builder()
                    .order(savedOrder)
                    .prescription(p)
                    .build();
            linkRepository.save(link);
        }

        // 8. Record OrderStatusHistory
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(savedOrder)
                .previousStatus(null)
                .newStatus(OrderStatus.PLACED)
                .changedByUserId(userId)
                .notes("Order placed successfully by customer")
                .build();
        historyRepository.save(history);

        // 9. Clear Cart
        cartItemRepository.deleteByCartId(cart.getId());

        // 10. Send Notification & Audit
        notificationService.createNotification(userId, "Order Placed: " + savedOrder.getOrderNumber(),
                "Your order #" + savedOrder.getOrderNumber() + " for ₹" + totalAmount +
                        " has been placed successfully. Awaiting payment/confirmation.",
                "ORDER");

        auditLogService.log(userId, "ORDER_PLACED", "CustomerOrder", savedOrder.getId(),
                "Order placed: " + savedOrder.getOrderNumber() + ", Total: ₹" + totalAmount);

        return mapToResponse(savedOrder, savedItems, verifiedPrescriptions.stream().map(Prescription::getId).collect(Collectors.toList()));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::enrichOrderResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId, Long userId, boolean isStaff) {
        CustomerOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (!isStaff && !order.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Access denied to this order");
        }

        return enrichOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getAllOrders(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<CustomerOrder> pageResult = orderRepository.findAllByOrderByCreatedAtDesc(pageable);

        List<OrderResponse> content = pageResult.getContent().stream()
                .map(this::enrichOrderResponse)
                .collect(Collectors.toList());

        return PageResponse.<OrderResponse>builder()
                .content(content)
                .pageNumber(pageResult.getNumber())
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .build();
    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId, Long userId, String reason, boolean isStaff) {
        CustomerOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (!isStaff && !order.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Access denied to this order");
        }

        if (order.getOrderStatus() == OrderStatus.SHIPPED ||
            order.getOrderStatus() == OrderStatus.OUT_FOR_DELIVERY ||
            order.getOrderStatus() == OrderStatus.DELIVERED ||
            order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Order cannot be cancelled at stage: " + order.getOrderStatus());
        }

        OrderStatus prevStatus = order.getOrderStatus();
        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setCancellationReason(reason);
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            order.setPaymentStatus(PaymentStatus.REFUNDED);
        }
        orderRepository.save(order);

        // Release allocated inventory back to batches
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        for (OrderItem oi : items) {
            if (oi.getBatch() != null) {
                MedicineBatch batch = oi.getBatch();
                batch.setAvailableQuantity(batch.getAvailableQuantity() + oi.getQuantity());
                batchRepository.save(batch);

                StockMovement movement = StockMovement.builder()
                        .batch(batch)
                        .movementType(StockMovementType.RETURN)
                        .quantity(oi.getQuantity())
                        .referenceType("ORDER_CANCELLATION")
                        .referenceId(order.getOrderNumber())
                        .notes("Stock restored due to order cancellation")
                        .createdByUserId(userId)
                        .build();
                movementRepository.save(movement);
            }
        }

        // Record Status History
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .previousStatus(prevStatus)
                .newStatus(OrderStatus.CANCELLED)
                .changedByUserId(userId)
                .notes("Cancelled: " + reason)
                .build();
        historyRepository.save(history);

        notificationService.createNotification(order.getUser().getId(), "Order Cancelled: " + order.getOrderNumber(),
                "Your order #" + order.getOrderNumber() + " has been cancelled. " + reason,
                "ORDER");

        auditLogService.log(userId, "ORDER_CANCELLED", "CustomerOrder", order.getId(),
                "Cancelled order " + order.getOrderNumber() + ": " + reason);

        return enrichOrderResponse(order);
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, OrderStatus newStatus, String notes, Long staffUserId) {
        CustomerOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        OrderStatus current = order.getOrderStatus();
        if (current == OrderStatus.CANCELLED || current == OrderStatus.DELIVERED) {
            throw new BadRequestException("Cannot update order that is already " + current);
        }

        // Validate state transitions
        if (!isValidTransition(current, newStatus)) {
            throw new BadRequestException("Invalid status transition from " + current + " to " + newStatus);
        }

        order.setOrderStatus(newStatus);
        orderRepository.save(order);

        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .previousStatus(current)
                .newStatus(newStatus)
                .changedByUserId(staffUserId)
                .notes(notes != null ? notes : "Status updated to " + newStatus)
                .build();
        historyRepository.save(history);

        notificationService.createNotification(order.getUser().getId(), "Order Status Updated",
                "Your order #" + order.getOrderNumber() + " is now " + newStatus + ".",
                "ORDER");

        auditLogService.log(staffUserId, "ORDER_STATUS_UPDATED", "CustomerOrder", order.getId(),
                "Updated status to " + newStatus + " for order " + order.getOrderNumber());

        return enrichOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderStatusHistoryResponse> getOrderHistory(Long orderId, Long userId, boolean isStaff) {
        CustomerOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (!isStaff && !order.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Access denied to this order");
        }

        return historyRepository.findByOrderIdOrderByCreatedAtAsc(orderId).stream()
                .map(h -> OrderStatusHistoryResponse.builder()
                        .id(h.getId())
                        .previousStatus(h.getPreviousStatus())
                        .newStatus(h.getNewStatus())
                        .changedByUserId(h.getChangedByUserId())
                        .notes(h.getNotes())
                        .createdAt(h.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    private boolean isValidTransition(OrderStatus from, OrderStatus to) {
        if (to == OrderStatus.CANCELLED) return from != OrderStatus.DELIVERED && from != OrderStatus.SHIPPED;
        return switch (from) {
            case PENDING_PAYMENT -> to == OrderStatus.PLACED || to == OrderStatus.CONFIRMED;
            case PLACED -> to == OrderStatus.CONFIRMED || to == OrderStatus.PROCESSING;
            case CONFIRMED -> to == OrderStatus.PROCESSING;
            case PROCESSING -> to == OrderStatus.PACKED;
            case PACKED -> to == OrderStatus.SHIPPED;
            case SHIPPED -> to == OrderStatus.OUT_FOR_DELIVERY;
            case OUT_FOR_DELIVERY -> to == OrderStatus.DELIVERED;
            default -> false;
        };
    }

    private OrderResponse enrichOrderResponse(CustomerOrder order) {
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        List<Long> prescIds = linkRepository.findByOrderId(order.getId()).stream()
                .map(l -> l.getPrescription().getId())
                .collect(Collectors.toList());
        return mapToResponse(order, items, prescIds);
    }

    private OrderResponse mapToResponse(CustomerOrder o, List<OrderItem> items, List<Long> prescIds) {
        DeliveryAddressResponse addr = DeliveryAddressResponse.builder()
                .id(o.getDeliveryAddressId())
                .recipientName(o.getRecipientName())
                .phone(o.getPhone())
                .addressLine1(o.getAddressLine1())
                .addressLine2(o.getAddressLine2())
                .city(o.getCity())
                .state(o.getState())
                .postalCode(o.getPostalCode())
                .country(o.getCountry())
                .build();

        List<OrderItemResponse> itemDtos = items.stream().map(i -> OrderItemResponse.builder()
                .id(i.getId())
                .medicineId(i.getMedicine().getId())
                .medicineName(i.getMedicineNameSnapshot())
                .brand(i.getBrandSnapshot())
                .unitPrice(i.getUnitPrice())
                .quantity(i.getQuantity())
                .lineTotal(i.getLineTotal())
                .batchId(i.getBatch() != null ? i.getBatch().getId() : null)
                .batchNumber(i.getBatch() != null ? i.getBatch().getBatchNumber() : null)
                .build()).collect(Collectors.toList());

        return OrderResponse.builder()
                .id(o.getId())
                .orderNumber(o.getOrderNumber())
                .userId(o.getUser().getId())
                .customerName(o.getUser().getFullName())
                .customerEmail(o.getUser().getEmail())
                .orderStatus(o.getOrderStatus())
                .paymentStatus(o.getPaymentStatus())
                .subtotal(o.getSubtotal())
                .deliveryFee(o.getDeliveryFee())
                .discountAmount(o.getDiscountAmount())
                .totalAmount(o.getTotalAmount())
                .customerNotes(o.getCustomerNotes())
                .cancellationReason(o.getCancellationReason())
                .deliveryAddress(addr)
                .items(itemDtos)
                .prescriptionIds(prescIds)
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .build();
    }
}

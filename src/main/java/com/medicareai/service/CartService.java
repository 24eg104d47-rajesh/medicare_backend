package com.medicareai.service;

import com.medicareai.dto.cart.AddToCartRequest;
import com.medicareai.dto.cart.CartItemResponse;
import com.medicareai.dto.cart.CartResponse;
import com.medicareai.entity.Cart;
import com.medicareai.entity.CartItem;
import com.medicareai.entity.Medicine;
import com.medicareai.entity.MedicineBatch;
import com.medicareai.entity.User;
import com.medicareai.exception.BadRequestException;
import com.medicareai.exception.ResourceNotFoundException;
import com.medicareai.repository.CartItemRepository;
import com.medicareai.repository.CartRepository;
import com.medicareai.repository.MedicineBatchRepository;
import com.medicareai.repository.MedicineRepository;
import com.medicareai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final MedicineRepository medicineRepository;
    private final MedicineBatchRepository batchRepository;
    private final UserRepository userRepository;

    @Transactional
    public Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
            Cart cart = Cart.builder().user(user).build();
            return cartRepository.save(cart);
        });
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());

        List<CartItemResponse> itemResponses = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        boolean hasPrescriptionRequired = false;
        LocalDate today = LocalDate.now();

        for (CartItem item : items) {
            Medicine medicine = item.getMedicine();
            List<MedicineBatch> batches = batchRepository.findEligibleBatchesForSale(medicine.getId(), today);

            int availableStock = batches.stream()
                    .mapToInt(MedicineBatch::getAvailableQuantity)
                    .sum();

            BigDecimal unitPrice = batches.isEmpty() ? BigDecimal.ZERO : batches.get(0).getSellingPrice();
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));

            subtotal = subtotal.add(lineTotal);
            if (medicine.isRequiresPrescription()) {
                hasPrescriptionRequired = true;
            }

            itemResponses.add(CartItemResponse.builder()
                    .id(item.getId())
                    .medicineId(medicine.getId())
                    .medicineName(medicine.getName())
                    .brand(medicine.getBrand())
                    .dosageForm(medicine.getDosageForm())
                    .strength(medicine.getStrength())
                    .productImageUrl(medicine.getProductImageUrl())
                    .requiresPrescription(medicine.isRequiresPrescription())
                    .unitPrice(unitPrice)
                    .quantity(item.getQuantity())
                    .lineTotal(lineTotal)
                    .availableStock(availableStock)
                    .build());
        }

        // Delivery fee: ₹50 if subtotal < ₹500, else Free (₹0)
        BigDecimal deliveryFee = (subtotal.compareTo(BigDecimal.ZERO) > 0 && subtotal.compareTo(BigDecimal.valueOf(500)) < 0)
                ? BigDecimal.valueOf(50)
                : BigDecimal.ZERO;

        BigDecimal totalAmount = subtotal.add(deliveryFee);

        return CartResponse.builder()
                .id(cart.getId())
                .items(itemResponses)
                .subtotal(subtotal)
                .deliveryFee(deliveryFee)
                .totalAmount(totalAmount)
                .hasPrescriptionRequiredItems(hasPrescriptionRequired)
                .build();
    }

    @Transactional
    public CartResponse addToCart(Long userId, AddToCartRequest request) {
        if (request.getQuantity() <= 0) {
            throw new BadRequestException("Quantity must be greater than zero");
        }

        Medicine medicine = medicineRepository.findByIdAndActiveTrue(request.getMedicineId())
                .orElseThrow(() -> new ResourceNotFoundException("Active medicine not found: " + request.getMedicineId()));

        Cart cart = getOrCreateCart(userId);
        Optional<CartItem> existingItem = cartItemRepository.findByCartIdAndMedicineId(cart.getId(), medicine.getId());

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
            cartItemRepository.save(item);
        } else {
            CartItem item = CartItem.builder()
                    .cart(cart)
                    .medicine(medicine)
                    .quantity(request.getQuantity())
                    .build();
            cartItemRepository.save(item);
        }

        return getCart(userId);
    }

    @Transactional
    public CartResponse updateCartItemQuantity(Long userId, Long cartItemId, int quantity) {
        if (quantity <= 0) {
            return removeFromCart(userId, cartItemId);
        }

        CartItem item = cartItemRepository.findByIdAndCartUserId(cartItemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found: " + cartItemId));

        item.setQuantity(quantity);
        cartItemRepository.save(item);

        return getCart(userId);
    }

    @Transactional
    public CartResponse removeFromCart(Long userId, Long cartItemId) {
        CartItem item = cartItemRepository.findByIdAndCartUserId(cartItemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found: " + cartItemId));

        cartItemRepository.delete(item);
        return getCart(userId);
    }

    @Transactional
    public void clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        cartItemRepository.deleteByCartId(cart.getId());
    }
}

package com.medicareai.service;

import com.medicareai.dto.admin.AdminDashboardSummary;
import com.medicareai.entity.CustomerOrder;
import com.medicareai.enums.OrderStatus;
import com.medicareai.enums.PrescriptionStatus;
import com.medicareai.enums.Role;
import com.medicareai.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AdminAnalyticsService {

    private final CustomerOrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final MedicineRepository medicineRepository;
    private final MedicineBatchRepository batchRepository;
    private final PrescriptionRepository prescriptionRepository;

    @Transactional(readOnly = true)
    public AdminDashboardSummary getDashboardSummary() {
        long totalCustomers = userRepository.findByRole(Role.CUSTOMER).size();
        long totalMedicines = medicineRepository.count();
        long totalOrders = orderRepository.count();
        BigDecimal totalSales = orderRepository.calculateTotalPaidSales();

        LocalDate today = LocalDate.now();
        long lowStockCount = batchRepository.findLowStockBatches(20, today).size();
        long expiringCount = batchRepository.findExpiringBatches(today, today.plusDays(90)).size();
        long pendingPrescriptions = prescriptionRepository.countByStatus(PrescriptionStatus.UPLOADED);

        Map<String, Long> statusMap = new HashMap<>();
        for (OrderStatus st : OrderStatus.values()) {
            statusMap.put(st.name(), orderRepository.countByOrderStatus(st));
        }

        return AdminDashboardSummary.builder()
                .totalCustomers(totalCustomers)
                .totalMedicines(totalMedicines)
                .totalOrders(totalOrders)
                .totalSales(totalSales != null ? totalSales : BigDecimal.ZERO)
                .lowStockCount(lowStockCount)
                .expiringCount(expiringCount)
                .pendingPrescriptionsCount(pendingPrescriptions)
                .ordersByStatus(statusMap)
                .build();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTopSellingProducts(int limit) {
        List<Object[]> results = orderItemRepository.findTopSellingProducts(PageRequest.of(0, limit));
        List<Map<String, Object>> list = new ArrayList<>();

        for (Object[] row : results) {
            Map<String, Object> map = new HashMap<>();
            map.put("medicineId", row[0]);
            map.put("name", row[1]);
            map.put("brand", row[2]);
            map.put("totalQuantitySold", row[3]);
            map.put("totalRevenue", row[4]);
            list.add(map);
        }
        return list;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRecentOrders(int limit) {
        List<CustomerOrder> orders = orderRepository.findRecentOrders(PageRequest.of(0, limit));
        List<Map<String, Object>> list = new ArrayList<>();

        for (CustomerOrder o : orders) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", o.getId());
            map.put("orderNumber", o.getOrderNumber());
            map.put("customerName", o.getUser().getFullName());
            map.put("totalAmount", o.getTotalAmount());
            map.put("orderStatus", o.getOrderStatus().name());
            map.put("paymentStatus", o.getPaymentStatus().name());
            map.put("createdAt", o.getCreatedAt());
            list.add(map);
        }
        return list;
    }
}

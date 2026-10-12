package com.medicareai.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDashboardSummary {
    private long totalCustomers;
    private long totalMedicines;
    private long totalOrders;
    private BigDecimal totalSales;
    private long lowStockCount;
    private long expiringCount;
    private long pendingPrescriptionsCount;
    private Map<String, Long> ordersByStatus;
}

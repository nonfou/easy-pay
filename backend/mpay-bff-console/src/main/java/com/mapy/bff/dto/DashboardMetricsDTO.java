package com.mapy.bff.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DashboardMetricsDTO {
    BigDecimal monthIncome;
    BigDecimal weekIncome;
    BigDecimal yesterdayIncome;
    BigDecimal todayIncome;
    Long successOrders;
    Long pendingOrders;
}

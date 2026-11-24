package com.tracelens.order;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record Order(UUID orderId, String userId, List<OrderItem> items, BigDecimal totalAmount, OrderStatus status) {
    public Order {
        items = List.copyOf(items);
    }
}

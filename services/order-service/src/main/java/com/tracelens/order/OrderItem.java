package com.tracelens.order;

import java.math.BigDecimal;

public record OrderItem(String itemId, String name, int quantity, BigDecimal unitPrice) {
}

package com.tracelens.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record OrderItemRequest(
        @NotBlank String itemId,
        @NotBlank String name,
        @Positive int quantity,
        @NotNull @Positive BigDecimal unitPrice) {
    OrderItem toItem() {
        return new OrderItem(itemId, name, quantity, unitPrice);
    }
}

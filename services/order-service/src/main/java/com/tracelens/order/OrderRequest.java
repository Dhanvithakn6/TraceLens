package com.tracelens.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;

public record OrderRequest(
        @NotBlank String userId,
        @NotEmpty List<@Valid OrderItemRequest> items,
        @NotNull @Positive BigDecimal totalAmount) {
}

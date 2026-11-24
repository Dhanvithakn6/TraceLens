package com.tracelens.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrderServiceTest {
    private OrderRequest request() {
        return new OrderRequest("user-123",
                List.of(new OrderItemRequest("item-1", "Notebook", 2, new BigDecimal("4.50"))),
                new BigDecimal("9.00"));
    }

    @Test
    void createsOrderWithUniqueIdAndRetrievesIt() {
        OrderService service = new OrderService(0, false);
        Order first = service.create(request());
        Order second = service.create(request());

        assertNotNull(first.orderId());
        assertNotEquals(first.orderId(), second.orderId());
        assertEquals("user-123", first.userId());
        assertEquals(1, first.items().size());
        assertEquals(new BigDecimal("9.00"), first.totalAmount());
        assertEquals(OrderStatus.CREATED, first.status());
        assertEquals(first, service.get(first.orderId()));
    }

    @Test
    void marksOrderFailedWhenFailureIsSimulated() {
        OrderService service = new OrderService(0, true);

        assertEquals(OrderStatus.FAILED, service.create(request()).status());
    }

    @Test
    void throwsWhenOrderDoesNotExist() {
        OrderService service = new OrderService(0, false);

        assertThrows(OrderNotFoundException.class, () -> service.get(UUID.randomUUID()));
    }
}

package com.tracelens.order;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OrderService {
    private final ConcurrentMap<UUID, Order> orders = new ConcurrentHashMap<>();
    private final long delayMs;
    private final boolean simulateFailure;

    public OrderService(
            @Value("${order.simulation.delay-ms:0}") long delayMs,
            @Value("${order.simulation.failure:false}") boolean simulateFailure) {
        if (delayMs < 0) {
            throw new IllegalArgumentException("order.simulation.delay-ms must be non-negative");
        }
        this.delayMs = delayMs;
        this.simulateFailure = simulateFailure;
    }

    public Order create(OrderRequest request) {
        delayIfConfigured();
        Order order = new Order(
                UUID.randomUUID(),
                request.userId(),
                request.items().stream().map(OrderItemRequest::toItem).toList(),
                request.totalAmount(),
                simulateFailure ? OrderStatus.FAILED : OrderStatus.CREATED);
        orders.put(order.orderId(), order);
        return order;
    }

    public Order get(UUID id) {
        Order order = orders.get(id);
        if (order == null) {
            throw new OrderNotFoundException(id);
        }
        return order;
    }

    private void delayIfConfigured() {
        if (delayMs == 0) return;
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Order creation was interrupted", exception);
        }
    }
}

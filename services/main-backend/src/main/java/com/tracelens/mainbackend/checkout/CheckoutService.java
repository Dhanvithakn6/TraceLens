package com.tracelens.mainbackend.checkout;

import java.util.List;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Service
public class CheckoutService {
    private final RestClient restClient;
    private final String userServiceUrl;
    private final String orderServiceUrl;
    private final String paymentServiceUrl;

    public CheckoutService(
            RestClient.Builder restClientBuilder,
            @Value("${checkout.services.user.base-url}") String userServiceUrl,
            @Value("${checkout.services.order.base-url}") String orderServiceUrl,
            @Value("${checkout.services.payment.base-url}") String paymentServiceUrl) {
        this.restClient = restClientBuilder.build();
        this.userServiceUrl = userServiceUrl;
        this.orderServiceUrl = orderServiceUrl;
        this.paymentServiceUrl = paymentServiceUrl;
    }

    public CheckoutResponse checkout(CheckoutRequest request) {
        CheckoutResponse.UserDetails user = call("User Service", () -> restClient.post()
                .uri(userServiceUrl + "/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request.user())
                .retrieve()
                .body(CheckoutResponse.UserDetails.class));

        OrderRequest orderRequest = new OrderRequest(user.userId().toString(), request.items(), request.totalAmount());
        CheckoutResponse.OrderDetails order = call("Order Service", () -> restClient.post()
                .uri(orderServiceUrl + "/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .body(orderRequest)
                .retrieve()
                .body(CheckoutResponse.OrderDetails.class));

        PaymentRequest paymentRequest = new PaymentRequest(order.orderId().toString(), request.totalAmount());
        CheckoutResponse.PaymentDetails payment = call("Payment Service", () -> restClient.post()
                .uri(paymentServiceUrl + "/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .body(paymentRequest)
                .retrieve()
                .body(CheckoutResponse.PaymentDetails.class));

        if ("FAILED".equals(payment.status())) {
            throw new CheckoutDownstreamException(HttpStatus.BAD_GATEWAY,
                    "Payment Service reported a failed payment", null);
        }

        return new CheckoutResponse(user, order, payment);
    }

    private <T> T call(String serviceName, Supplier<T> request) {
        try {
            T response = request.get();
            if (response == null) {
                throw new CheckoutDownstreamException(HttpStatus.BAD_GATEWAY,
                        serviceName + " returned an empty response", null);
            }
            return response;
        } catch (CheckoutDownstreamException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            HttpStatusCode status = exception.getStatusCode().is4xxClientError()
                    ? exception.getStatusCode()
                    : HttpStatus.BAD_GATEWAY;
            throw new CheckoutDownstreamException(status,
                    serviceName + " returned HTTP " + exception.getStatusCode().value(), exception);
        } catch (ResourceAccessException exception) {
            throw new CheckoutDownstreamException(HttpStatus.SERVICE_UNAVAILABLE,
                    serviceName + " is unavailable", exception);
        } catch (RestClientException exception) {
            throw new CheckoutDownstreamException(HttpStatus.BAD_GATEWAY,
                    serviceName + " returned an invalid response", exception);
        }
    }

    private record OrderRequest(
            String userId,
            List<CheckoutRequest.CheckoutItem> items,
            java.math.BigDecimal totalAmount) {
    }

    private record PaymentRequest(String orderId, java.math.BigDecimal amount) {
    }
}

package com.tracelens.mainbackend.checkout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class CheckoutServiceTest {
    private static final String USER_URL = "http://localhost:8084";
    private static final String ORDER_URL = "http://localhost:8082";
    private static final String PAYMENT_URL = "http://localhost:8083";
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ORDER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PAYMENT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private MockRestServiceServer server;
    private CheckoutService checkoutService;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        checkoutService = new CheckoutService(builder, USER_URL, ORDER_URL, PAYMENT_URL);
    }

    @Test
    void createsUserOrderAndPaymentSequentially() {
        server.expect(requestTo(USER_URL + "/api/users"))
                .andExpect(method(POST))
                .andExpect(content().json("""
                        {"name":"Ada Lovelace","email":"ada@example.com"}
                        """))
                .andRespond(withSuccess("""
                        {"userId":"11111111-1111-1111-1111-111111111111","name":"Ada Lovelace","email":"ada@example.com","status":"ACTIVE"}
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo(ORDER_URL + "/api/orders"))
                .andExpect(method(POST))
                .andExpect(content().json("""
                        {"userId":"11111111-1111-1111-1111-111111111111","items":[{"itemId":"item-1","name":"Notebook","quantity":2,"unitPrice":4.50}],"totalAmount":9.00}
                        """))
                .andRespond(withSuccess("""
                        {"orderId":"22222222-2222-2222-2222-222222222222","userId":"11111111-1111-1111-1111-111111111111","items":[{"itemId":"item-1","name":"Notebook","quantity":2,"unitPrice":4.50}],"totalAmount":9.00,"status":"CREATED"}
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo(PAYMENT_URL + "/api/payments"))
                .andExpect(method(POST))
                .andExpect(content().json("""
                        {"orderId":"22222222-2222-2222-2222-222222222222","amount":9.00}
                        """))
                .andRespond(withSuccess("""
                        {"paymentId":"33333333-3333-3333-3333-333333333333","orderId":"22222222-2222-2222-2222-222222222222","amount":9.00,"status":"SUCCEEDED"}
                        """, MediaType.APPLICATION_JSON));

        CheckoutResponse response = checkoutService.checkout(request());

        assertEquals(USER_ID, response.user().userId());
        assertEquals(USER_ID.toString(), response.order().userId());
        assertEquals(ORDER_ID.toString(), response.payment().orderId());
        assertEquals(PAYMENT_ID, response.payment().paymentId());
        assertEquals(new BigDecimal("9.00"), response.payment().amount());
        assertEquals("SUCCEEDED", response.payment().status());
        server.verify();
    }

    @Test
    void reportsFailedPaymentAsCheckoutFailure() {
        expectSuccessfulUser();
        expectSuccessfulOrder();
        server.expect(requestTo(PAYMENT_URL + "/api/payments"))
                .andExpect(method(POST))
                .andRespond(withSuccess("""
                        {"paymentId":"33333333-3333-3333-3333-333333333333","orderId":"22222222-2222-2222-2222-222222222222","amount":9.00,"status":"FAILED"}
                        """, MediaType.APPLICATION_JSON));

        CheckoutDownstreamException exception = assertThrows(
                CheckoutDownstreamException.class, () -> checkoutService.checkout(request()));

        assertEquals(HttpStatus.BAD_GATEWAY, exception.status());
        assertEquals("Payment Service reported a failed payment", exception.getMessage());
        server.verify();
    }

    @Test
    void stopsWhenUserServiceFails() {
        server.expect(requestTo(USER_URL + "/api/users"))
                .andExpect(method(POST))
                .andRespond(withServerError());

        CheckoutDownstreamException exception = assertThrows(
                CheckoutDownstreamException.class, () -> checkoutService.checkout(request()));

        assertEquals(HttpStatus.BAD_GATEWAY, exception.status());
        server.verify();
    }

    @Test
    void stopsBeforePaymentWhenOrderServiceFails() {
        expectSuccessfulUser();
        server.expect(requestTo(ORDER_URL + "/api/orders"))
                .andExpect(method(POST))
                .andRespond(withServerError());

        CheckoutDownstreamException exception = assertThrows(
                CheckoutDownstreamException.class, () -> checkoutService.checkout(request()));

        assertEquals(HttpStatus.BAD_GATEWAY, exception.status());
        server.verify();
    }

    @Test
    void reportsPaymentServiceFailureAfterEarlierCallsSucceed() {
        expectSuccessfulUser();
        expectSuccessfulOrder();
        server.expect(requestTo(PAYMENT_URL + "/api/payments"))
                .andExpect(method(POST))
                .andRespond(withServerError());

        CheckoutDownstreamException exception = assertThrows(
                CheckoutDownstreamException.class, () -> checkoutService.checkout(request()));

        assertEquals(HttpStatus.BAD_GATEWAY, exception.status());
        server.verify();
    }

    private void expectSuccessfulUser() {
        server.expect(requestTo(USER_URL + "/api/users"))
                .andRespond(withSuccess("""
                        {"userId":"11111111-1111-1111-1111-111111111111","name":"Ada Lovelace","email":"ada@example.com","status":"ACTIVE"}
                        """, MediaType.APPLICATION_JSON));
    }

    private void expectSuccessfulOrder() {
        server.expect(requestTo(ORDER_URL + "/api/orders"))
                .andRespond(withSuccess("""
                        {"orderId":"22222222-2222-2222-2222-222222222222","userId":"11111111-1111-1111-1111-111111111111","items":[{"itemId":"item-1","name":"Notebook","quantity":2,"unitPrice":4.50}],"totalAmount":9.00,"status":"CREATED"}
                        """, MediaType.APPLICATION_JSON));
    }

    private CheckoutRequest request() {
        return new CheckoutRequest(
                new CheckoutRequest.UserRegistration("Ada Lovelace", "ada@example.com"),
                List.of(new CheckoutRequest.CheckoutItem("item-1", "Notebook", 2, new BigDecimal("4.50"))),
                new BigDecimal("9.00"));
    }
}

package com.tracelens.mainbackend.checkout;

import org.springframework.http.HttpStatusCode;

public class CheckoutDownstreamException extends RuntimeException {
    private final HttpStatusCode status;

    public CheckoutDownstreamException(HttpStatusCode status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatusCode status() {
        return status;
    }
}

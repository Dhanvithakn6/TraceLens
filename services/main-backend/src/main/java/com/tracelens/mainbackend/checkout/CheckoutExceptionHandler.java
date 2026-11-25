package com.tracelens.mainbackend.checkout;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CheckoutExceptionHandler {
    @ExceptionHandler(CheckoutDownstreamException.class)
    public ResponseEntity<ApiError> handleDownstreamFailure(CheckoutDownstreamException exception) {
        return ResponseEntity.status(exception.status()).body(new ApiError(exception.getMessage()));
    }

    public record ApiError(String error) {
    }
}

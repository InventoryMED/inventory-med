package br.com.inventorymed.common;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<ApiError> handleBadCredentials(
        BadCredentialsException exception,
        HttpServletRequest request
    ) {
        return response(
            HttpStatus.UNAUTHORIZED,
            "INVALID_CREDENTIALS",
            exception.getMessage(),
            request
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> handleAccessDenied(
        AccessDeniedException exception,
        HttpServletRequest request
    ) {
        return response(HttpStatus.FORBIDDEN, "ACCESS_DENIED", exception.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(
        MethodArgumentNotValidException exception,
        HttpServletRequest request
    ) {
        String message = exception
            .getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .distinct()
            .collect(Collectors.joining("; "));
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, request);
    }

    @ExceptionHandler(ResourceConflictException.class)
    ResponseEntity<ApiError> handleConflict(
        ResourceConflictException exception,
        HttpServletRequest request
    ) {
        return response(HttpStatus.CONFLICT, "RESOURCE_CONFLICT", exception.getMessage(), request);
    }

    @ExceptionHandler(BusinessValidationException.class)
    ResponseEntity<ApiError> handleBusinessValidation(
        BusinessValidationException exception,
        HttpServletRequest request
    ) {
        return response(
            HttpStatus.BAD_REQUEST,
            "BUSINESS_VALIDATION_ERROR",
            exception.getMessage(),
            request
        );
    }

    @ExceptionHandler(ProvisioningException.class)
    ResponseEntity<ApiError> handleProvisioning(
        ProvisioningException exception,
        HttpServletRequest request
    ) {
        return response(
            HttpStatus.SERVICE_UNAVAILABLE,
            "TENANT_PROVISIONING_FAILED",
            exception.getMessage(),
            request
        );
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(
        Exception exception,
        HttpServletRequest request
    ) {
        LOGGER.error(
            "Falha não tratada em {} {}",
            request.getMethod(),
            request.getRequestURI(),
            exception
        );
        return response(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "INTERNAL_ERROR",
            "Não foi possível concluir a operação",
            request
        );
    }

    private ResponseEntity<ApiError> response(
        HttpStatus status,
        String code,
        String message,
        HttpServletRequest request
    ) {
        return ResponseEntity.status(status).body(
            new ApiError(Instant.now(), status.value(), code, message, request.getRequestURI())
        );
    }
}

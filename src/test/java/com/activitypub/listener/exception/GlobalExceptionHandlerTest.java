package com.activitypub.listener.exception;

import com.activitypub.listener.dto.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler unit tests")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("ResourceNotFoundException returns 404 and RESOURCE_NOT_FOUND code")
    void handleResourceNotFound_returns404() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Monitor not found: x");

        ResponseEntity<ApiResponse<Void>> res = handler.handleResourceNotFoundException(ex);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getCode()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(res.getBody().getError()).isEqualTo("Monitor not found: x");
    }

    @Test
    @DisplayName("IllegalArgumentException returns 400 and INVALID_ARGUMENT code")
    void handleIllegalArgument_returns400() {
        IllegalArgumentException ex = new IllegalArgumentException("end_date must be >= start_date");

        ResponseEntity<ApiResponse<Void>> res = handler.handleIllegalArgumentException(ex);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getCode()).isEqualTo("INVALID_ARGUMENT");
    }

    @Test
    @DisplayName("IllegalStateException returns 422 and ILLEGAL_STATE code")
    void handleIllegalState_returns422() {
        IllegalStateException ex = new IllegalStateException("Monitor limit exceeded");

        ResponseEntity<ApiResponse<Void>> res = handler.handleIllegalStateException(ex);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getCode()).isEqualTo("ILLEGAL_STATE");
        assertThat(res.getBody().getError()).contains("Monitor limit exceeded");
    }

    @Test
    @DisplayName("MethodArgumentNotValidException returns 400 and VALIDATION_ERROR code")
    void handleValidation_returns400() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(
                List.of(new FieldError("CreateMonitorDTO", "name", "must not be blank")));

        ResponseEntity<ApiResponse<java.util.Map<String, String>>> res =
                handler.handleValidationExceptions(ex);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(res.getBody().getData()).containsKey("name");
    }
}

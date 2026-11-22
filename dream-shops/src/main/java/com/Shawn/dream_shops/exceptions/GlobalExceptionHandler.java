package com.Shawn.dream_shops.exceptions;

import com.Shawn.dream_shops.reponse.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.nio.file.AccessDeniedException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({ResourceNotFoundException.class, ProductNotFoundException.class})
    public ResponseEntity<ApiResponse> handleNotFound(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse(ex.getMessage(), null));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<String> handleAccessDeniedException(AccessDeniedException ex)
    {
        String message = "You do not have permission to this action.";
        return new ResponseEntity<>(message, HttpStatus.FORBIDDEN);
    }
}

package com.pta.outsourcing.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<ResultVO<Void>> handleBizException(BizException exception) {
        return ResponseEntity.ok(ResultVO.fail(exception.getErrorCode(), exception.getMessage()));
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            BindException.class,
            ConstraintViolationException.class
    })
    public ResponseEntity<ResultVO<Void>> handleParamException(Exception exception) {
        String message = resolveParamMessage(exception);
        return ResponseEntity.badRequest().body(ResultVO.fail(ErrorCode.PARAM_ERROR, message));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ResultVO<Void>> handleAccessDenied(AccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ResultVO.fail(ErrorCode.FORBIDDEN, ErrorCode.FORBIDDEN.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResultVO<Void>> handleException(Exception exception, HttpServletRequest request) {
        log.error("Unhandled exception, path={}", request.getRequestURI(), exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResultVO.fail(ErrorCode.SYSTEM_ERROR, ErrorCode.SYSTEM_ERROR.getMessage()));
    }

    private String resolveParamMessage(Exception exception) {
        if (exception instanceof MethodArgumentNotValidException validException) {
            return validException.getBindingResult().getFieldErrors().stream()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .collect(Collectors.joining("; "));
        }
        if (exception instanceof BindException bindException) {
            return bindException.getBindingResult().getFieldErrors().stream()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .collect(Collectors.joining("; "));
        }
        return exception.getMessage();
    }
}

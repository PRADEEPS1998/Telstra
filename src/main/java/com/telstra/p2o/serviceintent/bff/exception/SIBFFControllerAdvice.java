package com.telstra.p2o.serviceintent.bff.exception;

import static com.telstra.p2o.serviceintent.bff.constant.Constants.ERROR_MESSAGE;
import static com.telstra.p2o.serviceintent.bff.constant.Constants.GENERIC_ERROR;

import com.telstra.p2o.common.core.exception.UserInfoValidationException;
import com.telstra.p2o.serviceintent.bff.constant.Constants;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class SIBFFControllerAdvice {

    private static final Logger logger = LoggerFactory.getLogger(SIBFFControllerAdvice.class);

    @ExceptionHandler(SIBFFException.class)
    public ResponseEntity<SIBFFErrorResponse> handleSIBFFValidationException(
            SIBFFException exception) {
        return buildSIBFFErrorResponse(exception);
    }

    private ResponseEntity<SIBFFErrorResponse> buildSIBFFErrorResponse(SIBFFException exception) {
        logger.error(ERROR_MESSAGE, exception);
        return new ResponseEntity<>(
                SIBFFErrorResponse.builder()
                        .statusCode(exception.getStatusCode())
                        .success(false)
                        .errors(exception.getErrors())
                        .build(),
                HttpStatus.valueOf(exception.getStatusCode()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public SIBFFErrorResponse handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception) {
        return buildMethodArgumentNotValidResponse(exception);
    }

    @ExceptionHandler(UnAuthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public SIBFFErrorResponse handleUnAuthorizedException(UnAuthorizedException exception) {
        return buildUnAuthorizedExceptionResponse(exception);
    }

    @ExceptionHandler(CacheDataNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public SIBFFErrorResponse handleCacheDataNotFound(CacheDataNotFoundException exception) {
        return buildCacheDataNotFoundResponse(exception);
    }

    private SIBFFErrorResponse buildCacheDataNotFoundResponse(
            CacheDataNotFoundException exception) {
        logger.error(ERROR_MESSAGE, exception);
        return SIBFFErrorResponse.builder()
                .statusCode(exception.getStatusCode())
                .success(false)
                .errors(exception.getErrors())
                .build();
    }

    private SIBFFErrorResponse buildMethodArgumentNotValidResponse(
            MethodArgumentNotValidException exception) {
        logger.error(ERROR_MESSAGE, exception);
        return SIBFFErrorResponse.builder()
                .statusCode(exception.getStatusCode())
                .success(false)
                .errors(exception.getErrors())
                .build();
    }

    private SIBFFErrorResponse buildUnAuthorizedExceptionResponse(UnAuthorizedException exception) {
        logger.error(ERROR_MESSAGE, exception);
        return SIBFFErrorResponse.builder()
                .statusCode(exception.getStatusCode())
                .success(false)
                .errors(exception.getErrors())
                .build();
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public SIBFFErrorResponse handleGenericException(Exception exception) {
        return buildGenericExceptionResponse(exception);
    }

    private SIBFFErrorResponse buildGenericExceptionResponse(Exception exception) {
        logger.error(ERROR_MESSAGE, exception);
        return SIBFFErrorResponse.builder()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .success(false)
                .errors(
                        Collections.singletonList(
                                SIBFFError.builder()
                                        .code(GENERIC_ERROR)
                                        .message(exception.getMessage())
                                        .build()))
                .build();
    }

    @ExceptionHandler(UserInfoValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public SIBFFErrorResponse handleUserInfoValidationException(
            UserInfoValidationException exception) {
        logger.error("User info validation exception : {}", exception.getMessage());
        return SIBFFErrorResponse.builder()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .success(false)
                .errors(
                        Collections.singletonList(
                                SIBFFError.builder()
                                        .code(HttpStatus.BAD_REQUEST.name())
                                        .message(Constants.INVALID_INPUT_DATA)
                                        .build()))
                .build();
    }
}

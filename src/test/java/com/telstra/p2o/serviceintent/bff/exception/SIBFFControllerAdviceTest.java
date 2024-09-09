package com.telstra.p2o.serviceintent.bff.exception;

import static com.telstra.p2o.serviceintent.bff.constant.Constants.GENERIC_ERROR;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

@RunWith(MockitoJUnitRunner.class)
public class SIBFFControllerAdviceTest {

    @InjectMocks SIBFFControllerAdvice sibffControllerAdvice;

    @Test()
    public void handleCABFFValidationExceptionTest() {
        SIBFFException exception =
                new SIBFFException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Test", GENERIC_ERROR);
        ResponseEntity<SIBFFErrorResponse> response =
                sibffControllerAdvice.handleSIBFFValidationException(exception);
        Assert.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test()
    public void handleMethodArgumentNotValidExceptionTest() {
        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(
                        HttpStatus.BAD_REQUEST.value(), "Test", GENERIC_ERROR);
        SIBFFErrorResponse response =
                sibffControllerAdvice.handleMethodArgumentNotValidException(exception);
        Assert.assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode());
    }

    @Test()
    public void handleUnAuthorizedExceptionTest() {
        UnAuthorizedException exception =
                new UnAuthorizedException(HttpStatus.UNAUTHORIZED.value(), "Test", GENERIC_ERROR);
        SIBFFErrorResponse response = sibffControllerAdvice.handleUnAuthorizedException(exception);
        Assert.assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatusCode());
    }

    @Test()
    public void handleCacheDataNotFoundTest() {
        CacheDataNotFoundException exception =
                new CacheDataNotFoundException(HttpStatus.NOT_FOUND.value(), "Test", GENERIC_ERROR);
        SIBFFErrorResponse response = sibffControllerAdvice.handleCacheDataNotFound(exception);
        Assert.assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatusCode());
    }

    @Test()
    public void handleGenericExceptionTest() {
        Exception exception = new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
        SIBFFErrorResponse response = sibffControllerAdvice.handleGenericException(exception);
        Assert.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getStatusCode());
    }
}

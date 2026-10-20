package ro.tuc.common.handlers.exceptions.model;

import org.springframework.http.HttpStatus;

import java.util.ArrayList;

public class UnauthorizedException extends CustomException {
    private static final String MESSAGE = "Authentication required or invalid!";
    private static final HttpStatus httpStatus = HttpStatus.UNAUTHORIZED;

    public UnauthorizedException(String resource) {
        super(MESSAGE, httpStatus, resource, new ArrayList<>());
    }
}

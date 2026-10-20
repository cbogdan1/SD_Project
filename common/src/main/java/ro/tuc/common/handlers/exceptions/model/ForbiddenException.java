package ro.tuc.common.handlers.exceptions.model;

import org.springframework.http.HttpStatus;

import java.util.ArrayList;

public class ForbiddenException extends CustomException {
    private static final String MESSAGE = "Access to this resource is not allowed!";
    private static final HttpStatus httpStatus = HttpStatus.FORBIDDEN;

    public ForbiddenException(String resource) {
        super(MESSAGE, httpStatus, resource, new ArrayList<>());
    }
}

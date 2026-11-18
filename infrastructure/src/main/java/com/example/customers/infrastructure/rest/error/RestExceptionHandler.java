package com.example.customers.infrastructure.rest.error;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import com.example.customers.domain.exception.ConflictException;
import com.example.customers.domain.exception.ValidationException;
import com.example.customers.domain.genericfield.exception.InvalidGenericFieldValuesException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
class RestExceptionHandler {

  private static final String ERRORS = "errors";
  private static final String UNEXPECTED_ERROR = "Unexpected error";

  @ExceptionHandler(ValidationException.class)
  ProblemDetail handleValidation(ValidationException exception) {
    return ProblemDetail.forStatusAndDetail(BAD_REQUEST, exception.getMessage());
  }

  @ExceptionHandler(InvalidGenericFieldValuesException.class)
  ProblemDetail handleInvalidGenericFieldValues(InvalidGenericFieldValuesException exception) {
    var problemDetail = ProblemDetail.forStatusAndDetail(BAD_REQUEST, exception.getMessage());
    problemDetail.setProperty(ERRORS, exception.getInvalidFieldNames());
    return problemDetail;
  }

  @ExceptionHandler(ConflictException.class)
  ProblemDetail handleConflict(ConflictException exception) {
    return ProblemDetail.forStatusAndDetail(CONFLICT, exception.getMessage());
  }

  @ExceptionHandler(Exception.class)
  ProblemDetail handleUnexpected(Exception exception) {
    log.error(UNEXPECTED_ERROR, exception);
    return ProblemDetail.forStatusAndDetail(INTERNAL_SERVER_ERROR, UNEXPECTED_ERROR);
  }
}

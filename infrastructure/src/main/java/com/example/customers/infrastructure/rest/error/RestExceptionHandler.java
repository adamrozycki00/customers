package com.example.customers.infrastructure.rest.error;

import static org.springframework.http.HttpHeaders.RETRY_AFTER;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

import com.example.customers.domain.exception.ConflictException;
import com.example.customers.domain.exception.NotFoundException;
import com.example.customers.domain.exception.ValidationException;
import com.example.customers.domain.genericfield.exception.InvalidGenericFieldValuesException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
class RestExceptionHandler {

  private static final String ERRORS = "errors";
  private static final String UNEXPECTED_ERROR = "Unexpected error";
  private static final String DATABASE_UNAVAILABLE = "Database temporarily unavailable";
  private static final String RETRY_AFTER_SECONDS = "5";

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

  @ExceptionHandler(NotFoundException.class)
  ProblemDetail handleNotFound(NotFoundException exception) {
    return ProblemDetail.forStatusAndDetail(NOT_FOUND, exception.getMessage());
  }

  @ExceptionHandler(ConflictException.class)
  ProblemDetail handleConflict(ConflictException exception) {
    return ProblemDetail.forStatusAndDetail(CONFLICT, exception.getMessage());
  }

  @ExceptionHandler({DataAccessResourceFailureException.class, TransientDataAccessException.class})
  ProblemDetail handleDatabaseUnavailable(DataAccessException exception, HttpServletResponse response) {
    log.warn(DATABASE_UNAVAILABLE, exception);
    response.setHeader(RETRY_AFTER, RETRY_AFTER_SECONDS);
    return ProblemDetail.forStatusAndDetail(SERVICE_UNAVAILABLE, DATABASE_UNAVAILABLE);
  }

  @ExceptionHandler(Exception.class)
  ProblemDetail handleUnexpected(Exception exception) {
    log.error(UNEXPECTED_ERROR, exception);
    return ProblemDetail.forStatusAndDetail(INTERNAL_SERVER_ERROR, UNEXPECTED_ERROR);
  }
}

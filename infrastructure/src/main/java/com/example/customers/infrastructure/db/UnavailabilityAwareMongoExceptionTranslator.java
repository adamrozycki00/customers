package com.example.customers.infrastructure.db;

import com.mongodb.MongoSocketException;
import com.mongodb.MongoTimeoutException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.mongodb.core.MongoExceptionTranslator;

/**
 * Spring Data matches driver exceptions by exact class name, so subclasses such as the operation timeout
 * ({@code MongoOperationTimeoutException}) or socket read failures end up uncategorized. This translator maps every
 * timeout and socket failure to {@link DataAccessResourceFailureException}: the database is unavailable.
 */
public class UnavailabilityAwareMongoExceptionTranslator extends MongoExceptionTranslator {

  @Override
  public DataAccessException translateExceptionIfPossible(RuntimeException exception) {
    if (exception instanceof MongoTimeoutException || exception instanceof MongoSocketException) {
      return new DataAccessResourceFailureException(exception.getMessage(), exception);
    }

    return super.translateExceptionIfPossible(exception);
  }
}

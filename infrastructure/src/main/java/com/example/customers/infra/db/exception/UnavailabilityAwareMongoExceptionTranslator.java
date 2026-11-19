package com.example.customers.infra.db.exception;

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
  public DataAccessException translateExceptionIfPossible(RuntimeException e) {
    if (e instanceof MongoTimeoutException || e instanceof MongoSocketException) {
      return new DataAccessResourceFailureException(e.getMessage(), e);
    }

    return super.translateExceptionIfPossible(e);
  }
}

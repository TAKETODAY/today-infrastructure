/*
 * Copyright 2017 - 2025 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.dao;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/12 18:47
 */
class ExceptionTests {

  @Nested
  class TransientDataAccessResourceExceptionTests {

    @Test
    void constructorWithMessage() {
      String message = "Resource temporarily unavailable";
      TransientDataAccessResourceException exception = new TransientDataAccessResourceException(message);

      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getCause()).isNull();
    }

    @Test
    void constructorWithMessageAndCause() {
      String message = "Resource temporarily unavailable";
      Throwable cause = new RuntimeException("Connection failed");
      TransientDataAccessResourceException exception = new TransientDataAccessResourceException(message, cause);

      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void exceptionIsTransientDataAccessException() {
      TransientDataAccessResourceException exception = new TransientDataAccessResourceException("test");

      assertThat(exception).isInstanceOf(TransientDataAccessException.class);
    }

    @Test
    void exceptionIsDataAccessException() {
      TransientDataAccessResourceException exception = new TransientDataAccessResourceException("test");

      assertThat(exception).isInstanceOf(DataAccessException.class);
    }

    @Test
    void exceptionIsRuntimeException() {
      TransientDataAccessResourceException exception = new TransientDataAccessResourceException("test");

      assertThat(exception).isInstanceOf(RuntimeException.class);
    }

  }

  @Nested
  class IncorrectUpdateSemanticsDataAccessExceptionTests {

    @Test
    void constructorWithMessage() {
      String message = "Expected to update 1 row but updated 3 rows";
      IncorrectUpdateSemanticsDataAccessException exception = new IncorrectUpdateSemanticsDataAccessException(message);

      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getCause()).isNull();
    }

    @Test
    void constructorWithMessageAndCause() {
      String message = "Expected to update 1 row but updated 3 rows";
      Throwable cause = new RuntimeException("SQL execution error");
      IncorrectUpdateSemanticsDataAccessException exception = new IncorrectUpdateSemanticsDataAccessException(message, cause);

      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void exceptionIsInvalidDataAccessResourceUsageException() {
      IncorrectUpdateSemanticsDataAccessException exception = new IncorrectUpdateSemanticsDataAccessException("test");

      assertThat(exception).isInstanceOf(InvalidDataAccessResourceUsageException.class);
    }

    @Test
    void exceptionIsDataAccessException() {
      IncorrectUpdateSemanticsDataAccessException exception = new IncorrectUpdateSemanticsDataAccessException("test");

      assertThat(exception).isInstanceOf(DataAccessException.class);
    }

    @Test
    void exceptionIsRuntimeException() {
      IncorrectUpdateSemanticsDataAccessException exception = new IncorrectUpdateSemanticsDataAccessException("test");

      assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    void wasDataUpdatedReturnsTrueByDefault() {
      IncorrectUpdateSemanticsDataAccessException exception = new IncorrectUpdateSemanticsDataAccessException("test");

      assertThat(exception.wasDataUpdated()).isTrue();
    }

  }

  @Nested
  class OptimisticLockingFailureExceptionTests {
    @Test
    void constructorWithMessage() {
      String message = "Optimistic locking failed";
      OptimisticLockingFailureException exception = new OptimisticLockingFailureException(message);

      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getCause()).isNull();
    }

    @Test
    void constructorWithMessageAndCause() {
      String message = "Optimistic locking failed";
      Throwable cause = new RuntimeException("Version conflict");
      OptimisticLockingFailureException exception = new OptimisticLockingFailureException(message, cause);

      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void exceptionIsConcurrencyFailureException() {
      OptimisticLockingFailureException exception = new OptimisticLockingFailureException("test");

      assertThat(exception).isInstanceOf(ConcurrencyFailureException.class);
    }

    @Test
    void exceptionIsTransientDataAccessException() {
      OptimisticLockingFailureException exception = new OptimisticLockingFailureException("test");

      assertThat(exception).isInstanceOf(TransientDataAccessException.class);
    }

    @Test
    void exceptionIsDataAccessException() {
      OptimisticLockingFailureException exception = new OptimisticLockingFailureException("test");

      assertThat(exception).isInstanceOf(DataAccessException.class);
    }

    @Test
    void exceptionIsRuntimeException() {
      OptimisticLockingFailureException exception = new OptimisticLockingFailureException("test");

      assertThat(exception).isInstanceOf(RuntimeException.class);
    }

  }

  @Nested
  class QueryTimeoutExceptionTests {
    @Test
    void constructorWithMessage() {
      String message = "Query timed out after 30 seconds";
      QueryTimeoutException exception = new QueryTimeoutException(message);

      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getCause()).isNull();
    }

    @Test
    void constructorWithMessageAndCause() {
      String message = "Query timed out after 30 seconds";
      Throwable cause = new RuntimeException("Database timeout");
      QueryTimeoutException exception = new QueryTimeoutException(message, cause);

      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void exceptionIsTransientDataAccessException() {
      QueryTimeoutException exception = new QueryTimeoutException("test");

      assertThat(exception).isInstanceOf(TransientDataAccessException.class);
    }

    @Test
    void exceptionIsDataAccessException() {
      QueryTimeoutException exception = new QueryTimeoutException("test");
      assertThat(exception).isInstanceOf(DataAccessException.class);
    }

    @Test
    void exceptionIsRuntimeException() {
      QueryTimeoutException exception = new QueryTimeoutException("test");

      assertThat(exception).isInstanceOf(RuntimeException.class);
    }

  }

  @Nested
  class RecoverableDataAccessExceptionTests {
    @Test
    void constructorWithMessage() {
      String message = "Recoverable data access exception";
      RecoverableDataAccessException exception = new RecoverableDataAccessException(message);

      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getCause()).isNull();
    }

    @Test
    void constructorWithMessageAndCause() {
      String message = "Recoverable data access exception";
      Throwable cause = new RuntimeException("Underlying cause");
      RecoverableDataAccessException exception = new RecoverableDataAccessException(message, cause);

      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void exceptionIsDataAccessException() {
      RecoverableDataAccessException exception = new RecoverableDataAccessException("test");

      assertThat(exception).isInstanceOf(DataAccessException.class);
    }

    @Test
    void exceptionIsRuntimeException() {
      RecoverableDataAccessException exception = new RecoverableDataAccessException("test");

      assertThat(exception).isInstanceOf(RuntimeException.class);
    }

  }

  @Nested
  class CannotAcquireLockExceptionTests {
    @Test
    void constructorWithMessage() {
      String message = "Failed to acquire lock";
      CannotAcquireLockException exception = new CannotAcquireLockException(message);

      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getCause()).isNull();
    }

    @Test
    void constructorWithMessageAndCause() {
      String message = "Failed to acquire lock";
      Throwable cause = new RuntimeException("Lock timeout");
      CannotAcquireLockException exception = new CannotAcquireLockException(message, cause);

      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void exceptionIsPessimisticLockingFailureException() {
      CannotAcquireLockException exception = new CannotAcquireLockException("test");

      assertThat(exception).isInstanceOf(PessimisticLockingFailureException.class);
    }

    @Test
    void exceptionIsTransientDataAccessException() {
      CannotAcquireLockException exception = new CannotAcquireLockException("test");
      assertThat(exception).isInstanceOf(TransientDataAccessException.class);
    }

    @Test
    void exceptionIsDataAccessException() {
      CannotAcquireLockException exception = new CannotAcquireLockException("test");
      assertThat(exception).isInstanceOf(DataAccessException.class);
    }

    @Test
    void exceptionIsRuntimeException() {
      CannotAcquireLockException exception = new CannotAcquireLockException("test");
      assertThat(exception).isInstanceOf(RuntimeException.class);
    }

  }

  @Nested
  class PessimisticLockingFailureExceptionTests {
    @Test
    void constructorWithMessage() {
      String message = "Pessimistic locking failed";
      PessimisticLockingFailureException exception = new PessimisticLockingFailureException(message);

      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getCause()).isNull();
    }

    @Test
    void constructorWithMessageAndCause() {
      String message = "Pessimistic locking failed";
      Throwable cause = new RuntimeException("Lock acquisition timeout");
      PessimisticLockingFailureException exception = new PessimisticLockingFailureException(message, cause);

      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void exceptionIsConcurrencyFailureException() {
      PessimisticLockingFailureException exception = new PessimisticLockingFailureException("test");

      assertThat(exception).isInstanceOf(ConcurrencyFailureException.class);
    }

    @Test
    void exceptionIsTransientDataAccessException() {
      PessimisticLockingFailureException exception = new PessimisticLockingFailureException("test");

      assertThat(exception).isInstanceOf(TransientDataAccessException.class);
    }

    @Test
    void exceptionIsDataAccessException() {
      PessimisticLockingFailureException exception = new PessimisticLockingFailureException("test");

      assertThat(exception).isInstanceOf(DataAccessException.class);
    }

    @Test
    void exceptionIsRuntimeException() {
      PessimisticLockingFailureException exception = new PessimisticLockingFailureException("test");

      assertThat(exception).isInstanceOf(RuntimeException.class);
    }

  }

  @Nested
  class CannotSerializeTransactionExceptionTests {
    @Test
    void constructorWithMessage() {
      String message = "Cannot serialize transaction due to conflicts";
      CannotSerializeTransactionException exception = new CannotSerializeTransactionException(message);

      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getCause()).isNull();
    }

    @Test
    void constructorWithMessageAndCause() {
      String message = "Cannot serialize transaction due to conflicts";
      Throwable cause = new RuntimeException("Serialization failure");
      CannotSerializeTransactionException exception = new CannotSerializeTransactionException(message, cause);

      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void exceptionIsPessimisticLockingFailureException() {
      CannotSerializeTransactionException exception = new CannotSerializeTransactionException("test");

      assertThat(exception).isInstanceOf(PessimisticLockingFailureException.class);
    }

    @Test
    void exceptionIsTransientDataAccessException() {
      CannotSerializeTransactionException exception = new CannotSerializeTransactionException("test");

      assertThat(exception).isInstanceOf(TransientDataAccessException.class);
    }

    @Test
    void exceptionIsDataAccessException() {
      CannotSerializeTransactionException exception = new CannotSerializeTransactionException("test");

      assertThat(exception).isInstanceOf(DataAccessException.class);
    }

    @Test
    void exceptionIsRuntimeException() {
      CannotSerializeTransactionException exception = new CannotSerializeTransactionException("test");

      assertThat(exception).isInstanceOf(RuntimeException.class);
    }

  }

  @Nested
  class DuplicateKeyExceptionTests {
    @Test
    void constructorWithMessage() {
      String message = "Duplicate key violation";
      DuplicateKeyException exception = new DuplicateKeyException(message);

      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getCause()).isNull();
    }

    @Test
    void constructorWithMessageAndCause() {
      String message = "Duplicate key violation";
      Throwable cause = new RuntimeException("Constraint violation");
      DuplicateKeyException exception = new DuplicateKeyException(message, cause);

      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void exceptionIsDataIntegrityViolationException() {
      DuplicateKeyException exception = new DuplicateKeyException("test");

      assertThat(exception).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void exceptionIsDataAccessException() {
      DuplicateKeyException exception = new DuplicateKeyException("test");

      assertThat(exception).isInstanceOf(DataAccessException.class);
    }

    @Test
    void exceptionIsRuntimeException() {
      DuplicateKeyException exception = new DuplicateKeyException("test");

      assertThat(exception).isInstanceOf(RuntimeException.class);
    }

  }

  @Nested
  class DataIntegrityViolationExceptionTests {
    @Test
    void constructorWithMessage() {
      String message = "Data integrity violation";
      DataIntegrityViolationException exception = new DataIntegrityViolationException(message);

      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getCause()).isNull();
    }

    @Test
    void constructorWithMessageAndCause() {
      String message = "Data integrity violation";
      Throwable cause = new RuntimeException("Constraint violation");
      DataIntegrityViolationException exception = new DataIntegrityViolationException(message, cause);

      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void exceptionIsNonTransientDataAccessException() {
      DataIntegrityViolationException exception = new DataIntegrityViolationException("test");

      assertThat(exception).isInstanceOf(NonTransientDataAccessException.class);
    }

    @Test
    void exceptionIsDataAccessException() {
      DataIntegrityViolationException exception = new DataIntegrityViolationException("test");

      assertThat(exception).isInstanceOf(DataAccessException.class);
    }

    @Test
    void exceptionIsRuntimeException() {
      DataIntegrityViolationException exception = new DataIntegrityViolationException("test");

      assertThat(exception).isInstanceOf(RuntimeException.class);
    }

  }

  @Nested
  class EmptyResultDataAccessExceptionTests {
    @Test
    void constructorWithExpectedSize() {
      EmptyResultDataAccessException exception = new EmptyResultDataAccessException(1);

      assertThat(exception.getMessage()).contains("1");
      assertThat(exception.getExpectedSize()).isEqualTo(1);
      assertThat(exception.getActualSize()).isEqualTo(0);
      assertThat(exception.getCause()).isNull();
    }

    @Test
    void constructorWithMessageAndExpectedSize() {
      String message = "Expected 1 result but found 0";
      EmptyResultDataAccessException exception = new EmptyResultDataAccessException(message, 1);

      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getExpectedSize()).isEqualTo(1);
      assertThat(exception.getActualSize()).isEqualTo(0);
      assertThat(exception.getCause()).isNull();
    }

    @Test
    void constructorWithMessageExpectedSizeAndCause() {
      String message = "Expected 1 result but found 0";
      Throwable cause = new RuntimeException("Query returned no results");
      EmptyResultDataAccessException exception = new EmptyResultDataAccessException(message, 1, cause);

      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getExpectedSize()).isEqualTo(1);
      assertThat(exception.getActualSize()).isEqualTo(0);
      assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void exceptionIsIncorrectResultSizeDataAccessException() {
      EmptyResultDataAccessException exception = new EmptyResultDataAccessException(1);

      assertThat(exception).isInstanceOf(IncorrectResultSizeDataAccessException.class);
    }

    @Test
    void exceptionIsDataAccessException() {
      EmptyResultDataAccessException exception = new EmptyResultDataAccessException(1);

      assertThat(exception).isInstanceOf(DataAccessException.class);
    }

    @Test
    void exceptionIsRuntimeException() {
      EmptyResultDataAccessException exception = new EmptyResultDataAccessException(1);

      assertThat(exception).isInstanceOf(RuntimeException.class);
    }

  }

  @Nested
  class IncorrectResultSizeDataAccessExceptionTests {
    @Test
    void constructorWithExpectedSize() {
      int expectedSize = 5;
      IncorrectResultSizeDataAccessException exception = new IncorrectResultSizeDataAccessException(expectedSize);

      assertThat(exception.getMessage()).isEqualTo("Incorrect result size: expected " + expectedSize);
      assertThat(exception.getExpectedSize()).isEqualTo(expectedSize);
      assertThat(exception.getActualSize()).isEqualTo(-1);
      assertThat(exception.getCause()).isNull();
    }

    @Test
    void constructorWithExpectedSizeAndActualSize() {
      int expectedSize = 3;
      int actualSize = 7;
      IncorrectResultSizeDataAccessException exception = new IncorrectResultSizeDataAccessException(expectedSize, actualSize);

      assertThat(exception.getMessage()).isEqualTo("Incorrect result size: expected " + expectedSize + ", actual " + actualSize);
      assertThat(exception.getExpectedSize()).isEqualTo(expectedSize);
      assertThat(exception.getActualSize()).isEqualTo(actualSize);
      assertThat(exception.getCause()).isNull();
    }

    @Test
    void constructorWithMessageAndExpectedSize() {
      String message = "Custom message for incorrect result size";
      int expectedSize = 2;
      IncorrectResultSizeDataAccessException exception = new IncorrectResultSizeDataAccessException(message, expectedSize);

      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getExpectedSize()).isEqualTo(expectedSize);
      assertThat(exception.getActualSize()).isEqualTo(-1);
      assertThat(exception.getCause()).isNull();
    }

    @Test
    void constructorWithMessageExpectedSizeAndCause() {
      String message = "Custom message for incorrect result size";
      int expectedSize = 4;
      Throwable cause = new RuntimeException("Underlying cause");
      IncorrectResultSizeDataAccessException exception = new IncorrectResultSizeDataAccessException(message, expectedSize, cause);

      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getExpectedSize()).isEqualTo(expectedSize);
      assertThat(exception.getActualSize()).isEqualTo(-1);
      assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void constructorWithMessageExpectedSizeAndActualSize() {
      String message = "Custom message for incorrect result size";
      int expectedSize = 1;
      int actualSize = 3;
      IncorrectResultSizeDataAccessException exception = new IncorrectResultSizeDataAccessException(message, expectedSize, actualSize);

      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getExpectedSize()).isEqualTo(expectedSize);
      assertThat(exception.getActualSize()).isEqualTo(actualSize);
      assertThat(exception.getCause()).isNull();
    }

    @Test
    void constructorWithMessageExpectedSizeActualSizeAndCause() {
      String message = "Custom message for incorrect result size";
      int expectedSize = 6;
      int actualSize = 0;
      Throwable cause = new RuntimeException("Underlying cause");
      IncorrectResultSizeDataAccessException exception = new IncorrectResultSizeDataAccessException(message, expectedSize, actualSize, cause);

      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getExpectedSize()).isEqualTo(expectedSize);
      assertThat(exception.getActualSize()).isEqualTo(actualSize);
      assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void exceptionIsDataRetrievalFailureException() {
      IncorrectResultSizeDataAccessException exception = new IncorrectResultSizeDataAccessException(1);

      assertThat(exception).isInstanceOf(DataRetrievalFailureException.class);
    }

    @Test
    void exceptionIsDataAccessException() {
      IncorrectResultSizeDataAccessException exception = new IncorrectResultSizeDataAccessException(1);

      assertThat(exception).isInstanceOf(DataAccessException.class);
    }

    @Test
    void exceptionIsRuntimeException() {
      IncorrectResultSizeDataAccessException exception = new IncorrectResultSizeDataAccessException(1);

      assertThat(exception).isInstanceOf(RuntimeException.class);
    }

  }

}

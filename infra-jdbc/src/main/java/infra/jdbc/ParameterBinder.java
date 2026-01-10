/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.jdbc;

import org.jspecify.annotations.Nullable;

import java.io.InputStream;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;

import infra.jdbc.type.TypeHandler;

/**
 * An abstract class that provides a mechanism to bind values to a
 * {@link PreparedStatement} for database operations. This class defines
 * various static factory methods to create specific implementations of
 * {@code ParameterBinder} for different data types.
 *
 * <p>The {@link #bind(PreparedStatement, int)} method is implemented by
 * subclasses to handle the binding of specific types to a prepared statement.
 *
 * <p>Example usage:
 * <pre>{@code
 * // Bind an integer value to a PreparedStatement
 * ParameterBinder intBinder = ParameterBinder.forInt(42);
 * intBinder.bind(statement, 1);
 *
 * // Bind a string value to a PreparedStatement
 * ParameterBinder stringBinder = ParameterBinder.forString("example");
 * stringBinder.bind(statement, 2);
 *
 * // Bind a null value to a PreparedStatement
 * ParameterBinder.null_binder.bind(statement, 3);
 * }</pre>
 *
 * <p>This class also supports binding other types such as {@code long},
 * {@code Timestamp}, {@code Time}, {@code Date}, {@code Boolean},
 * {@code InputStream}, and custom objects using a {@code TypeHandler}.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see PreparedStatement
 * @see SQLException
 * @since 2021/1/7 15:09
 */
public abstract class ParameterBinder {

  /**
   * Binds a value to the specified parameter index in a {@link PreparedStatement}.
   * This method is typically implemented by subclasses to handle specific types
   * of values and bind them to the prepared statement.
   *
   * <p>Example usage:
   * <pre>{@code
   * ParameterBinder binder = ParameterBinder.forString("example");
   * try (PreparedStatement stmt = connection.prepareStatement("INSERT INTO table (column) VALUES (?)")) {
   *   binder.bind(stmt, 1);
   *   stmt.executeUpdate();
   * }
   * }</pre>
   *
   * @param statement the {@link PreparedStatement} to bind the value to
   * @param paramIdx the index of the parameter in the prepared statement (1-based)
   * @throws SQLException if an error occurs while binding the value to the statement
   */
  public abstract void bind(PreparedStatement statement, int paramIdx)
          throws SQLException;

  /**
   * null setter
   */
  public static final ParameterBinder null_binder = new ParameterBinder() {
    @Override
    public void bind(final PreparedStatement statement, final int paramIdx) throws SQLException {
      statement.setObject(paramIdx, null);
    }
  };

  /**
   * Creates a {@link ParameterBinder} that binds an integer value to a
   * {@link PreparedStatement}.
   *
   * <p>This method is useful when you need to bind an integer parameter to a
   * specific index in a prepared statement. The returned binder handles the
   * binding logic internally.
   *
   * <p>Example usage:
   * <pre>{@code
   * ParameterBinder binder = ParameterBinder.forInt(42);
   * try (PreparedStatement stmt = connection.prepareStatement("INSERT INTO table (column) VALUES (?)")) {
   *   binder.bind(stmt, 1);
   *   stmt.executeUpdate();
   * }
   * }</pre>
   *
   * @param value the integer value to bind to the prepared statement
   * @return a {@link ParameterBinder} instance that binds the specified integer value
   * @see PreparedStatement#setInt(int, int)
   */
  public static ParameterBinder forInt(int value) {
    final class IntegerParameterBinder extends ParameterBinder {

      @Override
      public void bind(PreparedStatement statement, int paramIdx) throws SQLException {
        statement.setInt(paramIdx, value);
      }

    }
    return new IntegerParameterBinder();
  }

  /**
   * Creates a {@link ParameterBinder} that binds a long value to a
   * {@link PreparedStatement}.
   *
   * <p>This method is useful when you need to bind a long parameter to a specific
   * index in a prepared statement. The returned binder handles the binding logic
   * internally.
   *
   * <p>Example usage:
   * <pre>{@code
   * ParameterBinder binder = ParameterBinder.forLong(123456789L);
   * try (PreparedStatement stmt = connection.prepareStatement(
   *         "INSERT INTO table (long_column) VALUES (?)")) {
   *   binder.bind(stmt, 1);
   *   stmt.executeUpdate();
   * }
   * }</pre>
   *
   * @param value the long value to bind to the prepared statement
   * @return a {@link ParameterBinder} instance that binds the specified long value
   * @see PreparedStatement#setLong(int, long)
   */
  public static ParameterBinder forLong(long value) {
    final class LongParameterBinder extends ParameterBinder {

      @Override
      public void bind(PreparedStatement statement, int paramIdx) throws SQLException {
        statement.setLong(paramIdx, value);
      }

    }
    return new LongParameterBinder();
  }

  /**
   * Creates a {@link ParameterBinder} that binds a string value to a
   * {@link PreparedStatement}.
   *
   * <p>This method is useful when you need to bind a string parameter to a specific
   * index in a prepared statement. The returned binder handles the binding logic
   * internally.
   *
   * <p>Example usage:
   * <pre>{@code
   *   ParameterBinder binder = ParameterBinder.forString("example");
   *   try (PreparedStatement stmt = connection.prepareStatement(
   *           "INSERT INTO table (column) VALUES (?)")) {
   *     binder.bind(stmt, 1);
   *     stmt.executeUpdate();
   *   }
   * }</pre>
   *
   * @param value the string value to bind to the prepared statement
   * @return a {@link ParameterBinder} instance that binds the specified string value
   * @see PreparedStatement#setString(int, String)
   */
  public static ParameterBinder forString(String value) {
    final class StringParameterBinder extends ParameterBinder {

      @Override
      public void bind(PreparedStatement statement, int paramIdx) throws SQLException {
        statement.setString(paramIdx, value);
      }

    }
    return new StringParameterBinder();
  }

  /**
   * Creates a {@link ParameterBinder} that binds a timestamp value to a
   * {@link PreparedStatement}.
   *
   * <p>This method is useful when you need to bind a timestamp parameter to a specific
   * index in a prepared statement. The returned binder handles the binding logic
   * internally.
   *
   * <p>Example usage:
   * <pre>{@code
   *   Timestamp timestamp = new Timestamp(System.currentTimeMillis());
   *   ParameterBinder binder = ParameterBinder.forTimestamp(timestamp);
   *   try (PreparedStatement stmt = connection.prepareStatement(
   *           "INSERT INTO table (timestamp_column) VALUES (?)")) {
   *     binder.bind(stmt, 1);
   *     stmt.executeUpdate();
   *   }
   * }</pre>
   *
   * @param value the timestamp value to bind to the prepared statement
   * @return a {@link ParameterBinder} instance that binds the specified timestamp value
   * @see PreparedStatement#setTimestamp(int, Timestamp)
   */
  public static ParameterBinder forTimestamp(Timestamp value) {
    final class TimestampParameterBinder extends ParameterBinder {

      @Override
      public void bind(PreparedStatement statement, int paramIdx) throws SQLException {
        statement.setTimestamp(paramIdx, value);
      }

    }
    return new TimestampParameterBinder();
  }

  /**
   * Creates a {@link ParameterBinder} that binds a time value to a
   * {@link PreparedStatement}.
   *
   * <p>This method is useful when you need to bind a time parameter to a specific
   * index in a prepared statement. The returned binder handles the binding logic
   * internally.
   *
   * <p>Example usage:
   * <pre>{@code
   *   Time time = new Time(System.currentTimeMillis());
   *   ParameterBinder binder = ParameterBinder.forTime(time);
   *   try (PreparedStatement stmt = connection.prepareStatement(
   *           "INSERT INTO table (time_column) VALUES (?)")) {
   *     binder.bind(stmt, 1);
   *     stmt.executeUpdate();
   *   }
   * }</pre>
   *
   * @param value the time value to bind to the prepared statement
   * @return a {@link ParameterBinder} instance that binds the specified time value
   * @see PreparedStatement#setTime(int, Time)
   */
  public static ParameterBinder forTime(Time value) {
    final class TimeParameterBinder extends ParameterBinder {

      @Override
      public void bind(PreparedStatement statement, int paramIdx) throws SQLException {
        statement.setTime(paramIdx, value);
      }

    }
    return new TimeParameterBinder();
  }

  /**
   * Creates a {@link ParameterBinder} that binds a date value to a
   * {@link PreparedStatement}.
   *
   * <p>This method is useful when you need to bind a date parameter to a specific
   * index in a prepared statement. The returned binder handles the binding logic
   * internally.
   *
   * <p>Example usage:
   * <pre>{@code
   *   Date date = new Date(System.currentTimeMillis());
   *   ParameterBinder binder = ParameterBinder.forDate(date);
   *   try (PreparedStatement stmt = connection.prepareStatement(
   *           "INSERT INTO table (date_column) VALUES (?)")) {
   *     binder.bind(stmt, 1);
   *     stmt.executeUpdate();
   *   }
   * }</pre>
   *
   * @param value the date value to bind to the prepared statement
   * @return a {@link ParameterBinder} instance that binds the specified date value
   * @see PreparedStatement#setDate(int, Date)
   */
  public static ParameterBinder forDate(Date value) {
    final class DateParameterBinder extends ParameterBinder {
      @Override
      public void bind(PreparedStatement statement, int paramIdx) throws SQLException {
        statement.setDate(paramIdx, value);
      }
    }
    return new DateParameterBinder();
  }

  /**
   * Creates a {@link ParameterBinder} that binds a boolean value to a
   * {@link PreparedStatement}.
   *
   * <p>This method is useful when you need to bind a boolean parameter to a specific
   * index in a prepared statement. The returned binder handles the binding logic
   * internally.
   *
   * <p>Example usage:
   * <pre>{@code
   *   ParameterBinder binder = ParameterBinder.forBoolean(true);
   *   try (PreparedStatement stmt = connection.prepareStatement(
   *           "INSERT INTO table (boolean_column) VALUES (?)")) {
   *     binder.bind(stmt, 1);
   *     stmt.executeUpdate();
   *   }
   * }</pre>
   *
   * @param value the boolean value to bind to the prepared statement
   * @return a {@link ParameterBinder} instance that binds the specified boolean value
   * @see PreparedStatement#setBoolean(int, boolean)
   */
  public static ParameterBinder forBoolean(boolean value) {
    final class BooleanParameterBinder extends ParameterBinder {

      @Override
      public void bind(PreparedStatement statement, int paramIdx) throws SQLException {
        statement.setBoolean(paramIdx, value);
      }

    }
    return new BooleanParameterBinder();
  }

  /**
   * Creates a {@link ParameterBinder} that binds an input stream value to a
   * {@link PreparedStatement}.
   *
   * <p>This method is useful when you need to bind binary data, such as a file or blob,
   * to a specific index in a prepared statement. The returned binder handles the binding
   * logic internally.
   *
   * <p>Example usage:
   * <pre>{@code
   *   InputStream inputStream = new FileInputStream("path/to/file");
   *   ParameterBinder binder = ParameterBinder.forBinaryStream(inputStream);
   *   try (PreparedStatement stmt = connection.prepareStatement(
   *           "INSERT INTO table (binary_column) VALUES (?)")) {
   *     binder.bind(stmt, 1);
   *     stmt.executeUpdate();
   *   }
   * }</pre>
   *
   * @param value the input stream value to bind to the prepared statement
   * @return a {@link ParameterBinder} instance that binds the specified input stream value
   * @see PreparedStatement#setBinaryStream(int, InputStream)
   */
  public static ParameterBinder forBinaryStream(InputStream value) {
    final class BinaryStreamParameterBinder extends ParameterBinder {
      @Override
      public void bind(PreparedStatement statement, int paramIdx) throws SQLException {
        statement.setBinaryStream(paramIdx, value);
      }
    }
    return new BinaryStreamParameterBinder();
  }

  /**
   * Creates a {@link ParameterBinder} that binds an object value to a
   * {@link PreparedStatement}.
   *
   * <p>This method is useful when you need to bind an arbitrary object parameter to a
   * specific index in a prepared statement. The returned binder uses the generic
   * setObject method to handle the binding logic.
   *
   * <p>Example usage:
   * <pre>{@code
   *   Object object = new Object();
   *   ParameterBinder binder = ParameterBinder.forObject(object);
   *   try (PreparedStatement stmt = connection.prepareStatement(
   *           "INSERT INTO table (object_column) VALUES (?)")) {
   *     binder.bind(stmt, 1);
   *     stmt.executeUpdate();
   *   }
   * }</pre>
   *
   * @param value the object value to bind to the prepared statement
   * @return a {@link ParameterBinder} instance that binds the specified object value
   * @see PreparedStatement#setObject(int, Object)
   */
  public static ParameterBinder forObject(@Nullable Object value) {
    final class ObjectParameterBinder extends ParameterBinder {

      @Override
      public void bind(PreparedStatement statement, int paramIdx) throws SQLException {
        statement.setObject(paramIdx, value);
      }

    }
    return new ObjectParameterBinder();
  }

  /**
   * Creates a {@link ParameterBinder} that binds an object value to a
   * {@link PreparedStatement} using a custom {@link TypeHandler}.
   *
   * <p>This method is useful when you need to bind a custom object parameter to a
   * specific index in a prepared statement. The provided {@link TypeHandler} handles
   * the conversion and binding logic.
   *
   * <p>Example usage:
   * <pre>{@code
   *   TypeHandler<MyCustomType> typeHandler = new MyCustomTypeHandler();
   *   MyCustomType customValue = new MyCustomType();
   *   ParameterBinder binder = ParameterBinder.forTypeHandler(typeHandler, customValue);
   *   try (PreparedStatement stmt = connection.prepareStatement(
   *           "INSERT INTO table (custom_column) VALUES (?)")) {
   *     binder.bind(stmt, 1);
   *     stmt.executeUpdate();
   *   }
   * }</pre>
   *
   * @param typeHandler the {@link TypeHandler} to use for binding the value
   * @param value the object value to bind to the prepared statement
   * @param <T> the type of the object being bound
   * @return a {@link ParameterBinder} instance that binds the specified object value
   * using the provided {@link TypeHandler}
   */
  public static <T> ParameterBinder forTypeHandler(TypeHandler<T> typeHandler, T value) {
    final class TypeHandlerParameterBinder extends ParameterBinder {
      @Override
      public void bind(PreparedStatement statement, int paramIdx) throws SQLException {
        typeHandler.setParameter(statement, paramIdx, value);
      }
    }
    return new TypeHandlerParameterBinder();
  }

}

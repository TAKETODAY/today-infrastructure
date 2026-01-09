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

package infra.jdbc.type;

import org.jspecify.annotations.Nullable;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Interface for handling the conversion between Java objects and JDBC types.
 *
 * <p>A {@code TypeHandler} is responsible for setting parameters on a
 * {@link PreparedStatement} and retrieving results from a {@link ResultSet}.
 * Implementations define how specific Java types are mapped to SQL types
 * and vice versa.</p>
 *
 * @param <T> value type
 * @author Clinton Begin
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface TypeHandler<T> {

  /**
   * <p>Sets the value of the designated parameter using the given object.
   *
   * <p>The JDBC specification specifies a standard mapping from
   * Java {@code Object} types to SQL types.  The given argument
   * will be converted to the corresponding SQL type before being
   * sent to the database.
   *
   * <p>Note that this method may be used to pass database-specific
   * abstract data types, by using a driver-specific Java type.
   *
   * If the object is of a class implementing the interface {@code SQLData},
   * the JDBC driver should call the method {@code SQLData.writeSQL}
   * to write it to the SQL data stream.
   * If, on the other hand, the object is of a class implementing
   * {@code Ref}, {@code Blob}, {@code Clob},  {@code NClob},
   * {@code Struct}, {@code java.net.URL}, {@code RowId}, {@code SQLXML}
   * or {@code Array}, the driver should pass it to the database as a
   * value of the corresponding SQL type.
   * <P>
   * <b>Note:</b> Not all databases allow for a non-typed Null to be sent to
   * the backend. For maximum portability, the {@code setNull} or the
   * {@code setObject(int parameterIndex, Object x, int sqlType)}
   * method should be used
   * instead of {@code setObject(int parameterIndex, Object x)}.
   * <p>
   * <b>Note:</b> This method throws an exception if there is an ambiguity, for example, if the
   * object is of a class implementing more than one of the interfaces named above.
   *
   * @param parameterIndex the first parameter is 1, the second is 2, ...
   * @param arg the object containing the input parameter value
   * @throws SQLException if parameterIndex does not correspond to a parameter
   * marker in the SQL statement; if a database access error occurs;
   * this method is called on a closed {@code PreparedStatement}
   * or the type of the given object is ambiguous
   */
  void setParameter(PreparedStatement ps, int parameterIndex, @Nullable T arg)
          throws SQLException;

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as a Java object.
   *
   * @param rs the ResultSet object
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return the column value as a Java object; if the value is SQL NULL,
   * the value returned is null
   * @throws SQLException if a database access error occurs or this method is
   * called on a closed result set
   */
  @Nullable
  T getResult(ResultSet rs, int columnIndex) throws SQLException;

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as a Java object.
   *
   * @param rs the ResultSet object
   * @param columnName Column name, when configuration <code>useColumnLabel</code> is <code>false</code>
   * @return the column value as a Java object; if the value is SQL NULL,
   * the value returned is null
   * @throws SQLException the SQL exception
   */
  @Nullable
  default T getResult(ResultSet rs, String columnName) throws SQLException {
    throw new UnsupportedOperationException();
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code CallableStatement} object as a Java object.
   *
   * @param cs the CallableStatement object
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return the column value as a Java object; if the value is SQL NULL,
   * the value returned is null
   * @throws SQLException if a database access error occurs or this method is
   * called on a closed callable statement
   */
  @Nullable
  default T getResult(CallableStatement cs, int columnIndex) throws SQLException {
    throw new UnsupportedOperationException();
  }

}

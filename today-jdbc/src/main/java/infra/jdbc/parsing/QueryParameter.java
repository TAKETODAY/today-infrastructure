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

package infra.jdbc.parsing;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;

import infra.jdbc.ParameterBinder;
import infra.lang.Nullable;

/**
 * Represents a query parameter used in SQL statements. This class encapsulates
 * the name, setter, and applier of a parameter, allowing for flexible binding
 * of values to a {@link PreparedStatement}.
 *
 * <p>Instances of this class are immutable in terms of their name but allow
 * modification of the {@link ParameterBinder} and {@link ParameterIndexHolder}
 * through setter methods.</p>
 *
 * <h3>Usage Example</h3>
 * The following example demonstrates how to use {@code QueryParameter} to bind
 * a value to a {@link PreparedStatement}:
 *
 * <pre>{@code
 * // Create a QueryParameter instance
 * QueryParameter param = new QueryParameter("age", indexHolder);
 *
 * // Set a binder to define how the parameter is bound
 * param.setSetter((statement, index, value) -> statement.setInt(index, (int) value));
 *
 * // Bind the parameter to a PreparedStatement
 * try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM users WHERE age = ?")) {
 *   param.setSetter((statement, index, value) -> statement.setInt(index, (int) value));
 *   param.setTo(stmt);
 * }
 * }</pre>
 *
 * <p>In this example, the {@code QueryParameter} instance is used to bind an integer
 * value to the first parameter of the SQL query.</p>
 *
 * <h3>Key Methods</h3>
 * <ul>
 *   <li>{@link #setTo(PreparedStatement)}: Binds the parameter to a statement.</li>
 *   <li>{@link #setHolder(ParameterIndexHolder)}: Sets the holder responsible for
 *       managing parameter indices.</li>
 *   <li>{@link #setSetter(ParameterBinder)}: Defines how the parameter value is set.</li>
 * </ul>
 *
 * <p>This class also overrides {@link #equals(Object)}, {@link #hashCode()}, and
 * {@link #toString()} to ensure proper behavior in collections and debugging scenarios.</p>
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0 2021/8/22 10:15
 */
public final class QueryParameter {

  private final String name;

  @Nullable
  private ParameterBinder setter;

  private ParameterIndexHolder applier;

  public QueryParameter(String name, ParameterIndexHolder indexHolder) {
    this.name = name;
    this.applier = indexHolder;
  }

  /**
   * set value to given statement
   *
   * @param statement statement
   * @throws SQLException any parameter setting error
   */
  public void setTo(final PreparedStatement statement) throws SQLException {
    if (setter != null) {
      applier.bind(setter, statement);
    }
  }

  public void setHolder(ParameterIndexHolder applier) {
    this.applier = applier;
  }

  public void setSetter(ParameterBinder setter) {
    this.setter = setter;
  }

  public ParameterIndexHolder getHolder() {
    return applier;
  }

  @Nullable
  public ParameterBinder getBinder() {
    return setter;
  }

  public String getName() {
    return name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof final QueryParameter parameter))
      return false;
    return Objects.equals(name, parameter.name)
            && Objects.equals(setter, parameter.setter)
            && Objects.equals(applier, parameter.applier);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, setter, applier);
  }

  @Override
  public String toString() {
    return "QueryParameter: '%s' setter: %s".formatted(name, setter);
  }

}

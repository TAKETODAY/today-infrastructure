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

package infra.jdbc.core;

import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.util.Arrays;
import java.util.List;

import infra.dao.InvalidDataAccessApiUsageException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/8 21:23
 */
class PreparedStatementCreatorFactoryTests {

  @Test
  void shouldCreateFactoryWithSqlOnly() {
    String sql = "SELECT * FROM users";

    PreparedStatementCreatorFactory factory = new PreparedStatementCreatorFactory(sql);

    assertThat(factory.getSql()).isEqualTo(sql);
  }

  @Test
  void shouldCreateFactoryWithSqlAndTypes() {
    String sql = "SELECT * FROM users WHERE id = ? AND name = ?";
    int[] types = { java.sql.Types.INTEGER, java.sql.Types.VARCHAR };

    PreparedStatementCreatorFactory factory = new PreparedStatementCreatorFactory(sql, types);

    assertThat(factory.getSql()).isEqualTo(sql);
  }

  @Test
  void shouldCreateFactoryWithSqlAndParameters() {
    String sql = "SELECT * FROM users WHERE id = ? AND name = ?";
    List<SqlParameter> parameters = Arrays.asList(
            new SqlParameter("id", java.sql.Types.INTEGER),
            new SqlParameter("name", java.sql.Types.VARCHAR)
    );

    PreparedStatementCreatorFactory factory = new PreparedStatementCreatorFactory(sql, parameters);

    assertThat(factory.getSql()).isEqualTo(sql);
  }

  @Test
  void shouldAddParameter() {
    String sql = "SELECT * FROM users WHERE id = ?";
    PreparedStatementCreatorFactory factory = new PreparedStatementCreatorFactory(sql);

    SqlParameter param = new SqlParameter("id", java.sql.Types.INTEGER);
    factory.addParameter(param);

    // Verify by creating a PreparedStatementCreator
    PreparedStatementCreator creator = factory.newPreparedStatementCreator(new Object[] { 1 });
    assertThat(creator).isNotNull();
  }

  @Test
  void shouldSetResultSetType() {
    String sql = "SELECT * FROM users";
    PreparedStatementCreatorFactory factory = new PreparedStatementCreatorFactory(sql);

    factory.setResultSetType(ResultSet.TYPE_SCROLL_INSENSITIVE);

    PreparedStatementCreator creator = factory.newPreparedStatementCreator(new Object[0]);
    assertThat(creator).isNotNull();
  }

  @Test
  void shouldSetUpdatableResults() {
    String sql = "SELECT * FROM users";
    PreparedStatementCreatorFactory factory = new PreparedStatementCreatorFactory(sql);

    factory.setUpdatableResults(true);

    PreparedStatementCreator creator = factory.newPreparedStatementCreator(new Object[0]);
    assertThat(creator).isNotNull();
  }

  @Test
  void shouldSetReturnGeneratedKeys() {
    String sql = "INSERT INTO users (name) VALUES (?)";
    PreparedStatementCreatorFactory factory = new PreparedStatementCreatorFactory(sql);

    factory.setReturnGeneratedKeys(true);

    PreparedStatementCreator creator = factory.newPreparedStatementCreator(new Object[] { "test" });
    assertThat(creator).isNotNull();
  }

  @Test
  void shouldSetGeneratedKeysColumnNames() {
    String sql = "INSERT INTO users (name) VALUES (?)";
    PreparedStatementCreatorFactory factory = new PreparedStatementCreatorFactory(sql);

    factory.setGeneratedKeysColumnNames("id", "created_at");

    PreparedStatementCreator creator = factory.newPreparedStatementCreator(new Object[] { "test" });
    assertThat(creator).isNotNull();
  }

  @Test
  void shouldCreatePreparedStatementSetterWithList() {
    String sql = "SELECT * FROM users WHERE id = ?";
    PreparedStatementCreatorFactory factory = new PreparedStatementCreatorFactory(sql);

    List<Object> params = Arrays.asList(1);
    PreparedStatementSetter setter = factory.newPreparedStatementSetter(params);

    assertThat(setter).isNotNull();
  }

  @Test
  void shouldCreatePreparedStatementSetterWithArray() {
    String sql = "SELECT * FROM users WHERE id = ? AND name = ?";
    PreparedStatementCreatorFactory factory = new PreparedStatementCreatorFactory(sql);

    Object[] params = { 1, "test" };
    PreparedStatementSetter setter = factory.newPreparedStatementSetter(params);

    assertThat(setter).isNotNull();
  }

  @Test
  void shouldCreatePreparedStatementCreatorWithList() {
    String sql = "SELECT * FROM users WHERE id = ?";
    PreparedStatementCreatorFactory factory = new PreparedStatementCreatorFactory(sql);

    List<Object> params = Arrays.asList(1);
    PreparedStatementCreator creator = factory.newPreparedStatementCreator(params);

    assertThat(creator).isNotNull();
  }

  @Test
  void shouldCreatePreparedStatementCreatorWithArray() {
    String sql = "SELECT * FROM users WHERE id = ? AND name = ?";
    PreparedStatementCreatorFactory factory = new PreparedStatementCreatorFactory(sql);

    Object[] params = { 1, "test" };
    PreparedStatementCreator creator = factory.newPreparedStatementCreator(params);

    assertThat(creator).isNotNull();
  }

  @Test
  void shouldCreatePreparedStatementCreatorWithCustomSql() {
    String sql = "SELECT * FROM users WHERE id = :id";
    String actualSql = "SELECT * FROM users WHERE id = ?";
    PreparedStatementCreatorFactory factory = new PreparedStatementCreatorFactory(sql);

    Object[] params = { 1 };
    PreparedStatementCreator creator = factory.newPreparedStatementCreator(actualSql, params);

    assertThat(creator).isNotNull();
  }

  @Test
  void shouldHandleNullParametersInPreparedStatementSetter() {
    String sql = "SELECT * FROM users";
    PreparedStatementCreatorFactory factory = new PreparedStatementCreatorFactory(sql);

    PreparedStatementSetter setter = factory.newPreparedStatementSetter((List<?>) null);
    assertThat(setter).isNotNull();

    PreparedStatementSetter setter2 = factory.newPreparedStatementSetter((Object[]) null);
    assertThat(setter2).isNotNull();
  }

  @Test
  void shouldHandleNullParametersInPreparedStatementCreator() {
    String sql = "SELECT * FROM users";
    PreparedStatementCreatorFactory factory = new PreparedStatementCreatorFactory(sql);

    PreparedStatementCreator creator = factory.newPreparedStatementCreator((List<?>) null);
    assertThat(creator).isNotNull();

    PreparedStatementCreator creator2 = factory.newPreparedStatementCreator((Object[]) null);
    assertThat(creator2).isNotNull();

    PreparedStatementCreator creator3 = factory.newPreparedStatementCreator("SELECT * FROM users", (Object[]) null);
    assertThat(creator3).isNotNull();
  }

  @Test
  void shouldValidateParameterCount() {
    String sql = "SELECT * FROM users WHERE id = ? AND name = ?";
    List<SqlParameter> parameters = Arrays.asList(
            new SqlParameter("id", java.sql.Types.INTEGER),
            new SqlParameter("name", java.sql.Types.VARCHAR)
    );
    PreparedStatementCreatorFactory factory = new PreparedStatementCreatorFactory(sql, parameters);

    // Correct number of parameters
    assertThatCode(() -> factory.newPreparedStatementCreator(Arrays.asList(1, "test")))
            .doesNotThrowAnyException();

    // Incorrect number of parameters
    assertThatThrownBy(() -> factory.newPreparedStatementCreator(Arrays.asList(1)))
            .isInstanceOf(InvalidDataAccessApiUsageException.class);
  }

}
/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.jdbc.core.simple;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import javax.sql.DataSource;

import infra.jdbc.core.SqlParameterValue;
import infra.jdbc.core.namedparam.Customer;
import infra.jdbc.support.GeneratedKeyHolder;
import infra.jdbc.support.KeyHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Juergen Hoeller
 * @since 4.0
 */
public class JdbcClientIndexedParameterTests {

  private static final String SELECT_INDEXED_PARAMETERS =
          "select id, forename from custmr where id = ? and country = ?";

  private static final String SELECT_NO_PARAMETERS =
          "select id, forename from custmr";

  private static final String UPDATE_INDEXED_PARAMETERS =
          "update seat_status set booking_id = null where performance_id = ? and price_band_id = ?";

  private static final String INSERT_GENERATE_KEYS =
          "insert into show (name) values(?)";

  private static final String[] COLUMN_NAMES = new String[] { "id", "forename" };

  private Connection connection = mock();

  private DataSource dataSource = mock();

  private PreparedStatement preparedStatement = mock();

  private ResultSet resultSet = mock();

  private ResultSetMetaData resultSetMetaData = mock();

  private DatabaseMetaData databaseMetaData = mock();

  private JdbcClient client = JdbcClient.create(dataSource);

  private List<Object> params = new ArrayList<>();

  @BeforeEach
  public void setup() throws Exception {
    given(dataSource.getConnection()).willReturn(connection);
    given(connection.prepareStatement(anyString())).willReturn(preparedStatement);
    given(preparedStatement.getConnection()).willReturn(connection);
    given(preparedStatement.executeQuery()).willReturn(resultSet);
    given(databaseMetaData.getDatabaseProductName()).willReturn("MySQL");
    given(databaseMetaData.supportsBatchUpdates()).willReturn(true);
  }

  @Test
  public void queryWithResultSetExtractor() throws SQLException {
    given(resultSet.next()).willReturn(true);
    given(resultSet.getInt("id")).willReturn(1);
    given(resultSet.getString("forename")).willReturn("rod");

    params.add(new SqlParameterValue(Types.DECIMAL, 1));
    params.add("UK");
    Customer cust = client.sql(SELECT_INDEXED_PARAMETERS).params(params).query(
            rs -> {
              rs.next();
              Customer cust1 = new Customer();
              cust1.setId(rs.getInt(COLUMN_NAMES[0]));
              cust1.setForename(rs.getString(COLUMN_NAMES[1]));
              return cust1;
            });

    assertThat(cust.getId()).as("Customer id was assigned correctly").isEqualTo(1);
    assertThat(cust.getForename()).as("Customer forename was assigned correctly").isEqualTo("rod");
    verify(connection).prepareStatement(SELECT_INDEXED_PARAMETERS);
    verify(preparedStatement).setObject(1, 1, Types.DECIMAL);
    verify(preparedStatement).setString(2, "UK");
    verify(resultSet).close();
    verify(preparedStatement).close();
    verify(connection).close();
  }

  @Test
  public void queryWithResultSetExtractorNoParameters() throws SQLException {
    given(resultSet.next()).willReturn(true);
    given(resultSet.getInt("id")).willReturn(1);
    given(resultSet.getString("forename")).willReturn("rod");

    Customer cust = client.sql(SELECT_NO_PARAMETERS).query(
            rs -> {
              rs.next();
              Customer cust1 = new Customer();
              cust1.setId(rs.getInt(COLUMN_NAMES[0]));
              cust1.setForename(rs.getString(COLUMN_NAMES[1]));
              return cust1;
            });

    assertThat(cust.getId()).as("Customer id was assigned correctly").isEqualTo(1);
    assertThat(cust.getForename()).as("Customer forename was assigned correctly").isEqualTo("rod");
    verify(connection).prepareStatement(SELECT_NO_PARAMETERS);
    verify(resultSet).close();
    verify(preparedStatement).close();
    verify(connection).close();
  }

  @Test
  public void queryWithRowCallbackHandler() throws SQLException {
    given(resultSet.next()).willReturn(true, false);
    given(resultSet.getInt("id")).willReturn(1);
    given(resultSet.getString("forename")).willReturn("rod");

    params.add(new SqlParameterValue(Types.DECIMAL, 1));
    params.add("UK");
    final List<Customer> customers = new ArrayList<>();
    client.sql(SELECT_INDEXED_PARAMETERS).params(params).query(rs -> {
      Customer cust = new Customer();
      cust.setId(rs.getInt(COLUMN_NAMES[0]));
      cust.setForename(rs.getString(COLUMN_NAMES[1]));
      customers.add(cust);
    });

    assertThat(customers).hasSize(1);
    assertThat(customers.get(0).getId()).as("Customer id was assigned correctly").isEqualTo(1);
    assertThat(customers.get(0).getForename()).as("Customer forename was assigned correctly").isEqualTo("rod");
    verify(connection).prepareStatement(SELECT_INDEXED_PARAMETERS);
    verify(preparedStatement).setObject(1, 1, Types.DECIMAL);
    verify(preparedStatement).setString(2, "UK");
    verify(resultSet).close();
    verify(preparedStatement).close();
    verify(connection).close();
  }

  @Test
  public void queryWithRowCallbackHandlerNoParameters() throws SQLException {
    given(resultSet.next()).willReturn(true, false);
    given(resultSet.getInt("id")).willReturn(1);
    given(resultSet.getString("forename")).willReturn("rod");

    final List<Customer> customers = new ArrayList<>();
    client.sql(SELECT_NO_PARAMETERS).query(rs -> {
      Customer cust = new Customer();
      cust.setId(rs.getInt(COLUMN_NAMES[0]));
      cust.setForename(rs.getString(COLUMN_NAMES[1]));
      customers.add(cust);
    });

    assertThat(customers).hasSize(1);
    assertThat(customers.get(0).getId()).as("Customer id was assigned correctly").isEqualTo(1);
    assertThat(customers.get(0).getForename()).as("Customer forename was assigned correctly").isEqualTo("rod");
    verify(connection).prepareStatement(SELECT_NO_PARAMETERS);
    verify(resultSet).close();
    verify(preparedStatement).close();
    verify(connection).close();
  }

  @Test
  public void queryWithRowMapper() throws SQLException {
    given(resultSet.next()).willReturn(true, false);
    given(resultSet.getInt("id")).willReturn(1);
    given(resultSet.getString("forename")).willReturn("rod");

    params.add(new SqlParameterValue(Types.DECIMAL, 1));
    params.add("UK");
    List<Customer> customers = client.sql(SELECT_INDEXED_PARAMETERS).params(params).query(
            (rs, rownum) -> {
              Customer cust = new Customer();
              cust.setId(rs.getInt(COLUMN_NAMES[0]));
              cust.setForename(rs.getString(COLUMN_NAMES[1]));
              return cust;
            }).list();

    assertThat(customers).hasSize(1);
    Customer cust = customers.get(0);
    assertThat(cust.getId()).as("Customer id was assigned correctly").isEqualTo(1);
    assertThat(cust.getForename()).as("Customer forename was assigned correctly").isEqualTo("rod");
    verify(connection).prepareStatement(SELECT_INDEXED_PARAMETERS);
    verify(preparedStatement).setObject(1, 1, Types.DECIMAL);
    verify(preparedStatement).setString(2, "UK");
    verify(resultSet).close();
    verify(preparedStatement).close();
    verify(connection).close();
  }

  @Test
  public void queryWithRowMapperNoParameters() throws SQLException {
    given(resultSet.next()).willReturn(true, false);
    given(resultSet.getInt("id")).willReturn(1);
    given(resultSet.getString("forename")).willReturn("rod");

    Set<Customer> customers = client.sql(SELECT_NO_PARAMETERS).query(
            (rs, rownum) -> {
              Customer cust = new Customer();
              cust.setId(rs.getInt(COLUMN_NAMES[0]));
              cust.setForename(rs.getString(COLUMN_NAMES[1]));
              return cust;
            }).set();

    assertThat(customers).hasSize(1);
    Customer cust = customers.iterator().next();
    assertThat(cust.getId()).as("Customer id was assigned correctly").isEqualTo(1);
    assertThat(cust.getForename()).as("Customer forename was assigned correctly").isEqualTo("rod");
    verify(connection).prepareStatement(SELECT_NO_PARAMETERS);
    verify(resultSet).close();
    verify(preparedStatement).close();
    verify(connection).close();
  }

  @Test
  public void queryForObjectWithRowMapper() throws SQLException {
    given(resultSet.next()).willReturn(true, false);
    given(resultSet.getInt("id")).willReturn(1);
    given(resultSet.getString("forename")).willReturn("rod");

    params.add(new SqlParameterValue(Types.DECIMAL, 1));
    params.add("UK");

    Customer cust = client.sql(SELECT_INDEXED_PARAMETERS).params(params).query(
            (rs, rownum) -> {
              Customer cust1 = new Customer();
              cust1.setId(rs.getInt(COLUMN_NAMES[0]));
              cust1.setForename(rs.getString(COLUMN_NAMES[1]));
              return cust1;
            }).single();

    assertThat(cust.getId()).as("Customer id was assigned correctly").isEqualTo(1);
    assertThat(cust.getForename()).as("Customer forename was assigned correctly").isEqualTo("rod");
    verify(connection).prepareStatement(SELECT_INDEXED_PARAMETERS);
    verify(preparedStatement).setObject(1, 1, Types.DECIMAL);
    verify(preparedStatement).setString(2, "UK");
    verify(resultSet).close();
    verify(preparedStatement).close();
    verify(connection).close();
  }

  @Test
  public void queryForStreamWithRowMapper() throws SQLException {
    given(resultSet.next()).willReturn(true, false);
    given(resultSet.getInt("id")).willReturn(1);
    given(resultSet.getString("forename")).willReturn("rod");

    params.add(new SqlParameterValue(Types.DECIMAL, 1));
    params.add("UK");
    AtomicInteger count = new AtomicInteger();

    try (Stream<Customer> s = client.sql(SELECT_INDEXED_PARAMETERS).params(params).query(
            (rs, rownum) -> {
              Customer cust1 = new Customer();
              cust1.setId(rs.getInt(COLUMN_NAMES[0]));
              cust1.setForename(rs.getString(COLUMN_NAMES[1]));
              return cust1;
            }).stream()) {
      s.forEach(cust -> {
        count.incrementAndGet();
        assertThat(cust.getId()).as("Customer id was assigned correctly").isEqualTo(1);
        assertThat(cust.getForename()).as("Customer forename was assigned correctly").isEqualTo("rod");
      });
    }

    assertThat(count.get()).isEqualTo(1);
    verify(connection).prepareStatement(SELECT_INDEXED_PARAMETERS);
    verify(preparedStatement).setObject(1, 1, Types.DECIMAL);
    verify(preparedStatement).setString(2, "UK");
    verify(resultSet).close();
    verify(preparedStatement).close();
    verify(connection).close();
  }

  @Test
  public void update() throws SQLException {
    given(preparedStatement.executeUpdate()).willReturn(1);

    params.add(1);
    params.add(1);
    int rowsAffected = client.sql(UPDATE_INDEXED_PARAMETERS).params(params).update();

    assertThat(rowsAffected).isEqualTo(1);
    verify(connection).prepareStatement(UPDATE_INDEXED_PARAMETERS);
    verify(preparedStatement).setObject(1, 1);
    verify(preparedStatement).setObject(2, 1);
    verify(preparedStatement).close();
    verify(connection).close();
  }

  @Test
  public void updateWithTypedParameters() throws SQLException {
    given(preparedStatement.executeUpdate()).willReturn(1);

    params.add(new SqlParameterValue(Types.DECIMAL, 1));
    params.add(new SqlParameterValue(Types.INTEGER, 1));
    int rowsAffected = client.sql(UPDATE_INDEXED_PARAMETERS).params(params).update();

    assertThat(rowsAffected).isEqualTo(1);
    verify(connection).prepareStatement(UPDATE_INDEXED_PARAMETERS);
    verify(preparedStatement).setObject(1, 1, Types.DECIMAL);
    verify(preparedStatement).setObject(2, 1, Types.INTEGER);
    verify(preparedStatement).close();
    verify(connection).close();
  }

  @Test
  public void updateWithGeneratedKeys() throws SQLException {
    given(resultSetMetaData.getColumnCount()).willReturn(1);
    given(resultSetMetaData.getColumnLabel(1)).willReturn("1");
    given(resultSet.getMetaData()).willReturn(resultSetMetaData);
    given(resultSet.next()).willReturn(true, false);
    given(resultSet.getObject(1)).willReturn(11);
    given(preparedStatement.executeUpdate()).willReturn(1);
    given(preparedStatement.getGeneratedKeys()).willReturn(resultSet);
    given(connection.prepareStatement(INSERT_GENERATE_KEYS, PreparedStatement.RETURN_GENERATED_KEYS))
            .willReturn(preparedStatement);

    KeyHolder generatedKeyHolder = new GeneratedKeyHolder();
    int rowsAffected = client.sql(INSERT_GENERATE_KEYS).param("rod").update(generatedKeyHolder);

    assertThat(rowsAffected).isEqualTo(1);
    assertThat(generatedKeyHolder.getKeyList()).hasSize(1);
    assertThat(generatedKeyHolder.getKey()).isEqualTo(11);
    verify(preparedStatement).setString(1, "rod");
    verify(resultSet).close();
    verify(preparedStatement).close();
    verify(connection).close();
  }

  @Test
  public void updateWithGeneratedKeysAndKeyColumnNames() throws SQLException {
    given(resultSetMetaData.getColumnCount()).willReturn(1);
    given(resultSetMetaData.getColumnLabel(1)).willReturn("1");
    given(resultSet.getMetaData()).willReturn(resultSetMetaData);
    given(resultSet.next()).willReturn(true, false);
    given(resultSet.getObject(1)).willReturn(11);
    given(preparedStatement.executeUpdate()).willReturn(1);
    given(preparedStatement.getGeneratedKeys()).willReturn(resultSet);
    given(connection.prepareStatement(INSERT_GENERATE_KEYS, new String[] { "id" }))
            .willReturn(preparedStatement);

    KeyHolder generatedKeyHolder = new GeneratedKeyHolder();
    int rowsAffected = client.sql(INSERT_GENERATE_KEYS).param("rod").update(generatedKeyHolder, "id");

    assertThat(rowsAffected).isEqualTo(1);
    assertThat(generatedKeyHolder.getKeyList()).hasSize(1);
    assertThat(generatedKeyHolder.getKey()).isEqualTo(11);
    verify(preparedStatement).setString(1, "rod");
    verify(resultSet).close();
    verify(preparedStatement).close();
    verify(connection).close();
  }

}

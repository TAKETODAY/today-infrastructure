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

package infra.jdbc.core.namedparam;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import javax.sql.DataSource;

import infra.jdbc.core.JdbcOperations;
import infra.jdbc.core.JdbcTemplate;
import infra.jdbc.core.PreparedStatementCallback;
import infra.jdbc.core.SqlParameterValue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Nikita Khateev
 * @author Fedor Bobin
 */
public class NamedParameterJdbcTemplateTests {

  private static final String SELECT_NAMED_PARAMETERS =
          "select id, forename from custmr where id = :id and country = :country";
  private static final String SELECT_NAMED_PARAMETERS_PARSED =
          "select id, forename from custmr where id = ? and country = ?";
  private static final String SELECT_NO_PARAMETERS =
          "select id, forename from custmr";

  private static final String UPDATE_NAMED_PARAMETERS =
          "update seat_status set booking_id = null where performance_id = :perfId and price_band_id = :priceId";
  private static final String UPDATE_NAMED_PARAMETERS_PARSED =
          "update seat_status set booking_id = null where performance_id = ? and price_band_id = ?";

  private static final String UPDATE_ARRAY_PARAMETERS =
          "update customer set type = array[:typeIds] where id = :id";
  private static final String UPDATE_ARRAY_PARAMETERS_PARSED =
          "update customer set type = array[?, ?, ?] where id = ?";

  private static final String[] COLUMN_NAMES = new String[] { "id", "forename" };

  private Connection connection;

  private DataSource dataSource;

  private PreparedStatement preparedStatement;

  private ResultSet resultSet;

  private DatabaseMetaData databaseMetaData;

  private Map<String, Object> params = new HashMap<>();

  private NamedParameterJdbcTemplate namedParameterTemplate;

  @BeforeEach
  public void setup() throws Exception {
    connection = mock(Connection.class);
    dataSource = mock(DataSource.class);
    preparedStatement = mock(PreparedStatement.class);
    resultSet = mock(ResultSet.class);
    namedParameterTemplate = new NamedParameterJdbcTemplate(dataSource);
    databaseMetaData = mock(DatabaseMetaData.class);
    given(dataSource.getConnection()).willReturn(connection);
    given(connection.prepareStatement(anyString())).willReturn(preparedStatement);
    given(preparedStatement.getConnection()).willReturn(connection);
    given(preparedStatement.executeQuery()).willReturn(resultSet);
    given(databaseMetaData.getDatabaseProductName()).willReturn("MySQL");
    given(databaseMetaData.supportsBatchUpdates()).willReturn(true);
  }

  @Test
  public void testNullDataSourceProvidedToCtor() {
    assertThatIllegalArgumentException().isThrownBy(() ->
            new NamedParameterJdbcTemplate((DataSource) null));
  }

  @Test
  public void testNullJdbcTemplateProvidedToCtor() {
    assertThatIllegalArgumentException().isThrownBy(() ->
            new NamedParameterJdbcTemplate((JdbcOperations) null));
  }

  @Test
  public void testTemplateConfiguration() {
    assertThat(namedParameterTemplate.getJdbcTemplate().getDataSource()).isSameAs(dataSource);
  }

  @Test
  public void testExecute() throws SQLException {
    given(preparedStatement.executeUpdate()).willReturn(1);

    params.put("perfId", 1);
    params.put("priceId", 1);
    Object result = namedParameterTemplate.execute(UPDATE_NAMED_PARAMETERS, params,
            (PreparedStatementCallback<Object>) ps -> {
              assertThat(ps).isEqualTo(preparedStatement);
              ps.executeUpdate();
              return "result";
            });

    assertThat(result).isEqualTo("result");
    verify(connection).prepareStatement(UPDATE_NAMED_PARAMETERS_PARSED);
    verify(preparedStatement).setObject(1, 1);
    verify(preparedStatement).setObject(2, 1);
    verify(preparedStatement).close();
    verify(connection).close();
  }

  @Disabled("SPR-16340")
  @Test
  public void testExecuteArray() throws SQLException {
    given(preparedStatement.executeUpdate()).willReturn(1);

    List<Integer> typeIds = Arrays.asList(1, 2, 3);

    params.put("typeIds", typeIds);
    params.put("id", 1);
    Object result = namedParameterTemplate.execute(UPDATE_ARRAY_PARAMETERS, params,
            (PreparedStatementCallback<Object>) ps -> {
              assertThat(ps).isEqualTo(preparedStatement);
              ps.executeUpdate();
              return "result";
            });

    assertThat(result).isEqualTo("result");
    verify(connection).prepareStatement(UPDATE_ARRAY_PARAMETERS_PARSED);
    verify(preparedStatement).setObject(1, 1);
    verify(preparedStatement).setObject(2, 2);
    verify(preparedStatement).setObject(3, 3);
    verify(preparedStatement).setObject(4, 1);
    verify(preparedStatement).close();
    verify(connection).close();
  }

  @Test
  public void testExecuteWithTypedParameters() throws SQLException {
    given(preparedStatement.executeUpdate()).willReturn(1);

    params.put("perfId", new SqlParameterValue(Types.DECIMAL, 1));
    params.put("priceId", new SqlParameterValue(Types.INTEGER, 1));
    Object result = namedParameterTemplate.execute(UPDATE_NAMED_PARAMETERS, params,
            (PreparedStatementCallback<Object>) ps -> {
              assertThat(ps).isEqualTo(preparedStatement);
              ps.executeUpdate();
              return "result";
            });

    assertThat(result).isEqualTo("result");
    verify(connection).prepareStatement(UPDATE_NAMED_PARAMETERS_PARSED);
    verify(preparedStatement).setObject(1, 1, Types.DECIMAL);
    verify(preparedStatement).setObject(2, 1, Types.INTEGER);
    verify(preparedStatement).close();
    verify(connection).close();
  }

  @Test
  public void testExecuteNoParameters() throws SQLException {
    given(preparedStatement.executeUpdate()).willReturn(1);

    Object result = namedParameterTemplate.execute(SELECT_NO_PARAMETERS,
            (PreparedStatementCallback<Object>) ps -> {
              assertThat(ps).isEqualTo(preparedStatement);
              ps.executeQuery();
              return "result";
            });

    assertThat(result).isEqualTo("result");
    verify(connection).prepareStatement(SELECT_NO_PARAMETERS);
    verify(preparedStatement).close();
    verify(connection).close();
  }

  @Test
  public void testQueryWithResultSetExtractor() throws SQLException {
    given(resultSet.next()).willReturn(true);
    given(resultSet.getInt("id")).willReturn(1);
    given(resultSet.getString("forename")).willReturn("rod");

    params.put("id", new SqlParameterValue(Types.DECIMAL, 1));
    params.put("country", "UK");
    Customer cust = namedParameterTemplate.query(SELECT_NAMED_PARAMETERS, params,
            rs -> {
              rs.next();
              Customer cust1 = new Customer();
              cust1.setId(rs.getInt(COLUMN_NAMES[0]));
              cust1.setForename(rs.getString(COLUMN_NAMES[1]));
              return cust1;
            });

    assertThat(cust.getId() == 1).as("Customer id was assigned correctly").isTrue();
    assertThat(cust.getForename().equals("rod")).as("Customer forename was assigned correctly").isTrue();
    verify(connection).prepareStatement(SELECT_NAMED_PARAMETERS_PARSED);
    verify(preparedStatement).setObject(1, 1, Types.DECIMAL);
    verify(preparedStatement).setString(2, "UK");
    verify(resultSet).close();
    verify(preparedStatement).close();
    verify(connection).close();
  }

  @Test
  public void testQueryWithResultSetExtractorNoParameters() throws SQLException {
    given(resultSet.next()).willReturn(true);
    given(resultSet.getInt("id")).willReturn(1);
    given(resultSet.getString("forename")).willReturn("rod");

    Customer cust = namedParameterTemplate.query(SELECT_NO_PARAMETERS,
            rs -> {
              rs.next();
              Customer cust1 = new Customer();
              cust1.setId(rs.getInt(COLUMN_NAMES[0]));
              cust1.setForename(rs.getString(COLUMN_NAMES[1]));
              return cust1;
            });

    assertThat(cust.getId() == 1).as("Customer id was assigned correctly").isTrue();
    assertThat(cust.getForename().equals("rod")).as("Customer forename was assigned correctly").isTrue();
    verify(connection).prepareStatement(SELECT_NO_PARAMETERS);
    verify(resultSet).close();
    verify(preparedStatement).close();
    verify(connection).close();
  }

  @Test
  public void testQueryWithRowCallbackHandler() throws SQLException {
    given(resultSet.next()).willReturn(true, false);
    given(resultSet.getInt("id")).willReturn(1);
    given(resultSet.getString("forename")).willReturn("rod");

    params.put("id", new SqlParameterValue(Types.DECIMAL, 1));
    params.put("country", "UK");
    final List<Customer> customers = new ArrayList<>();
    namedParameterTemplate.query(SELECT_NAMED_PARAMETERS, params, rs -> {
      Customer cust = new Customer();
      cust.setId(rs.getInt(COLUMN_NAMES[0]));
      cust.setForename(rs.getString(COLUMN_NAMES[1]));
      customers.add(cust);
    });

    assertThat(customers.size()).isEqualTo(1);
    assertThat(customers.get(0).getId() == 1).as("Customer id was assigned correctly").isTrue();
    assertThat(customers.get(0).getForename().equals("rod")).as("Customer forename was assigned correctly").isTrue();
    verify(connection).prepareStatement(SELECT_NAMED_PARAMETERS_PARSED);
    verify(preparedStatement).setObject(1, 1, Types.DECIMAL);
    verify(preparedStatement).setString(2, "UK");
    verify(resultSet).close();
    verify(preparedStatement).close();
    verify(connection).close();
  }

  @Test
  public void testQueryWithRowCallbackHandlerNoParameters() throws SQLException {
    given(resultSet.next()).willReturn(true, false);
    given(resultSet.getInt("id")).willReturn(1);
    given(resultSet.getString("forename")).willReturn("rod");

    final List<Customer> customers = new ArrayList<>();
    namedParameterTemplate.query(SELECT_NO_PARAMETERS, rs -> {
      Customer cust = new Customer();
      cust.setId(rs.getInt(COLUMN_NAMES[0]));
      cust.setForename(rs.getString(COLUMN_NAMES[1]));
      customers.add(cust);
    });

    assertThat(customers.size()).isEqualTo(1);
    assertThat(customers.get(0).getId() == 1).as("Customer id was assigned correctly").isTrue();
    assertThat(customers.get(0).getForename().equals("rod")).as("Customer forename was assigned correctly").isTrue();
    verify(connection).prepareStatement(SELECT_NO_PARAMETERS);
    verify(resultSet).close();
    verify(preparedStatement).close();
    verify(connection).close();
  }

  @Test
  public void testQueryWithRowMapper() throws SQLException {
    given(resultSet.next()).willReturn(true, false);
    given(resultSet.getInt("id")).willReturn(1);
    given(resultSet.getString("forename")).willReturn("rod");

    params.put("id", new SqlParameterValue(Types.DECIMAL, 1));
    params.put("country", "UK");
    List<Customer> customers = namedParameterTemplate.query(SELECT_NAMED_PARAMETERS, params,
            (rs, rownum) -> {
              Customer cust = new Customer();
              cust.setId(rs.getInt(COLUMN_NAMES[0]));
              cust.setForename(rs.getString(COLUMN_NAMES[1]));
              return cust;
            });

    assertThat(customers.size()).isEqualTo(1);
    assertThat(customers.get(0).getId() == 1).as("Customer id was assigned correctly").isTrue();
    assertThat(customers.get(0).getForename().equals("rod")).as("Customer forename was assigned correctly").isTrue();
    verify(connection).prepareStatement(SELECT_NAMED_PARAMETERS_PARSED);
    verify(preparedStatement).setObject(1, 1, Types.DECIMAL);
    verify(preparedStatement).setString(2, "UK");
    verify(resultSet).close();
    verify(preparedStatement).close();
    verify(connection).close();
  }

  @Test
  public void testQueryWithRowMapperNoParameters() throws SQLException {
    given(resultSet.next()).willReturn(true, false);
    given(resultSet.getInt("id")).willReturn(1);
    given(resultSet.getString("forename")).willReturn("rod");

    List<Customer> customers = namedParameterTemplate.query(SELECT_NO_PARAMETERS,
            (rs, rownum) -> {
              Customer cust = new Customer();
              cust.setId(rs.getInt(COLUMN_NAMES[0]));
              cust.setForename(rs.getString(COLUMN_NAMES[1]));
              return cust;
            });

    assertThat(customers.size()).isEqualTo(1);
    assertThat(customers.get(0).getId() == 1).as("Customer id was assigned correctly").isTrue();
    assertThat(customers.get(0).getForename().equals("rod")).as("Customer forename was assigned correctly").isTrue();
    verify(connection).prepareStatement(SELECT_NO_PARAMETERS);
    verify(resultSet).close();
    verify(preparedStatement).close();
    verify(connection).close();
  }

  @Test
  public void testQueryForObjectWithRowMapper() throws SQLException {
    given(resultSet.next()).willReturn(true, false);
    given(resultSet.getInt("id")).willReturn(1);
    given(resultSet.getString("forename")).willReturn("rod");

    params.put("id", new SqlParameterValue(Types.DECIMAL, 1));
    params.put("country", "UK");

    Customer cust = namedParameterTemplate.queryForObject(SELECT_NAMED_PARAMETERS, params,
            (rs, rownum) -> {
              Customer cust1 = new Customer();
              cust1.setId(rs.getInt(COLUMN_NAMES[0]));
              cust1.setForename(rs.getString(COLUMN_NAMES[1]));
              return cust1;
            });

    assertThat(cust.getId() == 1).as("Customer id was assigned correctly").isTrue();
    assertThat(cust.getForename().equals("rod")).as("Customer forename was assigned correctly").isTrue();
    verify(connection).prepareStatement(SELECT_NAMED_PARAMETERS_PARSED);
    verify(preparedStatement).setObject(1, 1, Types.DECIMAL);
    verify(preparedStatement).setString(2, "UK");
    verify(resultSet).close();
    verify(preparedStatement).close();
    verify(connection).close();
  }

  @Test
  public void testQueryForStreamWithRowMapper() throws SQLException {
    given(resultSet.next()).willReturn(true, false);
    given(resultSet.getInt("id")).willReturn(1);
    given(resultSet.getString("forename")).willReturn("rod");

    params.put("id", new SqlParameterValue(Types.DECIMAL, 1));
    params.put("country", "UK");
    AtomicInteger count = new AtomicInteger();

    try (Stream<Customer> s = namedParameterTemplate.queryForStream(SELECT_NAMED_PARAMETERS, params,
            (rs, rownum) -> {
              Customer cust1 = new Customer();
              cust1.setId(rs.getInt(COLUMN_NAMES[0]));
              cust1.setForename(rs.getString(COLUMN_NAMES[1]));
              return cust1;
            })) {
      s.forEach(cust -> {
        count.incrementAndGet();
        assertThat(cust.getId() == 1).as("Customer id was assigned correctly").isTrue();
        assertThat(cust.getForename().equals("rod")).as("Customer forename was assigned correctly").isTrue();
      });
    }

    assertThat(count.get()).isEqualTo(1);
    verify(connection).prepareStatement(SELECT_NAMED_PARAMETERS_PARSED);
    verify(preparedStatement).setObject(1, 1, Types.DECIMAL);
    verify(preparedStatement).setString(2, "UK");
    verify(resultSet).close();
    verify(preparedStatement).close();
    verify(connection).close();
  }

  @Test
  public void testUpdate() throws SQLException {
    given(preparedStatement.executeUpdate()).willReturn(1);

    params.put("perfId", 1);
    params.put("priceId", 1);
    int rowsAffected = namedParameterTemplate.update(UPDATE_NAMED_PARAMETERS, params);

    assertThat(rowsAffected).isEqualTo(1);
    verify(connection).prepareStatement(UPDATE_NAMED_PARAMETERS_PARSED);
    verify(preparedStatement).setObject(1, 1);
    verify(preparedStatement).setObject(2, 1);
    verify(preparedStatement).close();
    verify(connection).close();
  }

  @Test
  public void testUpdateWithTypedParameters() throws SQLException {
    given(preparedStatement.executeUpdate()).willReturn(1);

    params.put("perfId", new SqlParameterValue(Types.DECIMAL, 1));
    params.put("priceId", new SqlParameterValue(Types.INTEGER, 1));
    int rowsAffected = namedParameterTemplate.update(UPDATE_NAMED_PARAMETERS, params);

    assertThat(rowsAffected).isEqualTo(1);
    verify(connection).prepareStatement(UPDATE_NAMED_PARAMETERS_PARSED);
    verify(preparedStatement).setObject(1, 1, Types.DECIMAL);
    verify(preparedStatement).setObject(2, 1, Types.INTEGER);
    verify(preparedStatement).close();
    verify(connection).close();
  }

  @Test
  public void testBatchUpdateWithPlainMap() throws Exception {
    @SuppressWarnings("unchecked") final Map<String, Integer>[] ids = new Map[2];
    ids[0] = Collections.singletonMap("id", 100);
    ids[1] = Collections.singletonMap("id", 200);
    final int[] rowsAffected = new int[] { 1, 2 };

    given(preparedStatement.executeBatch()).willReturn(rowsAffected);
    given(connection.getMetaData()).willReturn(databaseMetaData);
    namedParameterTemplate = new NamedParameterJdbcTemplate(new JdbcTemplate(dataSource, false));

    int[] actualRowsAffected = namedParameterTemplate.batchUpdate(
            "UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = :id", ids);
    assertThat(actualRowsAffected.length == 2).as("executed 2 updates").isTrue();
    assertThat(actualRowsAffected[0]).isEqualTo(rowsAffected[0]);
    assertThat(actualRowsAffected[1]).isEqualTo(rowsAffected[1]);
    verify(connection).prepareStatement("UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = ?");
    verify(preparedStatement).setObject(1, 100);
    verify(preparedStatement).setObject(1, 200);
    verify(preparedStatement, times(2)).addBatch();
    verify(preparedStatement, atLeastOnce()).close();
    verify(connection, atLeastOnce()).close();
  }

  @Test
  public void testBatchUpdateWithEmptyMap() throws Exception {
    @SuppressWarnings("unchecked") final Map<String, Integer>[] ids = new Map[0];
    namedParameterTemplate = new NamedParameterJdbcTemplate(new JdbcTemplate(dataSource, false));

    int[] actualRowsAffected = namedParameterTemplate.batchUpdate(
            "UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = :id", ids);
    assertThat(actualRowsAffected.length == 0).as("executed 0 updates").isTrue();
  }

  @Test
  public void testBatchUpdateWithSqlParameterSource() throws Exception {
    SqlParameterSource[] ids = new SqlParameterSource[2];
    ids[0] = new MapSqlParameterSource("id", 100);
    ids[1] = new MapSqlParameterSource("id", 200);
    final int[] rowsAffected = new int[] { 1, 2 };

    given(preparedStatement.executeBatch()).willReturn(rowsAffected);
    given(connection.getMetaData()).willReturn(databaseMetaData);
    namedParameterTemplate = new NamedParameterJdbcTemplate(new JdbcTemplate(dataSource, false));

    int[] actualRowsAffected = namedParameterTemplate.batchUpdate(
            "UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = :id", ids);
    assertThat(actualRowsAffected.length == 2).as("executed 2 updates").isTrue();
    assertThat(actualRowsAffected[0]).isEqualTo(rowsAffected[0]);
    assertThat(actualRowsAffected[1]).isEqualTo(rowsAffected[1]);
    verify(connection).prepareStatement("UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = ?");
    verify(preparedStatement).setObject(1, 100);
    verify(preparedStatement).setObject(1, 200);
    verify(preparedStatement, times(2)).addBatch();
    verify(preparedStatement, atLeastOnce()).close();
    verify(connection, atLeastOnce()).close();
  }

  @Test
  public void testBatchUpdateWithInClause() throws Exception {
    @SuppressWarnings("unchecked")
    Map<String, Object>[] parameters = new Map[3];
    parameters[0] = Collections.singletonMap("ids", Arrays.asList(1, 2));
    parameters[1] = Collections.singletonMap("ids", Arrays.asList("3", "4"));
    parameters[2] = Collections.singletonMap("ids", (Iterable<Integer>) () -> Arrays.asList(5, 6).iterator());

    final int[] rowsAffected = new int[] { 1, 2, 3 };
    given(preparedStatement.executeBatch()).willReturn(rowsAffected);
    given(connection.getMetaData()).willReturn(databaseMetaData);

    JdbcTemplate template = new JdbcTemplate(dataSource, false);
    namedParameterTemplate = new NamedParameterJdbcTemplate(template);

    int[] actualRowsAffected = namedParameterTemplate.batchUpdate(
            "delete sometable where id in (:ids)",
            parameters
    );

    assertThat(actualRowsAffected.length).as("executed 3 updates").isEqualTo(3);

    InOrder inOrder = inOrder(preparedStatement);

    inOrder.verify(preparedStatement).setObject(1, 1);
    inOrder.verify(preparedStatement).setObject(2, 2);
    inOrder.verify(preparedStatement).addBatch();

    inOrder.verify(preparedStatement).setString(1, "3");
    inOrder.verify(preparedStatement).setString(2, "4");
    inOrder.verify(preparedStatement).addBatch();

    inOrder.verify(preparedStatement).setObject(1, 5);
    inOrder.verify(preparedStatement).setObject(2, 6);
    inOrder.verify(preparedStatement).addBatch();

    inOrder.verify(preparedStatement, atLeastOnce()).close();
    verify(connection, atLeastOnce()).close();
  }

  @Test
  public void testBatchUpdateWithSqlParameterSourcePlusTypeInfo() throws Exception {
    SqlParameterSource[] ids = new SqlParameterSource[3];
    ids[0] = new MapSqlParameterSource().addValue("id", null, Types.NULL);
    ids[1] = new MapSqlParameterSource().addValue("id", 100, Types.NUMERIC);
    ids[2] = new MapSqlParameterSource().addValue("id", 200, Types.NUMERIC);
    final int[] rowsAffected = new int[] { 1, 2, 3 };

    given(preparedStatement.executeBatch()).willReturn(rowsAffected);
    given(connection.getMetaData()).willReturn(databaseMetaData);
    namedParameterTemplate = new NamedParameterJdbcTemplate(new JdbcTemplate(dataSource, false));

    int[] actualRowsAffected = namedParameterTemplate.batchUpdate(
            "UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = :id", ids);
    assertThat(actualRowsAffected.length == 3).as("executed 3 updates").isTrue();
    assertThat(actualRowsAffected[0]).isEqualTo(rowsAffected[0]);
    assertThat(actualRowsAffected[1]).isEqualTo(rowsAffected[1]);
    assertThat(actualRowsAffected[2]).isEqualTo(rowsAffected[2]);
    verify(connection).prepareStatement("UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = ?");
    verify(preparedStatement).setNull(1, Types.NULL);
    verify(preparedStatement).setObject(1, 100, Types.NUMERIC);
    verify(preparedStatement).setObject(1, 200, Types.NUMERIC);
    verify(preparedStatement, times(3)).addBatch();
    verify(preparedStatement, atLeastOnce()).close();
    verify(connection, atLeastOnce()).close();
  }

}

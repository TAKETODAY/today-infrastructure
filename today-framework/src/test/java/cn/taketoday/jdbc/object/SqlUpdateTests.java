/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.jdbc.object;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import cn.taketoday.jdbc.JdbcUpdateAffectedIncorrectNumberOfRowsException;
import cn.taketoday.jdbc.core.SqlParameter;
import cn.taketoday.jdbc.support.GeneratedKeyHolder;
import cn.taketoday.jdbc.support.KeyHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Trevor Cook
 * @author Thomas Risberg
 * @author Juergen Hoeller
 */
public class SqlUpdateTests {

  private static final String UPDATE =
          "update seat_status set booking_id = null";

  private static final String UPDATE_INT =
          "update seat_status set booking_id = null where performance_id = ?";

  private static final String UPDATE_INT_INT =
          "update seat_status set booking_id = null where performance_id = ? and price_band_id = ?";

  private static final String UPDATE_NAMED_PARAMETERS =
          "update seat_status set booking_id = null where performance_id = :perfId and price_band_id = :priceId";

  private static final String UPDATE_STRING =
          "update seat_status set booking_id = null where name = ?";

  private static final String UPDATE_OBJECTS =
          "update seat_status set booking_id = null where performance_id = ? and price_band_id = ? and name = ? and confirmed = ?";

  private static final String INSERT_GENERATE_KEYS =
          "insert into show (name) values(?)";

  private DataSource dataSource;

  private Connection connection;

  private PreparedStatement preparedStatement;

  private ResultSet resultSet;

  private ResultSetMetaData resultSetMetaData;

  @BeforeEach
  public void setUp() throws Exception {
    dataSource = mock(DataSource.class);
    connection = mock(Connection.class);
    preparedStatement = mock(PreparedStatement.class);
    resultSet = mock(ResultSet.class);
    resultSetMetaData = mock(ResultSetMetaData.class);
    given(dataSource.getConnection()).willReturn(connection);
  }

  @AfterEach
  public void verifyClosed() throws Exception {
    verify(preparedStatement).close();
    verify(connection).close();
  }

  @Test
  public void testUpdate() throws SQLException {
    given(preparedStatement.executeUpdate()).willReturn(1);
    given(connection.prepareStatement(UPDATE)).willReturn(preparedStatement);

    Updater pc = new Updater();
    int rowsAffected = pc.run();

    assertThat(rowsAffected).isEqualTo(1);
  }

  @Test
  public void testUpdateInt() throws SQLException {
    given(preparedStatement.executeUpdate()).willReturn(1);
    given(connection.prepareStatement(UPDATE_INT)).willReturn(preparedStatement);

    IntUpdater pc = new IntUpdater();
    int rowsAffected = pc.run(1);

    assertThat(rowsAffected).isEqualTo(1);
    verify(preparedStatement).setObject(1, 1, Types.NUMERIC);
  }

  @Test
  public void testUpdateIntInt() throws SQLException {
    given(preparedStatement.executeUpdate()).willReturn(1);
    given(connection.prepareStatement(UPDATE_INT_INT)).willReturn(preparedStatement);

    IntIntUpdater pc = new IntIntUpdater();
    int rowsAffected = pc.run(1, 1);

    assertThat(rowsAffected).isEqualTo(1);
    verify(preparedStatement).setObject(1, 1, Types.NUMERIC);
    verify(preparedStatement).setObject(2, 1, Types.NUMERIC);
  }

  @Test
  public void testNamedParameterUpdateWithUnnamedDeclarations() throws SQLException {
    doTestNamedParameterUpdate(false);
  }

  @Test
  public void testNamedParameterUpdateWithNamedDeclarations() throws SQLException {
    doTestNamedParameterUpdate(true);
  }

  private void doTestNamedParameterUpdate(final boolean namedDeclarations)
          throws SQLException {
    given(preparedStatement.executeUpdate()).willReturn(1);
    given(connection.prepareStatement(UPDATE_INT_INT)).willReturn(preparedStatement);

    class NamedParameterUpdater extends SqlUpdate {
      public NamedParameterUpdater() {
        setSql(UPDATE_NAMED_PARAMETERS);
        setDataSource(dataSource);
        if (namedDeclarations) {
          declareParameter(new SqlParameter("priceId", Types.DECIMAL));
          declareParameter(new SqlParameter("perfId", Types.NUMERIC));
        }
        else {
          declareParameter(new SqlParameter(Types.NUMERIC));
          declareParameter(new SqlParameter(Types.DECIMAL));
        }
        compile();
      }

      public int run(int performanceId, int type) {
        Map<String, Integer> params = new HashMap<>();
        params.put("perfId", performanceId);
        params.put("priceId", type);
        return updateByNamedParam(params);
      }
    }

    NamedParameterUpdater pc = new NamedParameterUpdater();
    int rowsAffected = pc.run(1, 1);
    assertThat(rowsAffected).isEqualTo(1);
    verify(preparedStatement).setObject(1, 1, Types.NUMERIC);
    verify(preparedStatement).setObject(2, 1, Types.DECIMAL);
  }

  @Test
  public void testUpdateString() throws SQLException {
    given(preparedStatement.executeUpdate()).willReturn(1);
    given(connection.prepareStatement(UPDATE_STRING)).willReturn(preparedStatement);

    StringUpdater pc = new StringUpdater();
    int rowsAffected = pc.run("rod");

    assertThat(rowsAffected).isEqualTo(1);
    verify(preparedStatement).setString(1, "rod");
  }

  @Test
  public void testUpdateMixed() throws SQLException {
    given(preparedStatement.executeUpdate()).willReturn(1);
    given(connection.prepareStatement(UPDATE_OBJECTS)).willReturn(preparedStatement);

    MixedUpdater pc = new MixedUpdater();
    int rowsAffected = pc.run(1, 1, "rod", true);

    assertThat(rowsAffected).isEqualTo(1);
    verify(preparedStatement).setObject(1, 1, Types.NUMERIC);
    verify(preparedStatement).setObject(2, 1, Types.NUMERIC, 2);
    verify(preparedStatement).setString(3, "rod");
    verify(preparedStatement).setBoolean(4, Boolean.TRUE);
  }

  @Test
  public void testUpdateAndGeneratedKeys() throws SQLException {
    given(resultSetMetaData.getColumnCount()).willReturn(1);
    given(resultSetMetaData.getColumnLabel(1)).willReturn("1");
    given(resultSet.getMetaData()).willReturn(resultSetMetaData);
    given(resultSet.next()).willReturn(true, false);
    given(resultSet.getObject(1)).willReturn(11);
    given(preparedStatement.executeUpdate()).willReturn(1);
    given(preparedStatement.getGeneratedKeys()).willReturn(resultSet);
    given(connection.prepareStatement(INSERT_GENERATE_KEYS,
            PreparedStatement.RETURN_GENERATED_KEYS)
    ).willReturn(preparedStatement);

    GeneratedKeysUpdater pc = new GeneratedKeysUpdater();
    KeyHolder generatedKeyHolder = new GeneratedKeyHolder();
    int rowsAffected = pc.run("rod", generatedKeyHolder);

    assertThat(rowsAffected).isEqualTo(1);
    assertThat(generatedKeyHolder.getKeyList().size()).isEqualTo(1);
    assertThat(generatedKeyHolder.getKey().intValue()).isEqualTo(11);
    verify(preparedStatement).setString(1, "rod");
    verify(resultSet).close();
  }

  @Test
  public void testUpdateConstructor() throws SQLException {
    given(preparedStatement.executeUpdate()).willReturn(1);
    given(connection.prepareStatement(UPDATE_OBJECTS)).willReturn(preparedStatement);
    ConstructorUpdater pc = new ConstructorUpdater();

    int rowsAffected = pc.run(1, 1, "rod", true);

    assertThat(rowsAffected).isEqualTo(1);
    verify(preparedStatement).setObject(1, 1, Types.NUMERIC);
    verify(preparedStatement).setObject(2, 1, Types.NUMERIC);
    verify(preparedStatement).setString(3, "rod");
    verify(preparedStatement).setBoolean(4, Boolean.TRUE);
  }

  @Test
  public void testUnderMaxRows() throws SQLException {
    given(preparedStatement.executeUpdate()).willReturn(3);
    given(connection.prepareStatement(UPDATE)).willReturn(preparedStatement);

    MaxRowsUpdater pc = new MaxRowsUpdater();

    int rowsAffected = pc.run();
    assertThat(rowsAffected).isEqualTo(3);
  }

  @Test
  public void testMaxRows() throws SQLException {
    given(preparedStatement.executeUpdate()).willReturn(5);
    given(connection.prepareStatement(UPDATE)).willReturn(preparedStatement);

    MaxRowsUpdater pc = new MaxRowsUpdater();
    int rowsAffected = pc.run();

    assertThat(rowsAffected).isEqualTo(5);
  }

  @Test
  public void testOverMaxRows() throws SQLException {
    given(preparedStatement.executeUpdate()).willReturn(8);
    given(connection.prepareStatement(UPDATE)).willReturn(preparedStatement);

    MaxRowsUpdater pc = new MaxRowsUpdater();

    assertThatExceptionOfType(JdbcUpdateAffectedIncorrectNumberOfRowsException.class).isThrownBy(
            pc::run);
  }

  @Test
  public void testRequiredRows() throws SQLException {
    given(preparedStatement.executeUpdate()).willReturn(3);
    given(connection.prepareStatement(UPDATE)).willReturn(preparedStatement);

    RequiredRowsUpdater pc = new RequiredRowsUpdater();
    int rowsAffected = pc.run();

    assertThat(rowsAffected).isEqualTo(3);
  }

  @Test
  public void testNotRequiredRows() throws SQLException {
    given(preparedStatement.executeUpdate()).willReturn(2);
    given(connection.prepareStatement(UPDATE)).willReturn(preparedStatement);
    RequiredRowsUpdater pc = new RequiredRowsUpdater();
    assertThatExceptionOfType(JdbcUpdateAffectedIncorrectNumberOfRowsException.class).isThrownBy(
            pc::run);
  }

  private class Updater extends SqlUpdate {

    public Updater() {
      setSql(UPDATE);
      setDataSource(dataSource);
      compile();
    }

    public int run() {
      return update();
    }
  }

  private class IntUpdater extends SqlUpdate {

    public IntUpdater() {
      setSql(UPDATE_INT);
      setDataSource(dataSource);
      declareParameter(new SqlParameter(Types.NUMERIC));
      compile();
    }

    public int run(int performanceId) {
      return update(performanceId);
    }
  }

  private class IntIntUpdater extends SqlUpdate {

    public IntIntUpdater() {
      setSql(UPDATE_INT_INT);
      setDataSource(dataSource);
      declareParameter(new SqlParameter(Types.NUMERIC));
      declareParameter(new SqlParameter(Types.NUMERIC));
      compile();
    }

    public int run(int performanceId, int type) {
      return update(performanceId, type);
    }
  }

  private class StringUpdater extends SqlUpdate {

    public StringUpdater() {
      setSql(UPDATE_STRING);
      setDataSource(dataSource);
      declareParameter(new SqlParameter(Types.VARCHAR));
      compile();
    }

    public int run(String name) {
      return update(name);
    }
  }

  private class MixedUpdater extends SqlUpdate {

    public MixedUpdater() {
      setSql(UPDATE_OBJECTS);
      setDataSource(dataSource);
      declareParameter(new SqlParameter(Types.NUMERIC));
      declareParameter(new SqlParameter(Types.NUMERIC, 2));
      declareParameter(new SqlParameter(Types.VARCHAR));
      declareParameter(new SqlParameter(Types.BOOLEAN));
      compile();
    }

    public int run(int performanceId, int type, String name, boolean confirmed) {
      return update(performanceId, type, name, confirmed);
    }
  }

  private class GeneratedKeysUpdater extends SqlUpdate {

    public GeneratedKeysUpdater() {
      setSql(INSERT_GENERATE_KEYS);
      setDataSource(dataSource);
      declareParameter(new SqlParameter(Types.VARCHAR));
      setReturnGeneratedKeys(true);
      compile();
    }

    public int run(String name, KeyHolder generatedKeyHolder) {
      return update(new Object[] { name }, generatedKeyHolder);
    }
  }

  private class ConstructorUpdater extends SqlUpdate {

    public ConstructorUpdater() {
      super(dataSource, UPDATE_OBJECTS,
              new int[] { Types.NUMERIC, Types.NUMERIC, Types.VARCHAR, Types.BOOLEAN });
      compile();
    }

    public int run(int performanceId, int type, String name, boolean confirmed) {
      return update(performanceId, type, name, confirmed);
    }
  }

  private class MaxRowsUpdater extends SqlUpdate {

    public MaxRowsUpdater() {
      setSql(UPDATE);
      setDataSource(dataSource);
      setMaxRowsAffected(5);
      compile();
    }

    public int run() {
      return update();
    }
  }

  private class RequiredRowsUpdater extends SqlUpdate {

    public RequiredRowsUpdater() {
      setSql(UPDATE);
      setDataSource(dataSource);
      setRequiredRowsAffected(3);
      compile();
    }

    public int run() {
      return update();
    }
  }

}

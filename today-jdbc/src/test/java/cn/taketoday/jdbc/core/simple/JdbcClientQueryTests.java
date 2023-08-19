/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.jdbc.core.simple;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Juergen Hoeller
 * @since 4.0
 */
public class JdbcClientQueryTests {

  private Connection connection = mock();

  private DataSource dataSource = mock();

  private PreparedStatement preparedStatement = mock();

  private ResultSet resultSet = mock();

  private ResultSetMetaData resultSetMetaData = mock();

  private JdbcClient client = JdbcClient.create(dataSource);

  @BeforeEach
  public void setup() throws Exception {
    given(dataSource.getConnection()).willReturn(connection);
    given(resultSetMetaData.getColumnCount()).willReturn(1);
    given(resultSetMetaData.getColumnLabel(1)).willReturn("age");
    given(connection.prepareStatement(anyString())).willReturn(preparedStatement);
    given(preparedStatement.executeQuery()).willReturn(resultSet);
  }

  @AfterEach
  public void verifyClose() throws Exception {
    verify(preparedStatement).close();
    verify(resultSet).close();
    verify(connection).close();
  }

  // Indexed parameters

  @Test
  public void testQueryForListWithIndexedParam() throws Exception {
    given(resultSet.getMetaData()).willReturn(resultSetMetaData);
    given(resultSet.next()).willReturn(true, true, false);
    given(resultSet.getObject(1)).willReturn(11, 12);

    List<Map<String, Object>> li = client.sql("SELECT AGE FROM CUSTMR WHERE ID < ?")
            .param(3).query().listOfRows();

    assertThat(li.size()).as("All rows returned").isEqualTo(2);
    assertThat(li.get(0).get("age")).as("First row is Integer").isEqualTo(11);
    assertThat(li.get(1).get("age")).as("Second row is Integer").isEqualTo(12);

    verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID < ?");
    verify(preparedStatement).setObject(1, 3);
  }

  @Test
  public void testQueryForListWithIndexedParamAndEmptyResult() throws Exception {
    given(resultSet.next()).willReturn(false);

    List<Map<String, Object>> li = client.sql("SELECT AGE FROM CUSTMR WHERE ID < ?")
            .param(3).query().listOfRows();

    assertThat(li.size()).as("All rows returned").isEqualTo(0);
    verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID < ?");
    verify(preparedStatement).setObject(1, 3);
  }

  @Test
  public void testQueryForListWithIndexedParamAndSingleRow() throws Exception {
    given(resultSet.getMetaData()).willReturn(resultSetMetaData);
    given(resultSet.next()).willReturn(true, false);
    given(resultSet.getObject(1)).willReturn(11);

    List<Map<String, Object>> li = client.sql("SELECT AGE FROM CUSTMR WHERE ID < ?")
            .param(3).query().listOfRows();

    assertThat(li.size()).as("All rows returned").isEqualTo(1);
    assertThat(li.get(0).get("age")).as("First row is Integer").isEqualTo(11);
    verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID < ?");
    verify(preparedStatement).setObject(1, 3);
  }

  @Test
  public void testQueryForListWithIndexedParamAndSingleColumn() throws Exception {
    given(resultSet.getMetaData()).willReturn(resultSetMetaData);
    given(resultSet.next()).willReturn(true, false);
    given(resultSet.getObject(1)).willReturn(11);

    List<Integer> li = client.sql("SELECT AGE FROM CUSTMR WHERE ID < ?")
            .param(1, 3)
            .query().singleColumn();

    assertThat(li.size()).as("All rows returned").isEqualTo(1);
    assertThat(li.get(0)).as("First row is Integer").isEqualTo(11);
    verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID < ?");
    verify(preparedStatement).setObject(1, 3);
  }

  @Test
  public void testQueryForMapWithIndexedParamAndSingleRow() throws Exception {
    given(resultSet.getMetaData()).willReturn(resultSetMetaData);
    given(resultSet.next()).willReturn(true, false);
    given(resultSet.getObject(1)).willReturn(11);

    Map<String, Object> map = client.sql("SELECT AGE FROM CUSTMR WHERE ID < ?")
            .param(1, 3)
            .query().singleRow();

    assertThat(map.get("age")).as("Row is Integer").isEqualTo(11);
    verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID < ?");
    verify(preparedStatement).setObject(1, 3);
  }

  @Test
  public void testQueryForObjectWithIndexedParamAndRowMapper() throws Exception {
    given(resultSet.next()).willReturn(true, false);
    given(resultSet.getInt(1)).willReturn(22);

    Integer value = client.sql("SELECT AGE FROM CUSTMR WHERE ID = ?")
            .param(1, 3)
            .query((rs, rowNum) -> rs.getInt(1)).single();

    assertThat(value).isEqualTo(22);
    verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID = ?");
    verify(preparedStatement).setObject(1, 3);
  }

  @Test
  public void testQueryForOptionalWithIndexedParamAndRowMapper() throws Exception {
    given(resultSet.next()).willReturn(true, false);
    given(resultSet.getInt(1)).willReturn(22);

    Optional<Integer> value = client.sql("SELECT AGE FROM CUSTMR WHERE ID = ?")
            .param(1, 3)
            .query((rs, rowNum) -> rs.getInt(1)).optional();

    assertThat(value.get()).isEqualTo(22);
    verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID = ?");
    verify(preparedStatement).setObject(1, 3);
  }

  @Test
  public void testQueryForObjectWithIndexedParamAndInteger() throws Exception {
    given(resultSet.getMetaData()).willReturn(resultSetMetaData);
    given(resultSet.next()).willReturn(true, false);
    given(resultSet.getObject(1)).willReturn(22);

    Integer value = client.sql("SELECT AGE FROM CUSTMR WHERE ID = ?")
            .param(1, 3)
            .query().singleValue();

    assertThat(value).isEqualTo(22);
    verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID = ?");
    verify(preparedStatement).setObject(1, 3);
  }

  @Test
  public void testQueryForIntWithIndexedParam() throws Exception {
    given(resultSet.getMetaData()).willReturn(resultSetMetaData);
    given(resultSet.next()).willReturn(true, false);
    given(resultSet.getObject(1)).willReturn(22);

    int i = client.sql("SELECT AGE FROM CUSTMR WHERE ID = ?")
            .param(1, 3)
            .query().singleValue();

    assertThat(i).as("Return of an int").isEqualTo(22);
    verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID = ?");
    verify(preparedStatement).setObject(1, 3);
  }

  // Named parameters

  @Test
  public void testQueryForListWithNamedParam() throws Exception {
    given(resultSet.getMetaData()).willReturn(resultSetMetaData);
    given(resultSet.next()).willReturn(true, true, false);
    given(resultSet.getObject(1)).willReturn(11, 12);

    List<Map<String, Object>> li = client.sql("SELECT AGE FROM CUSTMR WHERE ID < :id")
            .param("id", 3)
            .query().listOfRows();

    assertThat(li.size()).as("All rows returned").isEqualTo(2);
    assertThat(li.get(0).get("age")).as("First row is Integer").isEqualTo(11);
    assertThat(li.get(1).get("age")).as("Second row is Integer").isEqualTo(12);

    verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID < ?");
    verify(preparedStatement).setObject(1, 3);
  }

  @Test
  public void testQueryForListWithNamedParamAndEmptyResult() throws Exception {
    given(resultSet.next()).willReturn(false);

    List<Map<String, Object>> li = client.sql("SELECT AGE FROM CUSTMR WHERE ID < :id")
            .param("id", 3)
            .query().listOfRows();

    assertThat(li.size()).as("All rows returned").isEqualTo(0);
    verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID < ?");
    verify(preparedStatement).setObject(1, 3);
  }

  @Test
  public void testQueryForListWithNamedParamAndSingleRow() throws Exception {
    given(resultSet.getMetaData()).willReturn(resultSetMetaData);
    given(resultSet.next()).willReturn(true, false);
    given(resultSet.getObject(1)).willReturn(11);

    List<Map<String, Object>> li = client.sql("SELECT AGE FROM CUSTMR WHERE ID < :id")
            .param("id", 3)
            .query().listOfRows();

    assertThat(li.size()).as("All rows returned").isEqualTo(1);
    assertThat(li.get(0).get("age")).as("First row is Integer").isEqualTo(11);
    verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID < ?");
    verify(preparedStatement).setObject(1, 3);
  }

  @Test
  public void testQueryForListWithNamedParamAndSingleColumn() throws Exception {
    given(resultSet.getMetaData()).willReturn(resultSetMetaData);
    given(resultSet.next()).willReturn(true, false);
    given(resultSet.getObject(1)).willReturn(11);

    List<Integer> li = client.sql("SELECT AGE FROM CUSTMR WHERE ID < :id")
            .param("id", 3)
            .query().singleColumn();

    assertThat(li.size()).as("All rows returned").isEqualTo(1);
    assertThat(li.get(0)).as("First row is Integer").isEqualTo(11);
    verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID < ?");
    verify(preparedStatement).setObject(1, 3);
  }

  @Test
  public void testQueryForMapWithNamedParamAndSingleRow() throws Exception {
    given(resultSet.getMetaData()).willReturn(resultSetMetaData);
    given(resultSet.next()).willReturn(true, false);
    given(resultSet.getObject(1)).willReturn(11);

    Map<String, Object> map = client.sql("SELECT AGE FROM CUSTMR WHERE ID < :id")
            .param("id", 3)
            .query().singleRow();

    assertThat(map.get("age")).as("Row is Integer").isEqualTo(11);
    verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID < ?");
    verify(preparedStatement).setObject(1, 3);
  }

  @Test
  public void testQueryForObjectWithNamedParamAndRowMapper() throws Exception {
    given(resultSet.next()).willReturn(true, false);
    given(resultSet.getInt(1)).willReturn(22);

    Integer value = client.sql("SELECT AGE FROM CUSTMR WHERE ID = :id")
            .param("id", 3)
            .query((rs, rowNum) -> rs.getInt(1))
            .single();

    assertThat(value).isEqualTo(22);
    verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID = ?");
    verify(preparedStatement).setObject(1, 3);
  }

  @Test
  public void testQueryForObjectWithNamedParamAndMappedSimpleValue() throws Exception {
    given(resultSet.getMetaData()).willReturn(resultSetMetaData);
    given(resultSet.next()).willReturn(true, false);
    given(resultSet.getInt(1)).willReturn(22);

    Integer value = client.sql("SELECT AGE FROM CUSTMR WHERE ID = :id")
            .param("id", 3)
            .query(Integer.class)
            .single();

    assertThat(value).isEqualTo(22);
    verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID = ?");
    verify(preparedStatement).setObject(1, 3);
  }

  @Test
  public void testQueryForObjectWithNamedParamAndMappedRecord() throws Exception {
    given(resultSet.getMetaData()).willReturn(resultSetMetaData);
    given(resultSet.findColumn("age")).willReturn(1);
    given(resultSet.next()).willReturn(true, false);
    given(resultSet.getInt(1)).willReturn(22);

    AgeRecord value = client.sql("SELECT AGE FROM CUSTMR WHERE ID = :id")
            .param("id", 3)
            .query(AgeRecord.class)
            .single();

    assertThat(value.age()).isEqualTo(22);
    verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID = ?");
    verify(preparedStatement).setObject(1, 3);
  }

  @Test
  public void testQueryForObjectWithNamedParamAndMappedFieldHolder() throws Exception {
    given(resultSet.getMetaData()).willReturn(resultSetMetaData);
    given(resultSet.next()).willReturn(true, false);
    given(resultSet.getInt(1)).willReturn(22);

    AgeFieldHolder value = client.sql("SELECT AGE FROM CUSTMR WHERE ID = :id")
            .param("id", 3)
            .query(AgeFieldHolder.class)
            .single();

    assertThat(value.age).isEqualTo(22);
    verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID = ?");
    verify(preparedStatement).setObject(1, 3);
  }

  @Test
  public void testQueryForObjectWithNamedParamAndInteger() throws Exception {
    given(resultSet.getMetaData()).willReturn(resultSetMetaData);
    given(resultSet.next()).willReturn(true, false);
    given(resultSet.getObject(1)).willReturn(22);

    Integer value = client.sql("SELECT AGE FROM CUSTMR WHERE ID = :id")
            .param("id", 3)
            .query().singleValue();

    assertThat(value).isEqualTo(22);
    verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID = ?");
    verify(preparedStatement).setObject(1, 3);
  }

  @Test
  public void testQueryForObjectWithNamedParamAndList() throws Exception {
    given(resultSet.getMetaData()).willReturn(resultSetMetaData);
    given(resultSet.next()).willReturn(true, false);
    given(resultSet.getObject(1)).willReturn(22);

    Integer value = client.sql("SELECT AGE FROM CUSTMR WHERE ID IN (:ids)")
            .param("ids", Arrays.asList(3, 4))
            .query().singleValue();

    assertThat(value).isEqualTo(22);
    verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID IN (?, ?)");
    verify(preparedStatement).setObject(1, 3);
  }

  @Test
  public void testQueryForObjectWithNamedParamAndListOfExpressionLists() throws Exception {
    given(resultSet.getMetaData()).willReturn(resultSetMetaData);
    given(resultSet.next()).willReturn(true, false);
    given(resultSet.getObject(1)).willReturn(22);

    List<Object[]> l1 = new ArrayList<>();
    l1.add(new Object[] { 3, "Rod" });
    l1.add(new Object[] { 4, "Juergen" });
    Integer value = client.sql("SELECT AGE FROM CUSTMR WHERE (ID, NAME) IN (:multiExpressionList)")
            .param("multiExpressionList", l1)
            .query().singleValue();

    assertThat(value).isEqualTo(22);
    verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE (ID, NAME) IN ((?, ?), (?, ?))");
    verify(preparedStatement).setObject(1, 3);
  }

  @Test
  public void testQueryForIntWithNamedParam() throws Exception {
    given(resultSet.getMetaData()).willReturn(resultSetMetaData);
    given(resultSet.next()).willReturn(true, false);
    given(resultSet.getObject(1)).willReturn(22);

    int i = client.sql("SELECT AGE FROM CUSTMR WHERE ID = :id")
            .param("id", 3)
            .query().singleValue();

    assertThat(i).as("Return of an int").isEqualTo(22);
    verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID = ?");
    verify(preparedStatement).setObject(1, 3);
  }

  @Test
  public void testQueryForLongWithParamBean() throws Exception {
    given(resultSet.getMetaData()).willReturn(resultSetMetaData);
    given(resultSet.next()).willReturn(true, false);
    given(resultSet.getLong(1)).willReturn(87L);

    long l = client.sql("SELECT AGE FROM CUSTMR WHERE ID = :id")
            .paramSource(new ParameterBean(3))
            .query(Long.class).single();

    assertThat(l).as("Return of a long").isEqualTo(87);
    verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID = ?");
    verify(preparedStatement).setObject(1, 3, Types.INTEGER);
  }

  @Test
  public void testQueryForLongWithParamBeanWithCollection() throws Exception {
    given(resultSet.getMetaData()).willReturn(resultSetMetaData);
    given(resultSet.next()).willReturn(true, false);
    given(resultSet.getLong(1)).willReturn(87L);

    long l = client.sql("SELECT AGE FROM CUSTMR WHERE ID IN (:ids)")
            .paramSource(new ParameterCollectionBean(3, 5))
            .query(Long.class).single();

    assertThat(l).as("Return of a long").isEqualTo(87);
    verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID IN (?, ?)");
    verify(preparedStatement).setObject(1, 3);
    verify(preparedStatement).setObject(2, 5);
  }

  @Test
  public void testQueryForLongWithParamRecord() throws Exception {
    given(resultSet.getMetaData()).willReturn(resultSetMetaData);
    given(resultSet.next()).willReturn(true, false);
    given(resultSet.getLong(1)).willReturn(87L);

    long l = client.sql("SELECT AGE FROM CUSTMR WHERE ID = :id")
            .paramSource(new ParameterRecord(3))
            .query(Long.class).single();

    assertThat(l).as("Return of a long").isEqualTo(87);
    verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID = ?");
    verify(preparedStatement).setObject(1, 3, Types.INTEGER);
  }

  @Test
  public void testQueryForLongWithParamFieldHolder() throws Exception {
    given(resultSet.getMetaData()).willReturn(resultSetMetaData);
    given(resultSet.next()).willReturn(true, false);
    given(resultSet.getLong(1)).willReturn(87L);

    long l = client.sql("SELECT AGE FROM CUSTMR WHERE ID = :id")
            .paramSource(new ParameterFieldHolder(3))
            .query(Long.class).single();

    assertThat(l).as("Return of a long").isEqualTo(87);
    verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID = ?");
    verify(preparedStatement).setObject(1, 3, Types.INTEGER);
  }

  static class ParameterBean {

    private final int id;

    public ParameterBean(int id) {
      this.id = id;
    }

    public int getId() {
      return id;
    }
  }

  static class ParameterCollectionBean {

    private final Collection<Integer> ids;

    public ParameterCollectionBean(Integer... ids) {
      this.ids = Arrays.asList(ids);
    }

    public Collection<Integer> getIds() {
      return ids;
    }
  }

  record ParameterRecord(int id) {
  }

  static class ParameterFieldHolder {

    public ParameterFieldHolder(int id) {
      this.id = id;
    }

    public int id;
  }

  record AgeRecord(int age) {
  }

  static class AgeFieldHolder {

    public int age;
  }

}

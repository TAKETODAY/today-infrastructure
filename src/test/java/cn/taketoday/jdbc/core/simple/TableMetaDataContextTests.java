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

package cn.taketoday.jdbc.core.simple;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import cn.taketoday.jdbc.core.SqlParameterValue;
import cn.taketoday.jdbc.core.metadata.TableMetaDataContext;
import cn.taketoday.jdbc.core.namedparam.MapSqlParameterSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Mock object based tests for TableMetaDataContext.
 *
 * @author Thomas Risberg
 */
public class TableMetaDataContextTests {

  private Connection connection;

  private DataSource dataSource;

  private DatabaseMetaData databaseMetaData;

  private TableMetaDataContext context = new TableMetaDataContext();

  @BeforeEach
  public void setUp() throws Exception {
    connection = mock(Connection.class);
    dataSource = mock(DataSource.class);
    databaseMetaData = mock(DatabaseMetaData.class);
    given(connection.getMetaData()).willReturn(databaseMetaData);
    given(dataSource.getConnection()).willReturn(connection);
  }

  @Test
  public void testMatchInParametersAndSqlTypeInfoWrapping() throws Exception {
    final String TABLE = "customers";
    final String USER = "me";

    ResultSet metaDataResultSet = mock(ResultSet.class);
    given(metaDataResultSet.next()).willReturn(true, false);
    given(metaDataResultSet.getString("TABLE_SCHEM")).willReturn(USER);
    given(metaDataResultSet.getString("TABLE_NAME")).willReturn(TABLE);
    given(metaDataResultSet.getString("TABLE_TYPE")).willReturn("TABLE");

    ResultSet columnsResultSet = mock(ResultSet.class);
    given(columnsResultSet.next()).willReturn(
            true, true, true, true, false);
    given(columnsResultSet.getString("COLUMN_NAME")).willReturn(
            "id", "name", "customersince", "version");
    given(columnsResultSet.getInt("DATA_TYPE")).willReturn(
            Types.INTEGER, Types.VARCHAR, Types.DATE, Types.NUMERIC);
    given(columnsResultSet.getBoolean("NULLABLE")).willReturn(
            false, true, true, false);

    given(databaseMetaData.getDatabaseProductName()).willReturn("MyDB");
    given(databaseMetaData.getDatabaseProductName()).willReturn("1.0");
    given(databaseMetaData.getUserName()).willReturn(USER);
    given(databaseMetaData.storesLowerCaseIdentifiers()).willReturn(true);
    given(databaseMetaData.getTables(null, null, TABLE, null)).willReturn(metaDataResultSet);
    given(databaseMetaData.getColumns(null, USER, TABLE, null)).willReturn(columnsResultSet);

    MapSqlParameterSource map = new MapSqlParameterSource();
    map.addValue("id", 1);
    map.addValue("name", "Sven");
    map.addValue("customersince", new Date());
    map.addValue("version", 0);
    map.registerSqlType("customersince", Types.DATE);
    map.registerSqlType("version", Types.NUMERIC);

    context.setTableName(TABLE);
    context.processMetaData(dataSource, new ArrayList<>(), new String[] {});

    List<Object> values = context.matchInParameterValuesWithInsertColumns(map);

    assertThat(values.size()).as("wrong number of parameters: ").isEqualTo(4);
    boolean condition3 = values.get(0) instanceof Number;
    assertThat(condition3).as("id not wrapped with type info").isTrue();
    boolean condition2 = values.get(1) instanceof String;
    assertThat(condition2).as("name not wrapped with type info").isTrue();
    boolean condition1 = values.get(2) instanceof SqlParameterValue;
    assertThat(condition1).as("date wrapped with type info").isTrue();
    boolean condition = values.get(3) instanceof SqlParameterValue;
    assertThat(condition).as("version wrapped with type info").isTrue();
    verify(metaDataResultSet, atLeastOnce()).next();
    verify(columnsResultSet, atLeastOnce()).next();
    verify(metaDataResultSet).close();
    verify(columnsResultSet).close();
  }

  @Test
  public void testTableWithSingleColumnGeneratedKey() throws Exception {
    final String TABLE = "customers";
    final String USER = "me";

    ResultSet metaDataResultSet = mock(ResultSet.class);
    given(metaDataResultSet.next()).willReturn(true, false);
    given(metaDataResultSet.getString("TABLE_SCHEM")).willReturn(USER);
    given(metaDataResultSet.getString("TABLE_NAME")).willReturn(TABLE);
    given(metaDataResultSet.getString("TABLE_TYPE")).willReturn("TABLE");

    ResultSet columnsResultSet = mock(ResultSet.class);
    given(columnsResultSet.next()).willReturn(true, false);
    given(columnsResultSet.getString("COLUMN_NAME")).willReturn("id");
    given(columnsResultSet.getInt("DATA_TYPE")).willReturn(Types.INTEGER);
    given(columnsResultSet.getBoolean("NULLABLE")).willReturn(false);

    given(databaseMetaData.getDatabaseProductName()).willReturn("MyDB");
    given(databaseMetaData.getDatabaseProductName()).willReturn("1.0");
    given(databaseMetaData.getUserName()).willReturn(USER);
    given(databaseMetaData.storesLowerCaseIdentifiers()).willReturn(true);
    given(databaseMetaData.getTables(null, null, TABLE, null)).willReturn(metaDataResultSet);
    given(databaseMetaData.getColumns(null, USER, TABLE, null)).willReturn(columnsResultSet);

    MapSqlParameterSource map = new MapSqlParameterSource();
    String[] keyCols = new String[] { "id" };
    context.setTableName(TABLE);
    context.processMetaData(dataSource, new ArrayList<>(), keyCols);
    List<Object> values = context.matchInParameterValuesWithInsertColumns(map);
    String insertString = context.createInsertString(keyCols);

    assertThat(values.size()).as("wrong number of parameters: ").isEqualTo(0);
    assertThat(insertString).as("empty insert not generated correctly").isEqualTo("INSERT INTO customers () VALUES()");
    verify(metaDataResultSet, atLeastOnce()).next();
    verify(columnsResultSet, atLeastOnce()).next();
    verify(metaDataResultSet).close();
    verify(columnsResultSet).close();
  }

}

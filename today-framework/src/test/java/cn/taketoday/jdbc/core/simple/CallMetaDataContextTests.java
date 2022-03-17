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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import cn.taketoday.jdbc.core.SqlInOutParameter;
import cn.taketoday.jdbc.core.SqlOutParameter;
import cn.taketoday.jdbc.core.SqlParameter;
import cn.taketoday.jdbc.core.metadata.CallMetaDataContext;
import cn.taketoday.jdbc.core.namedparam.MapSqlParameterSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Mock object based tests for CallMetaDataContext.
 *
 * @author Thomas Risberg
 */
public class CallMetaDataContextTests {

  private DataSource dataSource;

  private Connection connection;

  private DatabaseMetaData databaseMetaData;

  private CallMetaDataContext context = new CallMetaDataContext();

  @BeforeEach
  public void setUp() throws Exception {
    connection = mock(Connection.class);
    databaseMetaData = mock(DatabaseMetaData.class);
    given(connection.getMetaData()).willReturn(databaseMetaData);
    dataSource = mock(DataSource.class);
    given(dataSource.getConnection()).willReturn(connection);
  }

  @AfterEach
  public void verifyClosed() throws Exception {
    verify(connection).close();
  }

  @Test
  public void testMatchParameterValuesAndSqlInOutParameters() throws Exception {
    final String TABLE = "customers";
    final String USER = "me";
    given(databaseMetaData.getDatabaseProductName()).willReturn("MyDB");
    given(databaseMetaData.getUserName()).willReturn(USER);
    given(databaseMetaData.storesLowerCaseIdentifiers()).willReturn(true);

    List<SqlParameter> parameters = new ArrayList<>();
    parameters.add(new SqlParameter("id", Types.NUMERIC));
    parameters.add(new SqlInOutParameter("name", Types.NUMERIC));
    parameters.add(new SqlOutParameter("customer_no", Types.NUMERIC));

    MapSqlParameterSource parameterSource = new MapSqlParameterSource();
    parameterSource.addValue("id", 1);
    parameterSource.addValue("name", "Sven");
    parameterSource.addValue("customer_no", "12345XYZ");

    context.setProcedureName(TABLE);
    context.initializeMetaData(dataSource);
    context.processParameters(parameters);

    Map<String, Object> inParameters = context.matchInParameterValuesWithCallParameters(parameterSource);
    assertThat(inParameters.size()).as("Wrong number of matched in parameter values").isEqualTo(2);
    assertThat(inParameters.containsKey("id")).as("in parameter value missing").isTrue();
    assertThat(inParameters.containsKey("name")).as("in out parameter value missing").isTrue();
    boolean condition = !inParameters.containsKey("customer_no");
    assertThat(condition).as("out parameter value matched").isTrue();

    List<String> names = context.getOutParameterNames();
    assertThat(names.size()).as("Wrong number of out parameters").isEqualTo(2);

    List<SqlParameter> callParameters = context.getCallParameters();
    assertThat(callParameters.size()).as("Wrong number of call parameters").isEqualTo(3);
  }

}

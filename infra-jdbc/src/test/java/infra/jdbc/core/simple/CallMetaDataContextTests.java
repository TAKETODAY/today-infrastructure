/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.jdbc.core.simple;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import infra.jdbc.core.SqlInOutParameter;
import infra.jdbc.core.SqlOutParameter;
import infra.jdbc.core.SqlParameter;
import infra.jdbc.core.metadata.CallMetaDataContext;
import infra.jdbc.core.namedparam.MapSqlParameterSource;

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

  @Test
  void reconcileParametersMatchesFunctionReturnParameterDeclaredBeforeOutParameter() throws Exception {
    initializeGetTotalFunctionMetaData();

    List<SqlParameter> parameters = List.of(
            new SqlOutParameter("RESULT", Types.INTEGER),
            new SqlOutParameter("out_status", Types.INTEGER));

    context.setFunction(true);
    context.setProcedureName("GET_TOTAL");
    context.initializeMetaData(dataSource);
    context.processParameters(parameters);

    assertThat(context.getCallParameters()).extracting(SqlParameter::getName)
            .containsExactly("RESULT", "AMOUNT", "out_status");
  }

  @Test
  void reconcileParametersMatchesFunctionReturnParameterDeclaredAfterOutParameter() throws Exception {
    initializeGetTotalFunctionMetaData();

    List<SqlParameter> parameters = List.of(
            new SqlOutParameter("out_status", Types.INTEGER),
            new SqlOutParameter("RESULT", Types.INTEGER));

    context.setFunction(true);
    context.setProcedureName("GET_TOTAL");
    context.initializeMetaData(dataSource);
    context.processParameters(parameters);

    assertThat(context.getCallParameters()).extracting(SqlParameter::getName)
            .containsExactly("RESULT", "AMOUNT", "out_status");
  }

  private void initializeGetTotalFunctionMetaData() throws SQLException {
    ResultSet proceduresResultSet = mock();
    ResultSet procedureColumnsResultSet = mock();
    given(databaseMetaData.getDatabaseProductName()).willReturn("Oracle");
    given(databaseMetaData.getUserName()).willReturn("ME");
    given(databaseMetaData.storesUpperCaseIdentifiers()).willReturn(true);
    given(databaseMetaData.getProcedures("", "ME", "GET_TOTAL")).willReturn(proceduresResultSet);
    given(databaseMetaData.getProcedureColumns("", "ME", "GET_TOTAL", null)).willReturn(procedureColumnsResultSet);
    given(proceduresResultSet.next()).willReturn(true, false);
    given(proceduresResultSet.getString("PROCEDURE_NAME")).willReturn("GET_TOTAL");
    given(procedureColumnsResultSet.next()).willReturn(true, true, true, false);
    given(procedureColumnsResultSet.getInt("DATA_TYPE")).willReturn(Types.INTEGER);
    given(procedureColumnsResultSet.getString("COLUMN_NAME")).willReturn(null, "amount", "out_status");
    given(procedureColumnsResultSet.getInt("COLUMN_TYPE")).willReturn(5, 1, 4);
  }

}

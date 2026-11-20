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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import infra.jdbc.core.SqlInOutParameter;
import infra.jdbc.core.SqlOutParameter;
import infra.jdbc.core.SqlParameter;
import infra.jdbc.core.SqlParameterValue;
import infra.jdbc.core.SqlReturnResultSet;
import infra.jdbc.core.metadata.CallMetaDataContext;
import infra.jdbc.core.namedparam.MapSqlParameterSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
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

  @Test
  void matchesParameterSourceUsingExactLowercaseCamelCaseAndMixedCaseNames() throws SQLException {
    initializeWithoutColumnMetadata("OtherDB");
    context.processParameters(List.of(
            new SqlParameter("EXACT", Types.INTEGER), new SqlParameter("LOWER", Types.INTEGER),
            new SqlParameter("USER_NAME", Types.VARCHAR), new SqlInOutParameter("STATUS", Types.INTEGER),
            new SqlParameter("MISSING", Types.INTEGER), new SqlOutParameter("OUTPUT", Types.INTEGER)));
    var source = new MapSqlParameterSource().addValue("EXACT", 1).addValue("lower", 2)
            .addValue("userName", "Ada", Types.VARCHAR).addValue("StAtUs", 3).addValue("OUTPUT", 4);

    var values = context.matchInParameterValuesWithCallParameters(source);

    assertThat(values).containsOnlyKeys("EXACT", "LOWER", "USER_NAME", "STATUS")
            .containsEntry("EXACT", 1).containsEntry("LOWER", 2).containsEntry("STATUS", 3);
    assertThat(values.get("USER_NAME")).isInstanceOfSatisfying(SqlParameterValue.class, value -> {
      assertThat(value.getValue()).isEqualTo("Ada");
      assertThat(value.getSqlType()).isEqualTo(Types.VARCHAR);
    });
    assertThat(context.getScalarOutParameterName()).isEqualTo("STATUS");
    var map = Map.of("extra", 1);
    assertThat(context.matchInParameterValuesWithCallParameters(map)).isSameAs(map);
  }

  @ParameterizedTest
  @ValueSource(booleans = { true, false })
  void reconcilesSqlServerParametersAndFiltersMapValues(boolean returnValueRequired) throws SQLException {
    var procedures = mock(ResultSet.class);
    var columns = mock(ResultSet.class);
    given(databaseMetaData.getDatabaseProductName()).willReturn("Microsoft SQL Server");
    given(databaseMetaData.getProcedures(null, null, "adjust")).willReturn(procedures);
    given(databaseMetaData.getProcedureColumns(null, null, "adjust", null)).willReturn(columns);
    given(procedures.next()).willReturn(true, false);
    given(procedures.getString("PROCEDURE_NAME")).willReturn("adjust");
    given(columns.next()).willReturn(true, true, true, true, true, false);
    given(columns.getString("COLUMN_NAME")).willReturn("@RETURN_VALUE", "@AMOUNT", "@OTHER", "@TOTAL", "@STATUS");
    given(columns.getInt("COLUMN_TYPE")).willReturn(DatabaseMetaData.procedureColumnReturn,
            DatabaseMetaData.procedureColumnIn, DatabaseMetaData.procedureColumnIn,
            DatabaseMetaData.procedureColumnInOut, DatabaseMetaData.procedureColumnOut);
    given(columns.getInt("DATA_TYPE")).willReturn(Types.INTEGER);
    context.setProcedureName("adjust");
    context.setReturnValueRequired(returnValueRequired);
    context.setLimitedInParameterNames(Set.of("amount"));
    context.setNamedBinding(true);
    context.initializeMetaData(dataSource);
    context.processParameters(List.of(new SqlReturnResultSet("rows", (rs, row) -> row)));

    assertThat(context.getLimitedInParameterNames()).containsExactly("amount");
    assertThat(context.getCallParameters()).extracting(SqlParameter::getName).containsExactlyElementsOf(
            returnValueRequired ? List.of("rows", "RETURN_VALUE", "AMOUNT", "TOTAL", "STATUS") :
                    List.of("rows", "AMOUNT", "TOTAL", "STATUS"));
    assertThat(context.getOutParameterNames()).containsExactly("TOTAL", "STATUS");
    assertThat(context.createCallString()).isEqualTo((returnValueRequired ? "{? = call " : "{call ") +
            "adjust(AMOUNT = ?, TOTAL = ?, STATUS = ?)}");
    assertThat(context.matchInParameterValuesWithCallParameters(Map.of("@amount", 10, "status", 99)))
            .hasSize(1).containsKey("AMOUNT").doesNotContainKey("STATUS");
    var positional = context.matchInParameterValuesWithCallParameters(new Object[] { 10, 20 });
    assertThat(positional).containsOnlyKeys("AMOUNT", "TOTAL");
    assertThat(positional.get("AMOUNT")).isEqualTo(10);
    assertThat(positional.get("TOTAL")).isEqualTo(20);
    verify(procedures).close();
    verify(columns).close();
  }

  @Test
  void usesConfiguredReturnNameForUnnamedOracleFunctionReturn() throws SQLException {
    initializeGetTotalFunctionMetaData();
    context.setProcedureName("GET_TOTAL");
    context.setFunction(true);
    context.setFunctionReturnName("answer");
    context.initializeMetaData(dataSource);
    context.processParameters(List.of());

    assertThat(context.getScalarOutParameterName()).isEqualTo("answer");
    assertThat(context.getOutParameterNames()).containsExactly("answer", "OUT_STATUS");
    assertThat(context.getCallParameters()).extracting(SqlParameter::getName)
            .containsExactly("answer", "AMOUNT", "OUT_STATUS");
  }

  @Test
  void keepsExplicitTypeForNamedFunctionReturnReportedByMetadata() throws SQLException {
    initializeGetTotalFunctionMetaData("result");
    context.setProcedureName("GET_TOTAL");
    context.setFunction(true);
    context.initializeMetaData(dataSource);
    var declared = new SqlOutParameter("ReSuLt", Types.BIGINT);

    context.processParameters(List.of(declared));

    assertThat(context.getScalarOutParameterName()).isEqualTo("ReSuLt");
    assertThat(context.getCallParameters()).startsWith(declared);
    assertThat(context.createCallString()).isEqualTo("{? = call GET_TOTAL(?, ?)}");
  }

  @Test
  void qualifiesProcedureUsingCatalogThenSchemaWhenBothAreSupported() throws SQLException {
    given(databaseMetaData.supportsCatalogsInProcedureCalls()).willReturn(true);
    given(databaseMetaData.supportsSchemasInProcedureCalls()).willReturn(true);
    initializeWithoutColumnMetadata("OtherDB");
    context.setCatalogName("sales");
    context.setSchemaName("reports");
    context.setProcedureName("recent_orders");
    var rows = context.createReturnResultSetParameter("rows", (rs, row) -> row);
    context.processParameters(List.of(rows));

    assertThat(rows).isInstanceOf(SqlReturnResultSet.class);
    assertThat(context.getScalarOutParameterName()).isNull();
    assertThat(context.createCallString()).isEqualTo("{call sales.reports.recent_orders()}");
  }

  @Test
  void putsOracleSchemaBeforePackageAndCreatesRefCursorParameter() throws SQLException {
    given(databaseMetaData.supportsSchemasInProcedureCalls()).willReturn(true);
    given(databaseMetaData.storesUpperCaseIdentifiers()).willReturn(true);
    initializeWithoutColumnMetadata("Oracle");
    context.setSchemaName("owner");
    context.setCatalogName("pkg");
    context.setProcedureName("find_items");
    SqlParameter cursor = context.createReturnResultSetParameter("items", (rs, row) -> row);
    context.processParameters(List.of(new SqlParameter("id", Types.INTEGER), cursor));

    assertThat(cursor).isInstanceOfSatisfying(SqlOutParameter.class, out -> {
      assertThat(out.getSqlType()).isEqualTo(-10);
      assertThat(out.getRowMapper()).isNotNull();
    });
    assertThat(context.createCallString()).isEqualTo("{call OWNER.PKG.FIND_ITEMS(?, ?)}");
  }

  @Test
  void rejectsAnonymousParameters() throws SQLException {
    initializeWithoutColumnMetadata("OtherDB");
    assertThatIllegalArgumentException().isThrownBy(() -> context.processParameters(List.of(new SqlParameter(Types.INTEGER))))
            .withMessageContaining("Anonymous parameters not supported");
  }

  @Test
  void usesOtherTypeForPostgresRefCursor() throws SQLException {
    given(databaseMetaData.getConnection()).willReturn(connection);
    initializeWithoutColumnMetadata("PostgreSQL");
    assertThat(context.createReturnResultSetParameter("rows", (rs, row) -> row))
            .isInstanceOfSatisfying(SqlOutParameter.class, out -> assertThat(out.getSqlType()).isEqualTo(Types.OTHER));
  }

  private void initializeWithoutColumnMetadata(String product) throws SQLException {
    given(databaseMetaData.getDatabaseProductName()).willReturn(product);
    context.setAccessCallParameterMetaData(false);
    context.initializeMetaData(dataSource);
  }

  private void initializeGetTotalFunctionMetaData() throws SQLException {
    initializeGetTotalFunctionMetaData(null);
  }

  private void initializeGetTotalFunctionMetaData(@Nullable String returnName) throws SQLException {
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
    given(procedureColumnsResultSet.getString("COLUMN_NAME")).willReturn(returnName, "amount", "out_status");
    given(procedureColumnsResultSet.getInt("COLUMN_TYPE")).willReturn(5, 1, 4);
  }

}

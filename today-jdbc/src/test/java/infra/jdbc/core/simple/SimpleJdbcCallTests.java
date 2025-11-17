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

package infra.jdbc.core.simple;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;

import javax.sql.DataSource;

import infra.dao.InvalidDataAccessApiUsageException;
import infra.jdbc.BadSqlGrammarException;
import infra.jdbc.core.JdbcTemplate;
import infra.jdbc.core.RowMapper;
import infra.jdbc.core.SqlOutParameter;
import infra.jdbc.core.SqlParameter;
import infra.jdbc.core.namedparam.MapSqlParameterSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link SimpleJdbcCall}.
 *
 * @author Thomas Risberg
 * @author Kiril Nugmanov
 * @author Sam Brannen
 */
class SimpleJdbcCallTests {

  private final Connection connection = mock();

  private final DatabaseMetaData databaseMetaData = mock();

  private final DataSource dataSource = mock();

  private final CallableStatement callableStatement = mock();

  @BeforeEach
  void setUp() throws Exception {
    given(connection.getMetaData()).willReturn(databaseMetaData);
    given(dataSource.getConnection()).willReturn(connection);
  }

  @Test
  void noSuchStoredProcedure() throws Exception {
    final String NO_SUCH_PROC = "x";
    SQLException sqlException = new SQLException("Syntax error or access violation exception", "42000");
    given(databaseMetaData.getDatabaseProductName()).willReturn("MyDB");
    given(databaseMetaData.getDatabaseProductName()).willReturn("MyDB");
    given(databaseMetaData.getUserName()).willReturn("me");
    given(databaseMetaData.storesLowerCaseIdentifiers()).willReturn(true);
    given(callableStatement.execute()).willThrow(sqlException);
    given(connection.prepareCall("{call " + NO_SUCH_PROC + "()}")).willReturn(callableStatement);
    SimpleJdbcCall sproc = new SimpleJdbcCall(dataSource).withProcedureName(NO_SUCH_PROC);
    try {
      assertThatExceptionOfType(BadSqlGrammarException.class)
              .isThrownBy(sproc::execute)
              .withCause(sqlException);
    }
    finally {
      verify(callableStatement).close();
      verify(connection, atLeastOnce()).close();
    }
  }

  @Test
  void unnamedParameterHandling() {
    final String MY_PROC = "my_proc";
    SimpleJdbcCall sproc = new SimpleJdbcCall(dataSource).withProcedureName(MY_PROC);
    // Shouldn't succeed in adding unnamed parameter
    assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(() ->
            sproc.addDeclaredParameter(new SqlParameter(1)));
  }

  @Test
  void addInvoiceProcWithoutMetaDataUsingMapParamSource() throws Exception {
    initializeAddInvoiceWithoutMetaData(false);
    SimpleJdbcCall adder = new SimpleJdbcCall(dataSource).withProcedureName("add_invoice");
    adder.declareParameters(
            new SqlParameter("amount", Types.INTEGER),
            new SqlParameter("custid", Types.INTEGER),
            new SqlOutParameter("newid", Types.INTEGER));
    Number newId = adder.executeObject(Number.class, new MapSqlParameterSource().
            addValue("amount", 1103).
            addValue("custid", 3));
    assertThat(newId.intValue()).isEqualTo(4);
    verifyAddInvoiceWithoutMetaData(false);
    verify(connection, atLeastOnce()).close();
  }

  @Test
  void addInvoiceProcWithoutMetaDataUsingArrayParams() throws Exception {
    initializeAddInvoiceWithoutMetaData(false);
    SimpleJdbcCall adder = new SimpleJdbcCall(dataSource).withProcedureName("add_invoice");
    adder.declareParameters(
            new SqlParameter("amount", Types.INTEGER),
            new SqlParameter("custid", Types.INTEGER),
            new SqlOutParameter("newid", Types.INTEGER));
    Number newId = adder.executeObject(Number.class, 1103, 3);
    assertThat(newId.intValue()).isEqualTo(4);
    verifyAddInvoiceWithoutMetaData(false);
    verify(connection, atLeastOnce()).close();
  }

  @Test
  void addInvoiceProcWithMetaDataUsingMapParamSource() throws Exception {
    initializeAddInvoiceWithMetaData(false);
    SimpleJdbcCall adder = new SimpleJdbcCall(dataSource).withProcedureName("add_invoice");
    Number newId = adder.executeObject(Number.class, new MapSqlParameterSource()
            .addValue("amount", 1103)
            .addValue("custid", 3));
    assertThat(newId.intValue()).isEqualTo(4);
    verifyAddInvoiceWithMetaData(false);
    verify(connection, atLeastOnce()).close();
  }

  @Test
  void addInvoiceProcWithMetaDataUsingArrayParams() throws Exception {
    initializeAddInvoiceWithMetaData(false);
    SimpleJdbcCall adder = new SimpleJdbcCall(dataSource).withProcedureName("add_invoice");
    Number newId = adder.executeObject(Number.class, 1103, 3);
    assertThat(newId.intValue()).isEqualTo(4);
    verifyAddInvoiceWithMetaData(false);
    verify(connection, atLeastOnce()).close();
  }

  @Test
  void addInvoiceFuncWithoutMetaDataUsingMapParamSource() throws Exception {
    initializeAddInvoiceWithoutMetaData(true);
    SimpleJdbcCall adder = new SimpleJdbcCall(dataSource).withFunctionName("add_invoice");
    adder.declareParameters(
            new SqlOutParameter("return", Types.INTEGER),
            new SqlParameter("amount", Types.INTEGER),
            new SqlParameter("custid", Types.INTEGER));
    Number newId = adder.executeFunction(Number.class, new MapSqlParameterSource()
            .addValue("amount", 1103)
            .addValue("custid", 3));
    assertThat(newId.intValue()).isEqualTo(4);
    verifyAddInvoiceWithoutMetaData(true);
    verify(connection, atLeastOnce()).close();
  }

  @Test
  void addInvoiceFuncWithoutMetaDataUsingArrayParams() throws Exception {
    initializeAddInvoiceWithoutMetaData(true);
    SimpleJdbcCall adder = new SimpleJdbcCall(dataSource).withFunctionName("add_invoice");
    adder.declareParameters(
            new SqlOutParameter("return", Types.INTEGER),
            new SqlParameter("amount", Types.INTEGER),
            new SqlParameter("custid", Types.INTEGER));
    Number newId = adder.executeFunction(Number.class, 1103, 3);
    assertThat(newId.intValue()).isEqualTo(4);
    verifyAddInvoiceWithoutMetaData(true);
    verify(connection, atLeastOnce()).close();
  }

  @Test
  void addInvoiceFuncWithMetaDataUsingMapParamSource() throws Exception {
    initializeAddInvoiceWithMetaData(true);
    SimpleJdbcCall adder = new SimpleJdbcCall(dataSource).withFunctionName("add_invoice");
    Number newId = adder.executeFunction(Number.class, new MapSqlParameterSource()
            .addValue("amount", 1103)
            .addValue("custid", 3));
    assertThat(newId.intValue()).isEqualTo(4);
    verifyAddInvoiceWithMetaData(true);
    verify(connection, atLeastOnce()).close();
  }

  @Test
  void addInvoiceFuncWithMetaDataUsingArrayParams() throws Exception {
    initializeAddInvoiceWithMetaData(true);
    SimpleJdbcCall adder = new SimpleJdbcCall(dataSource).withFunctionName("add_invoice");
    Number newId = adder.executeFunction(Number.class, 1103, 3);
    assertThat(newId.intValue()).isEqualTo(4);
    verifyAddInvoiceWithMetaData(true);
    verify(connection, atLeastOnce()).close();
  }

  @Test
  void correctFunctionStatement() throws Exception {
    initializeAddInvoiceWithMetaData(true);
    SimpleJdbcCall adder = new SimpleJdbcCall(dataSource).withFunctionName("add_invoice");
    adder.compile();
    verifyStatement(adder, "{? = call ADD_INVOICE(?, ?)}");
  }

  @Test
  void correctFunctionStatementNamed() throws Exception {
    initializeAddInvoiceWithMetaData(true);
    SimpleJdbcCall adder = new SimpleJdbcCall(dataSource).withNamedBinding().withFunctionName("add_invoice");
    adder.compile();
    verifyStatement(adder, "{? = call ADD_INVOICE(AMOUNT => ?, CUSTID => ?)}");
  }

  @Test
  void correctProcedureStatementNamed() throws Exception {
    initializeAddInvoiceWithMetaData(false);
    SimpleJdbcCall adder = new SimpleJdbcCall(dataSource).withNamedBinding().withProcedureName("add_invoice");
    adder.compile();
    verifyStatement(adder, "{call ADD_INVOICE(AMOUNT => ?, CUSTID => ?, NEWID => ?)}");
  }

  /**
   * This test demonstrates that a CALL statement will still be generated if
   * an exception occurs while retrieving metadata, potentially resulting in
   * missing metadata and consequently a failure while invoking the stored
   * procedure.
   */
  @Test
  // gh-26486
  void exceptionThrownWhileRetrievingColumnNamesFromMetadata() throws Exception {
    ResultSet proceduresResultSet = mock();
    ResultSet procedureColumnsResultSet = mock();

    given(databaseMetaData.getDatabaseProductName()).willReturn("Oracle");
    given(databaseMetaData.getUserName()).willReturn("ME");
    given(databaseMetaData.storesUpperCaseIdentifiers()).willReturn(true);
    given(databaseMetaData.getProcedures("", "ME", "ADD_INVOICE")).willReturn(proceduresResultSet);
    given(databaseMetaData.getProcedureColumns("", "ME", "ADD_INVOICE", null)).willReturn(procedureColumnsResultSet);

    given(proceduresResultSet.next()).willReturn(true, false);
    given(proceduresResultSet.getString("PROCEDURE_NAME")).willReturn("add_invoice");

    given(procedureColumnsResultSet.next()).willReturn(true, true, true, false);
    given(procedureColumnsResultSet.getString("COLUMN_NAME")).willReturn("amount", "custid", "newid");
    given(procedureColumnsResultSet.getInt("DATA_TYPE"))
            // Return a valid data type for the first 2 columns.
            .willReturn(Types.INTEGER, Types.INTEGER)
            // 3rd time, simulate an error while retrieving metadata.
            .willThrow(new SQLException("error with DATA_TYPE for column 3"));

    SimpleJdbcCall adder = new SimpleJdbcCall(dataSource).withNamedBinding().withProcedureName("add_invoice");
    adder.compile();
    // If an exception were not thrown for column 3, we would expect:
    // {call ADD_INVOICE(AMOUNT => ?, CUSTID => ?, NEWID => ?)}
    verifyStatement(adder, "{call ADD_INVOICE(AMOUNT => ?, CUSTID => ?)}");

    verify(proceduresResultSet).close();
    verify(procedureColumnsResultSet).close();
  }

  private void verifyStatement(SimpleJdbcCall adder, String expected) {
    assertThat(adder.getCallString()).as("Incorrect call statement").isEqualTo(expected);
  }

  private void initializeAddInvoiceWithoutMetaData(boolean isFunction) throws SQLException {
    given(databaseMetaData.getDatabaseProductName()).willReturn("MyDB");
    given(databaseMetaData.getUserName()).willReturn("me");
    given(databaseMetaData.storesLowerCaseIdentifiers()).willReturn(true);
    given(callableStatement.execute()).willReturn(false);
    given(callableStatement.getUpdateCount()).willReturn(-1);
    if (isFunction) {
      given(callableStatement.getObject(1)).willReturn(4L);
      given(connection.prepareCall("{? = call add_invoice(?, ?)}")
      ).willReturn(callableStatement);
    }
    else {
      given(callableStatement.getObject(3)).willReturn(4L);
      given(connection.prepareCall("{call add_invoice(?, ?, ?)}")
      ).willReturn(callableStatement);
    }
  }

  private void verifyAddInvoiceWithoutMetaData(boolean isFunction) throws SQLException {
    if (isFunction) {
      verify(callableStatement).registerOutParameter(1, 4);
      verify(callableStatement).setObject(2, 1103, 4);
      verify(callableStatement).setObject(3, 3, 4);
    }
    else {
      verify(callableStatement).setObject(1, 1103, 4);
      verify(callableStatement).setObject(2, 3, 4);
      verify(callableStatement).registerOutParameter(3, 4);
    }
    verify(callableStatement).close();
  }

  private void initializeAddInvoiceWithMetaData(boolean isFunction) throws SQLException {
    ResultSet proceduresResultSet = mock();
    ResultSet procedureColumnsResultSet = mock();
    given(databaseMetaData.getDatabaseProductName()).willReturn("Oracle");
    given(databaseMetaData.getUserName()).willReturn("ME");
    given(databaseMetaData.storesUpperCaseIdentifiers()).willReturn(true);
    given(databaseMetaData.getProcedures("", "ME", "ADD_INVOICE")).willReturn(proceduresResultSet);
    given(databaseMetaData.getProcedureColumns("", "ME", "ADD_INVOICE", null)).willReturn(procedureColumnsResultSet);

    given(proceduresResultSet.next()).willReturn(true, false);
    given(proceduresResultSet.getString("PROCEDURE_NAME")).willReturn("add_invoice");

    given(procedureColumnsResultSet.next()).willReturn(true, true, true, false);
    given(procedureColumnsResultSet.getInt("DATA_TYPE")).willReturn(4);
    if (isFunction) {
      given(procedureColumnsResultSet.getString("COLUMN_NAME")).willReturn(null, "amount", "custid");
      given(procedureColumnsResultSet.getInt("COLUMN_TYPE")).willReturn(5, 1, 1);
      given(connection.prepareCall("{? = call ADD_INVOICE(?, ?)}")).willReturn(callableStatement);
      given(callableStatement.getObject(1)).willReturn(4L);
    }
    else {
      given(procedureColumnsResultSet.getString("COLUMN_NAME")).willReturn("amount", "custid", "newid");
      given(procedureColumnsResultSet.getInt("COLUMN_TYPE")).willReturn(1, 1, 4);
      given(connection.prepareCall("{call ADD_INVOICE(?, ?, ?)}")).willReturn(callableStatement);
      given(callableStatement.getObject(3)).willReturn(4L);
    }
    given(callableStatement.getUpdateCount()).willReturn(-1);
  }

  private void verifyAddInvoiceWithMetaData(boolean isFunction) throws SQLException {
    ResultSet proceduresResultSet = databaseMetaData.getProcedures("", "ME", "ADD_INVOICE");
    ResultSet procedureColumnsResultSet = databaseMetaData.getProcedureColumns("", "ME", "ADD_INVOICE", null);
    if (isFunction) {
      verify(callableStatement).registerOutParameter(1, 4);
      verify(callableStatement).setObject(2, 1103, 4);
      verify(callableStatement).setObject(3, 3, 4);
    }
    else {
      verify(callableStatement).setObject(1, 1103, 4);
      verify(callableStatement).setObject(2, 3, 4);
      verify(callableStatement).registerOutParameter(3, 4);
    }
    verify(callableStatement).close();
    verify(proceduresResultSet).close();
    verify(procedureColumnsResultSet).close();
  }

  @Test
  void correctSybaseFunctionStatementNamed() throws Exception {
    given(databaseMetaData.getDatabaseProductName()).willReturn("Sybase");
    SimpleJdbcCall adder = new SimpleJdbcCall(dataSource)
            .withoutProcedureColumnMetaDataAccess()
            .withNamedBinding()
            .withProcedureName("ADD_INVOICE")
            .declareParameters(new SqlParameter("@AMOUNT", Types.NUMERIC))
            .declareParameters(new SqlParameter("@CUSTID", Types.NUMERIC));
    adder.compile();
    verifyStatement(adder, "{call ADD_INVOICE(@AMOUNT = ?, @CUSTID = ?)}");
  }

  @Test
  void declareParametersCannotBeInvokedWhenCompiled() {
    SimpleJdbcCall call = new SimpleJdbcCall(dataSource)
            .withProcedureName("procedure_name")
            .declareParameters(new SqlParameter("PARAM", Types.VARCHAR));
    call.compile();
    assertThatIllegalStateException()
            .isThrownBy(() -> call.declareParameters(new SqlParameter("Ignored Param", Types.VARCHAR)))
            .withMessage("SqlCall for procedure is already compiled");
  }

  @Test
  void addDeclaredRowMapperCanBeConfigured() {
    SimpleJdbcCall call = new SimpleJdbcCall(dataSource)
            .withProcedureName("procedure_name")
            .returningResultSet("result_set", (rs, i) -> new Object());

    assertThat(call).extracting("declaredRowMappers")
            .asInstanceOf(InstanceOfAssertFactories.map(String.class, RowMapper.class))
            .containsOnlyKeys("result_set");
  }

  @Test
  void addDeclaredRowMapperWhenCompiled() {
    SimpleJdbcCall call = new SimpleJdbcCall(dataSource)
            .withProcedureName("procedure_name")
            .returningResultSet("result_set", (rs, i) -> new Object());
    call.compile();
    assertThatIllegalStateException()
            .isThrownBy(() -> call.returningResultSet("not added", (rs, i) -> new Object()))
            .withMessage("SqlCall for procedure is already compiled");
  }

  @Test
  void shouldCreateSimpleJdbcCallWithDataSource() {
    DataSource dataSource = mock(DataSource.class);

    SimpleJdbcCall call = new SimpleJdbcCall(dataSource);

    assertThat(call).isNotNull();
  }

  @Test
  void shouldCreateSimpleJdbcCallWithJdbcTemplate() {
    JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

    SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate);

    assertThat(call).isNotNull();
  }

  @Test
  void shouldSetProcedureName() {
    DataSource dataSource = mock(DataSource.class);
    SimpleJdbcCall call = new SimpleJdbcCall(dataSource);

    SimpleJdbcCall result = call.withProcedureName("test_proc");

    assertThat(result).isEqualTo(call);
  }

  @Test
  void shouldSetFunctionName() {
    DataSource dataSource = mock(DataSource.class);
    SimpleJdbcCall call = new SimpleJdbcCall(dataSource);

    SimpleJdbcCall result = call.withFunctionName("test_func");

    assertThat(result).isEqualTo(call);
  }

  @Test
  void shouldSetSchemaName() {
    DataSource dataSource = mock(DataSource.class);
    SimpleJdbcCall call = new SimpleJdbcCall(dataSource);

    SimpleJdbcCall result = call.withSchemaName("test_schema");

    assertThat(result).isEqualTo(call);
  }

  @Test
  void shouldSetCatalogName() {
    DataSource dataSource = mock(DataSource.class);
    SimpleJdbcCall call = new SimpleJdbcCall(dataSource);

    SimpleJdbcCall result = call.withCatalogName("test_catalog");

    assertThat(result).isEqualTo(call);
  }

  @Test
  void shouldSetReturnValue() {
    DataSource dataSource = mock(DataSource.class);
    SimpleJdbcCall call = new SimpleJdbcCall(dataSource);

    SimpleJdbcCall result = call.withReturnValue();

    assertThat(result).isEqualTo(call);
  }

  @Test
  void shouldDeclareParameters() {
    DataSource dataSource = mock(DataSource.class);
    SimpleJdbcCall call = new SimpleJdbcCall(dataSource);

    SqlParameter param1 = new SqlParameter("param1", java.sql.Types.VARCHAR);
    SqlParameter param2 = new SqlParameter("param2", java.sql.Types.INTEGER);

    SimpleJdbcCall result = call.declareParameters(param1, param2);

    assertThat(result).isEqualTo(call);
  }

  @Test
  void shouldHandleNullParametersInDeclareParameters() {
    DataSource dataSource = mock(DataSource.class);
    SimpleJdbcCall call = new SimpleJdbcCall(dataSource);

    SqlParameter param1 = new SqlParameter("param1", java.sql.Types.VARCHAR);
    SqlParameter param2 = null;

    SimpleJdbcCall result = call.declareParameters(param1, param2);

    assertThat(result).isEqualTo(call);
  }

  @Test
  void shouldUseInParameterNames() {
    DataSource dataSource = mock(DataSource.class);
    SimpleJdbcCall call = new SimpleJdbcCall(dataSource);

    SimpleJdbcCall result = call.useInParameterNames("param1", "param2");

    assertThat(result).isEqualTo(call);
  }

  @Test
  void shouldSetWithoutProcedureColumnMetaDataAccess() {
    DataSource dataSource = mock(DataSource.class);
    SimpleJdbcCall call = new SimpleJdbcCall(dataSource);

    SimpleJdbcCall result = call.withoutProcedureColumnMetaDataAccess();

    assertThat(result).isEqualTo(call);
  }

  @Test
  void shouldSetWithNamedBinding() {
    DataSource dataSource = mock(DataSource.class);
    SimpleJdbcCall call = new SimpleJdbcCall(dataSource);

    SimpleJdbcCall result = call.withNamedBinding();

    assertThat(result).isEqualTo(call);
  }

  @Test
  void shouldReturnSameInstanceForFluentInterface() {
    DataSource dataSource = mock(DataSource.class);
    SimpleJdbcCall call = new SimpleJdbcCall(dataSource);

    SimpleJdbcCall result1 = call.withProcedureName("test_proc");
    SimpleJdbcCall result2 = call.withSchemaName("test_schema");
    SimpleJdbcCall result3 = call.withCatalogName("test_catalog");

    assertThat(result1).isSameAs(call);
    assertThat(result2).isSameAs(call);
    assertThat(result3).isSameAs(call);
  }

  @Test
  void shouldExecuteFunctionWithVarArgs() throws Exception {
    DataSource dataSource = mock(DataSource.class);
    Connection connection = mock(Connection.class);
    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    CallableStatement callableStatement = mock(CallableStatement.class);

    given(dataSource.getConnection()).willReturn(connection);
    given(connection.getMetaData()).willReturn(databaseMetaData);
    given(databaseMetaData.getDatabaseProductName()).willReturn("MyDB");
    given(databaseMetaData.getUserName()).willReturn("me");
    given(databaseMetaData.storesLowerCaseIdentifiers()).willReturn(true);
    given(callableStatement.execute()).willReturn(false);
    given(callableStatement.getUpdateCount()).willReturn(-1);
    given(callableStatement.getObject(1)).willReturn(4L);
    given(connection.prepareCall("{? = call test_func(?, ?)}")).willReturn(callableStatement);

    SimpleJdbcCall call = new SimpleJdbcCall(dataSource)
            .withFunctionName("test_func")
            .declareParameters(
                    new SqlOutParameter("return", Types.INTEGER),
                    new SqlParameter("param1", Types.VARCHAR),
                    new SqlParameter("param2", Types.INTEGER));

    Number result = call.executeFunction(Number.class, "value1", 123);

    assertThat(result.intValue()).isEqualTo(4);
    verify(callableStatement).close();
    verify(connection, atLeastOnce()).close();
  }

  @Test
  void shouldExecuteFunctionWithMap() throws Exception {
    DataSource dataSource = mock(DataSource.class);
    Connection connection = mock(Connection.class);
    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    CallableStatement callableStatement = mock(CallableStatement.class);

    given(dataSource.getConnection()).willReturn(connection);
    given(connection.getMetaData()).willReturn(databaseMetaData);
    given(databaseMetaData.getDatabaseProductName()).willReturn("MyDB");
    given(databaseMetaData.getUserName()).willReturn("me");
    given(databaseMetaData.storesLowerCaseIdentifiers()).willReturn(true);
    given(callableStatement.execute()).willReturn(false);
    given(callableStatement.getUpdateCount()).willReturn(-1);
    given(callableStatement.getObject(1)).willReturn(4L);
    given(connection.prepareCall("{? = call test_func(?, ?)}")).willReturn(callableStatement);

    SimpleJdbcCall call = new SimpleJdbcCall(dataSource)
            .withFunctionName("test_func")
            .declareParameters(
                    new SqlOutParameter("return", Types.INTEGER),
                    new SqlParameter("param1", Types.VARCHAR),
                    new SqlParameter("param2", Types.INTEGER));

    Number result = call.executeFunction(Number.class, Map.of("param1", "value1", "param2", 123));

    assertThat(result.intValue()).isEqualTo(4);
    verify(callableStatement).close();
    verify(connection, atLeastOnce()).close();
  }

  @Test
  void shouldExecuteFunctionWithSqlParameterSource() throws Exception {
    DataSource dataSource = mock(DataSource.class);
    Connection connection = mock(Connection.class);
    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    CallableStatement callableStatement = mock(CallableStatement.class);

    given(dataSource.getConnection()).willReturn(connection);
    given(connection.getMetaData()).willReturn(databaseMetaData);
    given(databaseMetaData.getDatabaseProductName()).willReturn("MyDB");
    given(databaseMetaData.getUserName()).willReturn("me");
    given(databaseMetaData.storesLowerCaseIdentifiers()).willReturn(true);
    given(callableStatement.execute()).willReturn(false);
    given(callableStatement.getUpdateCount()).willReturn(-1);
    given(callableStatement.getObject(1)).willReturn(4L);
    given(connection.prepareCall("{? = call test_func(?, ?)}")).willReturn(callableStatement);

    SimpleJdbcCall call = new SimpleJdbcCall(dataSource)
            .withFunctionName("test_func")
            .declareParameters(
                    new SqlOutParameter("return", Types.INTEGER),
                    new SqlParameter("param1", Types.VARCHAR),
                    new SqlParameter("param2", Types.INTEGER));

    Number result = call.executeFunction(Number.class, new MapSqlParameterSource()
            .addValue("param1", "value1")
            .addValue("param2", 123));

    assertThat(result.intValue()).isEqualTo(4);
    verify(callableStatement).close();
    verify(connection, atLeastOnce()).close();
  }

  @Test
  void shouldExecuteObjectWithVarArgs() throws Exception {
    DataSource dataSource = mock(DataSource.class);
    Connection connection = mock(Connection.class);
    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    CallableStatement callableStatement = mock(CallableStatement.class);

    given(dataSource.getConnection()).willReturn(connection);
    given(connection.getMetaData()).willReturn(databaseMetaData);
    given(databaseMetaData.getDatabaseProductName()).willReturn("MyDB");
    given(databaseMetaData.getUserName()).willReturn("me");
    given(databaseMetaData.storesLowerCaseIdentifiers()).willReturn(true);
    given(callableStatement.execute()).willReturn(false);
    given(callableStatement.getUpdateCount()).willReturn(-1);
    given(callableStatement.getObject(3)).willReturn(4L);
    given(connection.prepareCall("{call test_proc(?, ?, ?)}")).willReturn(callableStatement);

    SimpleJdbcCall call = new SimpleJdbcCall(dataSource)
            .withProcedureName("test_proc")
            .declareParameters(
                    new SqlParameter("param1", Types.VARCHAR),
                    new SqlParameter("param2", Types.INTEGER),
                    new SqlOutParameter("result", Types.INTEGER));

    Number result = call.executeObject(Number.class, "value1", 123);

    assertThat(result.intValue()).isEqualTo(4);
    verify(callableStatement).close();
    verify(connection, atLeastOnce()).close();
  }

  @Test
  void shouldExecuteObjectWithMap() throws Exception {
    DataSource dataSource = mock(DataSource.class);
    Connection connection = mock(Connection.class);
    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    CallableStatement callableStatement = mock(CallableStatement.class);

    given(dataSource.getConnection()).willReturn(connection);
    given(connection.getMetaData()).willReturn(databaseMetaData);
    given(databaseMetaData.getDatabaseProductName()).willReturn("MyDB");
    given(databaseMetaData.getUserName()).willReturn("me");
    given(databaseMetaData.storesLowerCaseIdentifiers()).willReturn(true);
    given(callableStatement.execute()).willReturn(false);
    given(callableStatement.getUpdateCount()).willReturn(-1);
    given(callableStatement.getObject(3)).willReturn(4L);
    given(connection.prepareCall("{call test_proc(?, ?, ?)}")).willReturn(callableStatement);

    SimpleJdbcCall call = new SimpleJdbcCall(dataSource)
            .withProcedureName("test_proc")
            .declareParameters(
                    new SqlParameter("param1", Types.VARCHAR),
                    new SqlParameter("param2", Types.INTEGER),
                    new SqlOutParameter("result", Types.INTEGER));

    Number result = call.executeObject(Number.class, Map.of("param1", "value1", "param2", 123));

    assertThat(result.intValue()).isEqualTo(4);
    verify(callableStatement).close();
    verify(connection, atLeastOnce()).close();
  }

  @Test
  void shouldExecuteObjectWithSqlParameterSource() throws Exception {
    DataSource dataSource = mock(DataSource.class);
    Connection connection = mock(Connection.class);
    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    CallableStatement callableStatement = mock(CallableStatement.class);

    given(dataSource.getConnection()).willReturn(connection);
    given(connection.getMetaData()).willReturn(databaseMetaData);
    given(databaseMetaData.getDatabaseProductName()).willReturn("MyDB");
    given(databaseMetaData.getUserName()).willReturn("me");
    given(databaseMetaData.storesLowerCaseIdentifiers()).willReturn(true);
    given(callableStatement.execute()).willReturn(false);
    given(callableStatement.getUpdateCount()).willReturn(-1);
    given(callableStatement.getObject(3)).willReturn(4L);
    given(connection.prepareCall("{call test_proc(?, ?, ?)}")).willReturn(callableStatement);

    SimpleJdbcCall call = new SimpleJdbcCall(dataSource)
            .withProcedureName("test_proc")
            .declareParameters(
                    new SqlParameter("param1", Types.VARCHAR),
                    new SqlParameter("param2", Types.INTEGER),
                    new SqlOutParameter("result", Types.INTEGER));

    Number result = call.executeObject(Number.class, new MapSqlParameterSource()
            .addValue("param1", "value1")
            .addValue("param2", 123));

    assertThat(result.intValue()).isEqualTo(4);
    verify(callableStatement).close();
    verify(connection, atLeastOnce()).close();
  }

  @Test
  void shouldExecuteWithVarArgs() throws Exception {
    DataSource dataSource = mock(DataSource.class);
    Connection connection = mock(Connection.class);
    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    CallableStatement callableStatement = mock(CallableStatement.class);

    given(dataSource.getConnection()).willReturn(connection);
    given(connection.getMetaData()).willReturn(databaseMetaData);
    given(databaseMetaData.getDatabaseProductName()).willReturn("MyDB");
    given(databaseMetaData.getUserName()).willReturn("me");
    given(databaseMetaData.storesLowerCaseIdentifiers()).willReturn(true);
    given(callableStatement.execute()).willReturn(false);
    given(callableStatement.getUpdateCount()).willReturn(-1);
    given(callableStatement.getObject(3)).willReturn(4L);
    given(connection.prepareCall("{call test_proc(?, ?, ?)}")).willReturn(callableStatement);

    SimpleJdbcCall call = new SimpleJdbcCall(dataSource)
            .withProcedureName("test_proc")
            .declareParameters(
                    new SqlParameter("param1", Types.VARCHAR),
                    new SqlParameter("param2", Types.INTEGER),
                    new SqlOutParameter("result", Types.INTEGER));

    Map<String, Object> result = call.execute("value1", 123);

    assertThat(result).isNotNull();
    verify(callableStatement).close();
    verify(connection, atLeastOnce()).close();
  }

  @Test
  void shouldExecuteWithMap() throws Exception {
    DataSource dataSource = mock(DataSource.class);
    Connection connection = mock(Connection.class);
    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    CallableStatement callableStatement = mock(CallableStatement.class);

    given(dataSource.getConnection()).willReturn(connection);
    given(connection.getMetaData()).willReturn(databaseMetaData);
    given(databaseMetaData.getDatabaseProductName()).willReturn("MyDB");
    given(databaseMetaData.getUserName()).willReturn("me");
    given(databaseMetaData.storesLowerCaseIdentifiers()).willReturn(true);
    given(callableStatement.execute()).willReturn(false);
    given(callableStatement.getUpdateCount()).willReturn(-1);
    given(callableStatement.getObject(3)).willReturn(4L);
    given(connection.prepareCall("{call test_proc(?, ?, ?)}")).willReturn(callableStatement);

    SimpleJdbcCall call = new SimpleJdbcCall(dataSource)
            .withProcedureName("test_proc")
            .declareParameters(
                    new SqlParameter("param1", Types.VARCHAR),
                    new SqlParameter("param2", Types.INTEGER),
                    new SqlOutParameter("result", Types.INTEGER));

    Map<String, Object> result = call.execute(Map.of("param1", "value1", "param2", 123));

    assertThat(result).isNotNull();
    verify(callableStatement).close();
    verify(connection, atLeastOnce()).close();
  }

  @Test
  void shouldExecuteWithSqlParameterSource() throws Exception {
    DataSource dataSource = mock(DataSource.class);
    Connection connection = mock(Connection.class);
    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    CallableStatement callableStatement = mock(CallableStatement.class);

    given(dataSource.getConnection()).willReturn(connection);
    given(connection.getMetaData()).willReturn(databaseMetaData);
    given(databaseMetaData.getDatabaseProductName()).willReturn("MyDB");
    given(databaseMetaData.getUserName()).willReturn("me");
    given(databaseMetaData.storesLowerCaseIdentifiers()).willReturn(true);
    given(callableStatement.execute()).willReturn(false);
    given(callableStatement.getUpdateCount()).willReturn(-1);
    given(callableStatement.getObject(3)).willReturn(4L);
    given(connection.prepareCall("{call test_proc(?, ?, ?)}")).willReturn(callableStatement);

    SimpleJdbcCall call = new SimpleJdbcCall(dataSource)
            .withProcedureName("test_proc")
            .declareParameters(
                    new SqlParameter("param1", Types.VARCHAR),
                    new SqlParameter("param2", Types.INTEGER),
                    new SqlOutParameter("result", Types.INTEGER));

    Map<String, Object> result = call.execute(new MapSqlParameterSource()
            .addValue("param1", "value1")
            .addValue("param2", 123));

    assertThat(result).isNotNull();
    verify(callableStatement).close();
    verify(connection, atLeastOnce()).close();
  }

  @Test
  void shouldAddDeclaredRowMapper() {
    DataSource dataSource = mock(DataSource.class);
    SimpleJdbcCall call = new SimpleJdbcCall(dataSource);
    RowMapper<Object> rowMapper = mock(RowMapper.class);

    SimpleJdbcCall result = call.returningResultSet("test_result_set", rowMapper);

    assertThat(result).isSameAs(call);
  }

  @Test
  void shouldExecuteAndReturnResultSet() throws Exception {
    DataSource dataSource = mock(DataSource.class);
    Connection connection = mock(Connection.class);
    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    CallableStatement callableStatement = mock(CallableStatement.class);
    ResultSet resultSet = mock(ResultSet.class);

    given(dataSource.getConnection()).willReturn(connection);
    given(connection.getMetaData()).willReturn(databaseMetaData);
    given(databaseMetaData.getDatabaseProductName()).willReturn("MyDB");
    given(databaseMetaData.getUserName()).willReturn("me");
    given(databaseMetaData.storesLowerCaseIdentifiers()).willReturn(true);
    given(callableStatement.execute()).willReturn(true);
    given(callableStatement.getResultSet()).willReturn(resultSet);
    given(callableStatement.getUpdateCount()).willReturn(-1);
    given(connection.prepareCall("{call test_proc()}")).willReturn(callableStatement);

    RowMapper<Object> rowMapper = mock(RowMapper.class);
    SimpleJdbcCall call = new SimpleJdbcCall(dataSource)
            .withProcedureName("test_proc")
            .returningResultSet("result_set", rowMapper);

    Map<String, Object> result = call.execute();

    assertThat(result).isNotNull();
    verify(callableStatement).close();
    verify(connection, atLeastOnce()).close();
  }

  @Test
  void shouldHandleMultipleResultSets() throws Exception {
    DataSource dataSource = mock(DataSource.class);
    Connection connection = mock(Connection.class);
    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    CallableStatement callableStatement = mock(CallableStatement.class);
    ResultSet resultSet1 = mock(ResultSet.class);
    ResultSet resultSet2 = mock(ResultSet.class);

    given(dataSource.getConnection()).willReturn(connection);
    given(connection.getMetaData()).willReturn(databaseMetaData);
    given(databaseMetaData.getDatabaseProductName()).willReturn("MyDB");
    given(databaseMetaData.getUserName()).willReturn("me");
    given(databaseMetaData.storesLowerCaseIdentifiers()).willReturn(true);
    given(callableStatement.execute()).willReturn(true);
    given(callableStatement.getResultSet()).willReturn(resultSet1);
    given(callableStatement.getMoreResults()).willReturn(true, false);
    given(callableStatement.getResultSet()).willReturn(resultSet2, (ResultSet) null);
    given(callableStatement.getUpdateCount()).willReturn(-1);
    given(connection.prepareCall("{call test_proc()}")).willReturn(callableStatement);

    RowMapper<Object> rowMapper1 = mock(RowMapper.class);
    RowMapper<Object> rowMapper2 = mock(RowMapper.class);
    SimpleJdbcCall call = new SimpleJdbcCall(dataSource)
            .withProcedureName("test_proc")
            .returningResultSet("result_set_1", rowMapper1)
            .returningResultSet("result_set_2", rowMapper2);

    Map<String, Object> result = call.execute();

    assertThat(result).isNotNull();
    verify(callableStatement).close();
    verify(connection, atLeastOnce()).close();
  }

  @Test
  void shouldHandleUpdateCountResults() throws Exception {
    DataSource dataSource = mock(DataSource.class);
    Connection connection = mock(Connection.class);
    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    CallableStatement callableStatement = mock(CallableStatement.class);

    given(dataSource.getConnection()).willReturn(connection);
    given(connection.getMetaData()).willReturn(databaseMetaData);
    given(databaseMetaData.getDatabaseProductName()).willReturn("MyDB");
    given(databaseMetaData.getUserName()).willReturn("me");
    given(databaseMetaData.storesLowerCaseIdentifiers()).willReturn(true);
    given(callableStatement.execute()).willReturn(false);
    given(callableStatement.getUpdateCount()).willReturn(5, -1);
    given(connection.prepareCall("{call test_proc()}")).willReturn(callableStatement);

    SimpleJdbcCall call = new SimpleJdbcCall(dataSource)
            .withProcedureName("test_proc");

    Map<String, Object> result = call.execute();

    assertThat(result).isNotNull();
    verify(callableStatement).close();
    verify(connection, atLeastOnce()).close();
  }

  @Test
  void shouldHandleSchemaAndCatalogWithProcedure() throws Exception {
    DataSource dataSource = mock(DataSource.class);
    Connection connection = mock(Connection.class);
    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    CallableStatement callableStatement = mock(CallableStatement.class);

    given(dataSource.getConnection()).willReturn(connection);
    given(connection.getMetaData()).willReturn(databaseMetaData);
    given(databaseMetaData.getDatabaseProductName()).willReturn("MyDB");
    given(databaseMetaData.getUserName()).willReturn("me");
    given(databaseMetaData.storesLowerCaseIdentifiers()).willReturn(true);
    given(callableStatement.execute()).willReturn(false);
    given(callableStatement.getUpdateCount()).willReturn(-1);
    given(callableStatement.getObject(3)).willReturn(4L);
    given(connection.prepareCall("{call my_catalog.my_schema.test_proc(?, ?, ?)}")).willReturn(callableStatement);

    SimpleJdbcCall call = new SimpleJdbcCall(dataSource)
            .withCatalogName("my_catalog")
            .withSchemaName("my_schema")
            .withProcedureName("test_proc")
            .declareParameters(
                    new SqlParameter("param1", Types.VARCHAR),
                    new SqlParameter("param2", Types.INTEGER),
                    new SqlOutParameter("result", Types.INTEGER));

    Number result = call.executeObject(Number.class, Map.of("param1", "value1", "param2", 123));

    assertThat(result.intValue()).isEqualTo(4);
    verify(callableStatement).close();
    verify(connection, atLeastOnce()).close();
  }

  @Test
  void shouldHandleReturnValueWithFunction() throws Exception {
    DataSource dataSource = mock(DataSource.class);
    Connection connection = mock(Connection.class);
    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    CallableStatement callableStatement = mock(CallableStatement.class);

    given(dataSource.getConnection()).willReturn(connection);
    given(connection.getMetaData()).willReturn(databaseMetaData);
    given(databaseMetaData.getDatabaseProductName()).willReturn("MyDB");
    given(databaseMetaData.getUserName()).willReturn("me");
    given(databaseMetaData.storesLowerCaseIdentifiers()).willReturn(true);
    given(callableStatement.execute()).willReturn(false);
    given(callableStatement.getUpdateCount()).willReturn(-1);
    given(callableStatement.getObject(1)).willReturn(4L);
    given(connection.prepareCall("{? = call test_func(?, ?)}")).willReturn(callableStatement);

    SimpleJdbcCall call = new SimpleJdbcCall(dataSource)
            .withReturnValue()
            .withFunctionName("test_func")
            .declareParameters(
                    new SqlOutParameter("return", Types.INTEGER),
                    new SqlParameter("param1", Types.VARCHAR),
                    new SqlParameter("param2", Types.INTEGER));

    Number result = call.executeFunction(Number.class, "value1", 123);

    assertThat(result.intValue()).isEqualTo(4);
    verify(callableStatement).close();
    verify(connection, atLeastOnce()).close();
  }

  @Test
  void shouldUseInParameterNamesFilter() throws Exception {
    DataSource dataSource = mock(DataSource.class);
    Connection connection = mock(Connection.class);
    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    CallableStatement callableStatement = mock(CallableStatement.class);

    given(dataSource.getConnection()).willReturn(connection);
    given(connection.getMetaData()).willReturn(databaseMetaData);
    given(databaseMetaData.getDatabaseProductName()).willReturn("MyDB");
    given(databaseMetaData.getUserName()).willReturn("me");
    given(databaseMetaData.storesLowerCaseIdentifiers()).willReturn(true);
    given(callableStatement.execute()).willReturn(false);
    given(callableStatement.getUpdateCount()).willReturn(-1);
    given(callableStatement.getObject(3)).willReturn(4L);
    given(connection.prepareCall("{call test_proc(?, ?, ?)}")).willReturn(callableStatement);

    SimpleJdbcCall call = new SimpleJdbcCall(dataSource)
            .withProcedureName("test_proc")
            .useInParameterNames("param1", "param3") // Only use these parameters
            .declareParameters(
                    new SqlParameter("param1", Types.VARCHAR),
                    new SqlParameter("param2", Types.INTEGER), // This should be ignored
                    new SqlOutParameter("result", Types.INTEGER));

    Map<String, Object> result = call.execute(Map.of("param1", "value1", "param2", 123, "param3", "value3"));

    assertThat(result).isNotNull();
    verify(callableStatement).close();
    verify(connection, atLeastOnce()).close();
  }

  @Test
  void shouldHandleWithoutProcedureColumnMetaDataAccess() throws Exception {
    DataSource dataSource = mock(DataSource.class);
    Connection connection = mock(Connection.class);
    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    CallableStatement callableStatement = mock(CallableStatement.class);

    given(dataSource.getConnection()).willReturn(connection);
    given(connection.getMetaData()).willReturn(databaseMetaData);
    given(databaseMetaData.getDatabaseProductName()).willReturn("MyDB");
    given(databaseMetaData.getUserName()).willReturn("me");
    given(databaseMetaData.storesLowerCaseIdentifiers()).willReturn(true);
    given(callableStatement.execute()).willReturn(false);
    given(callableStatement.getUpdateCount()).willReturn(-1);
    given(callableStatement.getObject(3)).willReturn(4L);
    given(connection.prepareCall("{call test_proc(?, ?, ?)}")).willReturn(callableStatement);

    SimpleJdbcCall call = new SimpleJdbcCall(dataSource)
            .withoutProcedureColumnMetaDataAccess()
            .withProcedureName("test_proc")
            .declareParameters(
                    new SqlParameter("param1", Types.VARCHAR),
                    new SqlParameter("param2", Types.INTEGER),
                    new SqlOutParameter("result", Types.INTEGER));

    Number result = call.executeObject(Number.class, "value1", 123);

    assertThat(result.intValue()).isEqualTo(4);
    verify(callableStatement).close();
    verify(connection, atLeastOnce()).close();
  }

}

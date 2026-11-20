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

package infra.jdbc.object;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import infra.dao.InvalidDataAccessApiUsageException;
import infra.jdbc.core.JdbcTemplate;
import infra.jdbc.core.SqlInOutParameter;
import infra.jdbc.core.SqlOutParameter;
import infra.jdbc.core.SqlParameter;
import infra.jdbc.datasource.DriverManagerDataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Trevor Cook
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
public class RdbmsOperationTests {

  private final TestRdbmsOperation operation = new TestRdbmsOperation();

  @Test
  public void emptySql() {
    assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(
            operation::compile);
  }

  @Test
  public void setTypeAfterCompile() {
    operation.setDataSource(new DriverManagerDataSource());
    operation.setSql("select * from mytable");
    operation.compile();
    assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(() ->
            operation.setTypes(new int[] { Types.INTEGER }));
  }

  @Test
  public void declareParameterAfterCompile() {
    operation.setDataSource(new DriverManagerDataSource());
    operation.setSql("select * from mytable");
    operation.compile();
    assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(() ->
            operation.declareParameter(new SqlParameter(Types.INTEGER)));
  }

  @Test
  public void tooFewParameters() {
    operation.setDataSource(new DriverManagerDataSource());
    operation.setSql("select * from mytable");
    operation.setTypes(new int[] { Types.INTEGER });
    assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(() ->
            operation.validateParameters(null)).withMessageContaining("0 parameters were supplied, but 1 in parameters were declared");
  }

  @Test
  public void tooFewMapParameters() {
    operation.setDataSource(new DriverManagerDataSource());
    operation.setSql("select * from mytable");
    operation.setTypes(new int[] { Types.INTEGER });
    assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(() ->
            operation.validateNamedParameters((Map<String, String>) null));
  }

  @Test
  public void operationConfiguredViaJdbcTemplateMustGetDataSource() {
    operation.setSql("foo");
    assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
            .isThrownBy(operation::compile)
            .withMessageContaining("'dataSource'");
  }

  @Test
  public void tooManyParameters() {
    operation.setDataSource(new DriverManagerDataSource());
    operation.setSql("select * from mytable");
    assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(() ->
            operation.validateParameters(new Object[] { 1, 2 }));
  }

  @Test
  public void tooManyMapParameters() {
    operation.setDataSource(new DriverManagerDataSource());
    operation.setSql("select * from mytable");
    assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(() ->
            operation.validateNamedParameters(Map.of("a", "b", "c", "d")));
  }

  @Test
  public void unspecifiedMapParameters() {
    operation.setDataSource(new DriverManagerDataSource());
    operation.setSql("select * from mytable");
    operation.declareParameter(new SqlParameter("id", Types.INTEGER));
    Map<String, String> params = new HashMap<>();
    params.put("col1", "value");
    assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(() ->
            operation.validateNamedParameters(params)).withMessageContaining("parameter named 'id'", "col1");
  }

  @Test
  public void compileTwice() {
    operation.setDataSource(new DriverManagerDataSource());
    operation.setSql("select * from mytable");
    operation.setTypes(null);
    operation.compile();
    operation.compile();
  }

  @Test
  public void emptyDataSource() {
    SqlOperation operation = new SqlOperation() { };
    operation.setSql("select * from mytable");
    assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(
            operation::compile);
  }

  @Test
  public void parameterPropagation() {
    SqlOperation operation = new SqlOperation() { };
    DataSource ds = new DriverManagerDataSource();
    operation.setDataSource(ds);
    operation.setFetchSize(10);
    operation.setMaxRows(20);
    JdbcTemplate jt = operation.getJdbcTemplate();
    Assertions.assertThat(jt.getDataSource()).isEqualTo(ds);
    assertThat(jt.getFetchSize()).isEqualTo(10);
    assertThat(jt.getMaxRows()).isEqualTo(20);
  }

  @Test
  public void validateInOutParameter() {
    operation.setDataSource(new DriverManagerDataSource());
    operation.setSql("DUMMY_PROC");
    operation.declareParameter(new SqlOutParameter("DUMMY_OUT_PARAM", Types.VARCHAR));
    operation.declareParameter(new SqlInOutParameter("DUMMY_IN_OUT_PARAM", Types.VARCHAR));
    operation.validateParameters(new Object[] { "DUMMY_VALUE1", "DUMMY_VALUE2" });
  }

  @Test
  public void parametersSetWithList() {
    DataSource ds = new DriverManagerDataSource();
    operation.setDataSource(ds);
    operation.setSql("select * from mytable where one = ? and two = ?");
    operation.setParameters(
            new SqlParameter("one", Types.NUMERIC),
            new SqlParameter("two", Types.NUMERIC)
    );
    operation.afterPropertiesSet();
    operation.validateParameters(new Object[] { 1, "2" });
    assertThat(operation.getDeclaredParameters().size()).isEqualTo(2);
  }

  @Test
  void appliesStatementSettingsSuppliedBeforeAndAfterDataSource() {
    operation.setFetchSize(10);
    operation.setMaxRows(20);
    operation.setQueryTimeout(30);
    operation.setDataSource(new DriverManagerDataSource());
    var template = operation.getJdbcTemplate();
    assertThat(template.getFetchSize()).isEqualTo(10);
    assertThat(template.getMaxRows()).isEqualTo(20);
    assertThat(template.getQueryTimeout()).isEqualTo(30);

    operation.setQueryTimeout(45);
    assertThat(template.getQueryTimeout()).isEqualTo(45);
  }

  @Test
  void preventsChangingStatementShapeAfterCompilation() {
    operation.setDataSource(new DriverManagerDataSource());
    operation.setSql("insert into users (name) values (?)");
    operation.setUpdatableResults(true);
    operation.setReturnGeneratedKeys(true);
    operation.setGeneratedKeysColumnNames("id");
    operation.setParameters(new SqlParameter("name", Types.VARCHAR));
    operation.compile();

    assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
            .isThrownBy(() -> operation.setUpdatableResults(false)).withMessageContaining("updateableResults");
    assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
            .isThrownBy(() -> operation.setReturnGeneratedKeys(false)).withMessageContaining("returnGeneratedKeys");
    assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
            .isThrownBy(() -> operation.setGeneratedKeysColumnNames("other")).withMessageContaining("column names");
    assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
            .isThrownBy(() -> operation.setParameters()).withMessageContaining("compiled");
    assertThat(operation.isUpdatableResults()).isTrue();
    assertThat(operation.isReturnGeneratedKeys()).isTrue();
    assertThat(operation.getGeneratedKeysColumnNames()).containsExactly("id");
  }

  @Test
  void reportsNullParameterIndex() {
    assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
            .isThrownBy(() -> operation.setParameters(new SqlParameter("id", Types.INTEGER), null))
            .withMessageContaining("index 1", "'null'");

    assertThat(operation.isCompiled()).isFalse();
  }

  @Test
  void replacingDataSourcePreservesJdbcTemplateSettings() {
    var original = new JdbcTemplate(new DriverManagerDataSource());
    original.setFetchSize(12);
    original.setIgnoreWarnings(false);
    operation.setJdbcTemplate(original);
    var replacement = new DriverManagerDataSource();

    operation.setDataSource(replacement);

    var configured = operation.getJdbcTemplate();
    assertThat(configured).isNotSameAs(original);
    assertThat(configured.getDataSource()).isSameAs(replacement);
    assertThat(configured.getFetchSize()).isEqualTo(12);
    assertThat(configured.isIgnoreWarnings()).isFalse();
  }

  @ParameterizedTest
  @ValueSource(ints = { Types.BLOB, Types.CLOB })
  void rejectsLobInputsWhenTheOperationDoesNotSupportThem(int type) {
    var noLobs = new TestRdbmsOperation() {
      @Override
      protected boolean supportsLobParameters() {
        return false;
      }
    };
    noLobs.setDataSource(new DriverManagerDataSource());
    noLobs.setSql("update users set data = ?");
    noLobs.declareParameter(new SqlParameter("data", type));
    assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
            .isThrownBy(() -> noLobs.validateParameters(new Object[] { "value" })).withMessageContaining("BLOB or CLOB");
    assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
            .isThrownBy(() -> noLobs.validateNamedParameters(Map.of("data", "value"))).withMessageContaining("BLOB or CLOB");
  }

  @Test
  void validatesNamedInputsWithoutRequiringOutputParametersAndCanAllowExtras() {
    var acceptsExtras = new TestRdbmsOperation() {
      @Override
      protected boolean allowsUnusedParameters() {
        return true;
      }
    };
    acceptsExtras.setDataSource(new DriverManagerDataSource());
    acceptsExtras.setSql("call load_user(?)");
    acceptsExtras.setParameters(new SqlOutParameter("result", Types.BIGINT), new SqlParameter("id", Types.INTEGER));

    assertThatCode(() -> acceptsExtras.validateNamedParameters(Map.of("id", 1, "extra1", 2, "extra2", 3)))
            .doesNotThrowAnyException();
  }

  private static class TestRdbmsOperation extends RdbmsOperation {

    @Override
    protected void compileInternal() {
    }
  }

}

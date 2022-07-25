/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

import org.junit.jupiter.api.Test;

import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import cn.taketoday.dao.InvalidDataAccessApiUsageException;
import cn.taketoday.jdbc.core.JdbcTemplate;
import cn.taketoday.jdbc.core.SqlInOutParameter;
import cn.taketoday.jdbc.core.SqlOutParameter;
import cn.taketoday.jdbc.core.SqlParameter;
import cn.taketoday.jdbc.datasource.DriverManagerDataSource;

import static org.assertj.core.api.Assertions.assertThat;
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
    operation.setSql("select * from mytable");
    operation.setTypes(new int[] { Types.INTEGER });
    assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(() ->
            operation.validateParameters((Object[]) null));
  }

  @Test
  public void tooFewMapParameters() {
    operation.setSql("select * from mytable");
    operation.setTypes(new int[] { Types.INTEGER });
    assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(() ->
            operation.validateNamedParameters((Map<String, String>) null));
  }

  @Test
  public void operationConfiguredViaJdbcTemplateMustGetDataSource() throws Exception {
    operation.setSql("foo");
    assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(() ->
                    operation.compile())
            .withMessageContaining("ataSource");
  }

  @Test
  public void tooManyParameters() {
    operation.setSql("select * from mytable");
    assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(() ->
            operation.validateParameters(new Object[] { 1, 2 }));
  }

  @Test
  public void unspecifiedMapParameters() {
    operation.setSql("select * from mytable");
    Map<String, String> params = new HashMap<>();
    params.put("col1", "value");
    assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(() ->
            operation.validateNamedParameters(params));
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
    assertThat(jt.getDataSource()).isEqualTo(ds);
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
    operation.setParameters(new SqlParameter[] {
            new SqlParameter("one", Types.NUMERIC),
            new SqlParameter("two", Types.NUMERIC) });
    operation.afterPropertiesSet();
    operation.validateParameters(new Object[] { 1, "2" });
    assertThat(operation.getDeclaredParameters().size()).isEqualTo(2);
  }

  private static class TestRdbmsOperation extends RdbmsOperation {

    @Override
    protected void compileInternal() {
    }
  }

}

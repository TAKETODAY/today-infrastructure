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

package infra.jdbc.core;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;

import infra.beans.BeanWrapper;
import infra.jdbc.core.test.ConcretePerson;
import infra.jdbc.core.test.ConstructorPerson;
import infra.jdbc.core.test.DatePerson;
import infra.jdbc.core.test.EmailPerson;
import infra.jdbc.core.test.Person;
import infra.jdbc.core.test.SpacePerson;
import infra.jdbc.datasource.SingleConnectionDataSource;
import infra.jdbc.support.SQLStateSQLExceptionTranslator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Mock object based abstract class for RowMapper tests.
 * Initializes mock objects and verifies results.
 *
 * @author Thomas Risberg
 */
public abstract class AbstractRowMapperTests {

  protected void verifyPerson(Person person) {
    assertThat(person.getName()).isEqualTo("Bubba");
    assertThat(person.getAge()).isEqualTo(22L);
    assertThat(person.getBirth_date()).usingComparator(Date::compareTo).isEqualTo(new Date(1221222L));
    assertThat(person.getBalance()).isEqualTo(new BigDecimal("1234.56"));
    verifyPersonViaBeanWrapper(person);
  }

  protected void verifyPerson(ConcretePerson person) {
    assertThat(person.getName()).isEqualTo("Bubba");
    assertThat(person.getAge()).isEqualTo(22L);
    assertThat(person.getBirth_date()).usingComparator(Date::compareTo).isEqualTo(new Date(1221222L));
    assertThat(person.getBalance()).isEqualTo(new BigDecimal("1234.56"));
    verifyPersonViaBeanWrapper(person);
  }

  protected void verifyPerson(SpacePerson person) {
    assertThat(person.getLastName()).isEqualTo("Bubba");
    assertThat(person.getAge()).isEqualTo(22L);
    assertThat(person.getBirthDate()).isEqualTo(new Timestamp(1221222L).toLocalDateTime());
    assertThat(person.getBalance()).isEqualTo(new BigDecimal("1234.56"));
  }

  protected void verifyPerson(DatePerson person) {
    assertThat(person.getLastName()).isEqualTo("Bubba");
    assertThat(person.getAge()).isEqualTo(22L);
    assertThat(person.getBirthDate()).isEqualTo(new java.sql.Date(1221222L).toLocalDate());
    assertThat(person.getBalance()).isEqualTo(new BigDecimal("1234.56"));
  }

  protected void verifyPerson(ConstructorPerson person) {
    assertThat(person.name()).isEqualTo("Bubba");
    assertThat(person.age()).isEqualTo(22L);
    assertThat(person.birth_date()).usingComparator(Date::compareTo).isEqualTo(new Date(1221222L));
    assertThat(person.balance()).isEqualTo(new BigDecimal("1234.56"));
    verifyPersonViaBeanWrapper(person);
  }

  protected void verifyPersonViaBeanWrapper(Object person) {
    BeanWrapper accessor = BeanWrapper.forBeanPropertyAccess(person);
    assertThat(accessor.getPropertyValue("name")).isEqualTo("Bubba");
    assertThat(accessor.getPropertyValue("age")).isEqualTo(22L);

    assertThat((Date) accessor.getPropertyValue("birth_date"))
            .usingComparator(Date::compareTo)
            .isEqualTo(new Date(1221222L));

    assertThat(accessor.getPropertyValue("balance")).isEqualTo(new BigDecimal("1234.56"));
  }

  protected void verifyPerson(EmailPerson person) {
    assertThat(person.getName()).isEqualTo("Bubba");
    assertThat(person.getAge()).isEqualTo(22L);
    assertThat(person.getBirth_date()).usingComparator(Date::compareTo).isEqualTo(new java.util.Date(1221222L));
    assertThat(person.getBalance()).isEqualTo(new BigDecimal("1234.56"));
    assertThat(person.getEMail()).isEqualTo("hello@world.info");
  }

  protected enum MockType {
    ONE, TWO, THREE, FOUR
  }

  protected static class Mock {

    private Connection connection = mock();

    private ResultSetMetaData resultSetMetaData = mock();

    private ResultSet resultSet = mock();

    private Statement statement = mock();

    private JdbcTemplate jdbcTemplate;

    public Mock() throws Exception {
      this(MockType.ONE);
    }

    @SuppressWarnings("unchecked")
    public Mock(MockType type) throws Exception {
      given(connection.createStatement()).willReturn(statement);
      given(statement.executeQuery(anyString())).willReturn(resultSet);
      given(resultSet.getMetaData()).willReturn(resultSetMetaData);

      given(resultSet.next()).willReturn(true, false);
      given(resultSet.getString(1)).willReturn("Bubba");
      given(resultSet.getLong(2)).willReturn(22L);
      given(resultSet.getTimestamp(3)).willReturn(new Timestamp(1221222L));
      given(resultSet.getObject(anyInt(), any(Class.class))).willThrow(new SQLFeatureNotSupportedException());
      given(resultSet.getDate(3)).willReturn(new java.sql.Date(1221222L));
      given(resultSet.getBigDecimal(4)).willReturn(new BigDecimal("1234.56"));
      given(resultSet.getObject(4)).willReturn(new BigDecimal("1234.56"));
      given(resultSet.getString(5)).willReturn("hello@world.info");
      given(resultSet.wasNull()).willReturn(type == MockType.TWO);

      given(resultSetMetaData.getColumnCount()).willReturn(5);
      given(resultSetMetaData.getColumnLabel(1)).willReturn(
              type == MockType.THREE ? "Last Name" : "name");
      given(resultSetMetaData.getColumnLabel(2)).willReturn("age");
      given(resultSetMetaData.getColumnLabel(3)).willReturn(type == MockType.FOUR ? "birthdate" : "birth_date");
      given(resultSetMetaData.getColumnLabel(4)).willReturn("balance");
      given(resultSetMetaData.getColumnLabel(5)).willReturn("e_mail");

      given(resultSet.findColumn("name")).willReturn(1);
      given(resultSet.findColumn("age")).willReturn(2);
      if (type == MockType.FOUR) {
        given(resultSet.findColumn("birthdate")).willReturn(3);
      }
      else {
        given(resultSet.findColumn("birthdate")).willThrow(new SQLException());
        given(resultSet.findColumn("birthDate")).willThrow(new SQLException());
        given(resultSet.findColumn("birth_date")).willReturn(3);
      }
      given(resultSet.findColumn("balance")).willReturn(4);
      given(resultSet.findColumn("e_mail")).willReturn(5);

      jdbcTemplate = new JdbcTemplate();
      jdbcTemplate.setDataSource(new SingleConnectionDataSource(connection, false));
      jdbcTemplate.setExceptionTranslator(new SQLStateSQLExceptionTranslator());
      jdbcTemplate.afterPropertiesSet();
    }

    public JdbcTemplate getJdbcTemplate() {
      return jdbcTemplate;
    }

    public void verifyClosed() throws Exception {
      verify(resultSet).close();
      verify(statement).close();
    }
  }

}

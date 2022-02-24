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

package cn.taketoday.jdbc.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import cn.taketoday.dao.InvalidDataAccessApiUsageException;
import cn.taketoday.jdbc.core.test.ConcretePerson;
import cn.taketoday.jdbc.core.test.DatePerson;
import cn.taketoday.jdbc.core.test.EmailPerson;
import cn.taketoday.jdbc.core.test.ExtendedPerson;
import cn.taketoday.jdbc.core.test.Person;
import cn.taketoday.jdbc.core.test.SpacePerson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Thomas Risberg
 * @author Juergen Hoeller
 */
public class BeanPropertyRowMapperTests extends AbstractRowMapperTests {

  @Test
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void testOverridingDifferentClassDefinedForMapping() {
    BeanPropertyRowMapper mapper = new BeanPropertyRowMapper(Person.class);
    assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
            .isThrownBy(() -> mapper.setMappedClass(Long.class));
  }

  @Test
  public void testOverridingSameClassDefinedForMapping() {
    BeanPropertyRowMapper<Person> mapper = new BeanPropertyRowMapper<>(Person.class);
    mapper.setMappedClass(Person.class);
  }

  @Test
  public void testStaticQueryWithRowMapper() throws Exception {
    Mock mock = new Mock();
    List<Person> result = mock.getJdbcTemplate().query(
            "select name, age, birth_date, balance from people", new BeanPropertyRowMapper<>(Person.class));
    assertThat(result.size()).isEqualTo(1);
    verifyPerson(result.get(0));
    mock.verifyClosed();
  }

  @Test
  public void testMappingWithInheritance() throws Exception {
    Mock mock = new Mock();
    List<ConcretePerson> result = mock.getJdbcTemplate().query(
            "select name, age, birth_date, balance from people", new BeanPropertyRowMapper<>(ConcretePerson.class));
    assertThat(result.size()).isEqualTo(1);
    verifyPerson(result.get(0));
    mock.verifyClosed();
  }

  @Test
  public void testMappingWithNoUnpopulatedFieldsFound() throws Exception {
    Mock mock = new Mock();
    List<ConcretePerson> result = mock.getJdbcTemplate().query(
            "select name, age, birth_date, balance from people",
            new BeanPropertyRowMapper<>(ConcretePerson.class, true));
    assertThat(result.size()).isEqualTo(1);
    verifyPerson(result.get(0));
    mock.verifyClosed();
  }

  @Test
  public void testMappingWithUnpopulatedFieldsNotChecked() throws Exception {
    Mock mock = new Mock();
    List<ExtendedPerson> result = mock.getJdbcTemplate().query(
            "select name, age, birth_date, balance from people",
            new BeanPropertyRowMapper<>(ExtendedPerson.class));
    assertThat(result.size()).isEqualTo(1);
    ExtendedPerson bean = result.get(0);
    verifyPerson(bean);
    mock.verifyClosed();
  }

  @Test
  public void testMappingWithUnpopulatedFieldsNotAccepted() throws Exception {
    Mock mock = new Mock();
    assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
            .isThrownBy(() -> mock.getJdbcTemplate().query("select name, age, birth_date, balance from people",
                    new BeanPropertyRowMapper<>(ExtendedPerson.class, true)));
  }

  @Test
  public void testMappingNullValue() throws Exception {
    BeanPropertyRowMapper<Person> mapper = new BeanPropertyRowMapper<>(Person.class);
    Mock mock = new Mock(MockType.TWO);
    List<Person> query = mock.getJdbcTemplate()
            .query("select name, null as age, birth_date, balance from people", mapper);

    System.out.println(query);
//    assertThatExceptionOfType(TypeMismatchException.class)
//            .isThrownBy(() -> );
  }

  @Test
  public void testQueryWithSpaceInColumnNameAndLocalDateTime() throws Exception {
    Mock mock = new Mock(MockType.THREE);
    List<SpacePerson> result = mock.getJdbcTemplate().query(
            "select last_name as \"Last Name\", age, birth_date, balance from people",
            new BeanPropertyRowMapper<>(SpacePerson.class));
    assertThat(result.size()).isEqualTo(1);
    verifyPerson(result.get(0));
    mock.verifyClosed();
  }

  @Test
  public void testQueryWithSpaceInColumnNameAndLocalDate() throws Exception {
    Mock mock = new Mock(MockType.THREE);
    List<DatePerson> result = mock.getJdbcTemplate().query(
            "select last_name as \"Last Name\", age, birth_date, balance from people",
            new BeanPropertyRowMapper<>(DatePerson.class));
    assertThat(result.size()).isEqualTo(1);
    verifyPerson(result.get(0));
    mock.verifyClosed();
  }

  @Test
  void queryWithUnderscoreInColumnNameAndPersonWithMultipleAdjacentUppercaseLettersInPropertyName() throws Exception {
    Mock mock = new Mock();
    List<EmailPerson> result = mock.getJdbcTemplate().query(
            "select name, age, birth_date, balance, e_mail from people",
            new BeanPropertyRowMapper<>(EmailPerson.class));
    assertThat(result).hasSize(1);
    verifyPerson(result.get(0));
    mock.verifyClosed();
  }

  @ParameterizedTest
  @CsvSource({
          "age, age",
          "lastName, last_name",
          "Name, name",
          "FirstName, first_name",
          "EMail, e_mail",
          "URL, u_r_l", // likely undesirable, but that's the status quo
  })
  void underscoreName(String input, String expected) {
    BeanPropertyRowMapper<?> mapper = new BeanPropertyRowMapper<>(Object.class);
    assertThat(mapper.underscoreName(input)).isEqualTo(expected);
  }

}

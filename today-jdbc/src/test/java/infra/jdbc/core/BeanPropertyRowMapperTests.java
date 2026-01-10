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

package infra.jdbc.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import infra.beans.TypeMismatchException;
import infra.dao.InvalidDataAccessApiUsageException;
import infra.jdbc.core.test.ConcretePerson;
import infra.jdbc.core.test.DatePerson;
import infra.jdbc.core.test.EmailPerson;
import infra.jdbc.core.test.ExtendedPerson;
import infra.jdbc.core.test.Person;
import infra.jdbc.core.test.SpacePerson;

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
    BeanPropertyRowMapper mapper = RowMapper.forMappedClass(Person.class);
    assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
            .isThrownBy(() -> mapper.setMappedClass(Long.class));
  }

  @Test
  public void testOverridingSameClassDefinedForMapping() {
    BeanPropertyRowMapper<Person> mapper = RowMapper.forMappedClass(Person.class);
    mapper.setMappedClass(Person.class);
  }

  @Test
  public void testStaticQueryWithRowMapper() throws Exception {
    Mock mock = new Mock();
    List<Person> result = mock.getJdbcTemplate().query(
            "select name, age, birth_date, balance from people", RowMapper.forMappedClass(Person.class));
    assertThat(result.size()).isEqualTo(1);
    verifyPerson(result.get(0));
    mock.verifyClosed();
  }

  @Test
  public void testMappingWithInheritance() throws Exception {
    Mock mock = new Mock();
    List<ConcretePerson> result = mock.getJdbcTemplate().query(
            "select name, age, birth_date, balance from people", RowMapper.forMappedClass(ConcretePerson.class));
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
            RowMapper.forMappedClass(ExtendedPerson.class));
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
  void mappingNullValue() throws Exception {
    BeanPropertyRowMapper<Person> mapper = RowMapper.forMappedClass(Person.class);
    Mock mock = new Mock(MockType.TWO);
    assertThatExceptionOfType(TypeMismatchException.class).isThrownBy(() ->
            mock.getJdbcTemplate().query("select name, null as age, birth_date, balance from people", mapper));
  }

  @Test
  public void testQueryWithSpaceInColumnNameAndLocalDateTime() throws Exception {
    Mock mock = new Mock(MockType.THREE);
    List<SpacePerson> result = mock.getJdbcTemplate().query(
            "select last_name as \"Last Name\", age, birth_date, balance from people",
            RowMapper.forMappedClass(SpacePerson.class));
    assertThat(result.size()).isEqualTo(1);
    verifyPerson(result.get(0));
    mock.verifyClosed();
  }

  @Test
  public void testQueryWithSpaceInColumnNameAndLocalDate() throws Exception {
    Mock mock = new Mock(MockType.THREE);
    List<DatePerson> result = mock.getJdbcTemplate().query(
            "select last_name as \"Last Name\", age, birth_date, balance from people",
            RowMapper.forMappedClass(DatePerson.class));
    assertThat(result.size()).isEqualTo(1);
    verifyPerson(result.get(0));
    mock.verifyClosed();
  }

  @Test
  void queryWithUnderscoreInColumnNameAndPersonWithMultipleAdjacentUppercaseLettersInPropertyName() throws Exception {
    Mock mock = new Mock();
    List<EmailPerson> result = mock.getJdbcTemplate().query(
            "select name, age, birth_date, balance, e_mail from people",
            RowMapper.forMappedClass(EmailPerson.class));
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
    BeanPropertyRowMapper<?> mapper = RowMapper.forMappedClass(Object.class);
    assertThat(mapper.underscoreName(input)).isEqualTo(expected);
  }

}

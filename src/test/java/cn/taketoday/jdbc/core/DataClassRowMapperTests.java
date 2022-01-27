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

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import cn.taketoday.jdbc.core.test.ConstructorPerson;
import cn.taketoday.jdbc.core.test.ConstructorPersonWithGenerics;
import cn.taketoday.jdbc.core.test.ConstructorPersonWithSetters;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 */
public class DataClassRowMapperTests extends AbstractRowMapperTests {

  @Test
  public void testStaticQueryWithDataClass() throws Exception {
    Mock mock = new Mock();
    List<ConstructorPerson> result = mock.getJdbcTemplate().query( // language=MySQL
            "select name, age, birth_date, balance from people", new DataClassRowMapper<>(ConstructorPerson.class));
    assertThat(result.size()).isEqualTo(1);
    verifyPerson(result.get(0));

    mock.verifyClosed();
  }

  @Test
  public void testStaticQueryWithDataClassAndGenerics() throws Exception {
    Mock mock = new Mock();
    List<ConstructorPersonWithGenerics> result = mock.getJdbcTemplate().query(// language=MySQL
            "select name, age, birth_date, balance from people", new DataClassRowMapper<>(ConstructorPersonWithGenerics.class));
    assertThat(result.size()).isEqualTo(1);
    ConstructorPersonWithGenerics person = result.get(0);
    assertThat(person.name()).isEqualTo("Bubba");
    assertThat(person.age()).isEqualTo(22L);
    assertThat(person.birth_date()).usingComparator(Date::compareTo).isEqualTo(new Date(1221222L));
    assertThat(person.balance()).isEqualTo(Collections.singletonList(new BigDecimal("1234.56")));

    mock.verifyClosed();
  }

  @Test
  public void testStaticQueryWithDataClassAndSetters() throws Exception {
    Mock mock = new Mock();
    List<ConstructorPersonWithSetters> result = mock.getJdbcTemplate().query(
            "select name, age, birth_date, balance from people",
            new DataClassRowMapper<>(ConstructorPersonWithSetters.class));
    assertThat(result.size()).isEqualTo(1);
    ConstructorPersonWithSetters person = result.get(0);
    assertThat(person.name()).isEqualTo("BUBBA");
    assertThat(person.age()).isEqualTo(22L);
    assertThat(person.birth_date()).usingComparator(Date::compareTo).isEqualTo(new Date(1221222L));
    assertThat(person.balance()).isEqualTo(new BigDecimal("1234.56"));

    mock.verifyClosed();
  }

  @Test
  public void testStaticQueryWithDataRecord() throws Exception {
    Mock mock = new Mock();
    List<RecordPerson> result = mock.getJdbcTemplate().query(
            "select name, age, birth_date, balance from people",
            new DataClassRowMapper<>(RecordPerson.class));
    assertThat(result.size()).isEqualTo(1);
    verifyPerson(result.get(0));

    mock.verifyClosed();
  }

  protected void verifyPerson(RecordPerson person) {
    assertThat(person.name()).isEqualTo("Bubba");
    assertThat(person.age()).isEqualTo(22L);
    assertThat(person.birth_date()).usingComparator(Date::compareTo).isEqualTo(new Date(1221222L));
    assertThat(person.balance()).isEqualTo(new BigDecimal("1234.56"));
    verifyPersonViaBeanWrapper(person);
  }

  static record RecordPerson(String name, long age, Date birth_date, BigDecimal balance) {
  }

}

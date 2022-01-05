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
package cn.taketoday.orm.mybatis.sample;

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import cn.taketoday.batch.core.JobExecution;
import cn.taketoday.batch.test.JobLauncherTestUtils;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.jdbc.core.namedparam.EmptySqlParameterSource;
import cn.taketoday.jdbc.core.namedparam.NamedParameterJdbcTemplate;

abstract class AbstractSampleJobTest {

  @Autowired
  private JobLauncherTestUtils jobLauncherTestUtils;

  @Autowired
  private NamedParameterJdbcTemplate jdbcTemplate;

  @Test
  void testJob() throws Exception {

    JobExecution jobExecution = jobLauncherTestUtils.launchJob();

    Assertions.assertEquals("COMPLETED", jobExecution.getExitStatus().getExitCode());

    List<Map<String, Object>> persons = jdbcTemplate.queryForList("SELECT * FROM persons ORDER BY person_id",
        EmptySqlParameterSource.INSTANCE);
    Assertions.assertEquals(5, persons.size());
    Object operationBy = persons.get(0).get("OPERATION_BY");
    Object operationAt = persons.get(0).get("OPERATION_AT");
    {
      Map<String, Object> person = persons.get(0);
      Assertions.assertEquals(0, person.get("PERSON_ID"));
      Assertions.assertEquals("Pocoyo", person.get("FIRST_NAME"));
      Assertions.assertNull(person.get("LAST_NAME"));
      Assertions.assertEquals(getExpectedOperationBy(), operationBy);
      Assertions.assertNotNull(operationAt);
    }
    {
      Map<String, Object> person = persons.get(1);
      Assertions.assertEquals(1, person.get("PERSON_ID"));
      Assertions.assertEquals("Pato", person.get("FIRST_NAME"));
      Assertions.assertNull(person.get("LAST_NAME"));
      Assertions.assertEquals(operationBy, person.get("OPERATION_BY"));
      Assertions.assertEquals(operationAt, person.get("OPERATION_AT"));
    }
    {
      Map<String, Object> person = persons.get(2);
      Assertions.assertEquals(2, person.get("PERSON_ID"));
      Assertions.assertEquals("Eli", person.get("FIRST_NAME"));
      Assertions.assertNull(person.get("LAST_NAME"));
      Assertions.assertEquals(operationBy, person.get("OPERATION_BY"));
      Assertions.assertEquals(operationAt, person.get("OPERATION_AT"));
    }
    {
      Map<String, Object> person = persons.get(3);
      Assertions.assertEquals(3, person.get("PERSON_ID"));
      Assertions.assertEquals("Valentina", person.get("FIRST_NAME"));
      Assertions.assertNull(person.get("LAST_NAME"));
      Assertions.assertEquals(operationBy, person.get("OPERATION_BY"));
      Assertions.assertEquals(operationAt, person.get("OPERATION_AT"));
    }
    {
      Map<String, Object> person = persons.get(4);
      Assertions.assertEquals(4, person.get("PERSON_ID"));
      Assertions.assertEquals("Taro", person.get("FIRST_NAME"));
      Assertions.assertEquals("Yamada", person.get("LAST_NAME"));
      Assertions.assertEquals(operationBy, person.get("OPERATION_BY"));
      Assertions.assertEquals(operationAt, person.get("OPERATION_AT"));
    }
  }

  protected abstract String getExpectedOperationBy();

  @Configuration
  static class LocalContext {
    @Bean
    JobLauncherTestUtils jobLauncherTestUtils() {
      return new JobLauncherTestUtils();
    }

    @Bean
    NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource dataSource) {
      return new NamedParameterJdbcTemplate(dataSource);
    }
  }

}

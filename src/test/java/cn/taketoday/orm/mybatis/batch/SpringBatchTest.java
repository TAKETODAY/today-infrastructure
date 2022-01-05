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
package cn.taketoday.orm.mybatis.batch;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.Test;
import cn.taketoday.orm.mybatis.batch.domain.Employee;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.factory.annotation.Qualifier;
import cn.taketoday.test.context.junit.jupiter.SpringJUnitConfig;
import cn.taketoday.transaction.annotation.Transactional;

@SpringJUnitConfig(locations = { "classpath:org/mybatis/spring/batch/applicationContext.xml" })
class SpringBatchTest {

  @Autowired
  @Qualifier("pagingNoNestedItemReader")
  private MyBatisPagingItemReader<Employee> pagingNoNestedItemReader;

  @Autowired
  @Qualifier("pagingNestedItemReader")
  private MyBatisPagingItemReader<Employee> pagingNestedItemReader;

  @Autowired
  @Qualifier("cursorNoNestedItemReader")
  private MyBatisCursorItemReader<Employee> cursorNoNestedItemReader;

  @Autowired
  @Qualifier("cursorNestedItemReader")
  private MyBatisCursorItemReader<Employee> cursorNestedItemReader;

  @Autowired
  private MyBatisBatchItemWriter<Employee> writer;

  @Autowired
  private SqlSession session;

  @Test
  @Transactional
  void shouldDuplicateSalaryOfAllEmployees() throws Exception {
    List<Employee> employees = new ArrayList<>();
    Employee employee = pagingNoNestedItemReader.read();
    while (employee != null) {
      employee.setSalary(employee.getSalary() * 2);
      employees.add(employee);
      employee = pagingNoNestedItemReader.read();
    }
    writer.write(employees);

    assertThat((Integer) session.selectOne("checkSalarySum")).isEqualTo(20000);
    assertThat((Integer) session.selectOne("checkEmployeeCount")).isEqualTo(employees.size());
  }

  @Test
  @Transactional
  void checkPagingReadingWithNestedInResultMap() throws Exception {
    // This test is here to show that PagingReader can return wrong result in case of nested result maps
    List<Employee> employees = new ArrayList<>();
    Employee employee = pagingNestedItemReader.read();
    while (employee != null) {
      employee.setSalary(employee.getSalary() * 2);
      employees.add(employee);
      employee = pagingNestedItemReader.read();
    }
    writer.write(employees);

    // Assert that we have a WRONG employee count
    assertThat((Integer) session.selectOne("checkEmployeeCount")).isNotEqualTo(employees.size());
  }

  @Test
  @Transactional
  void checkCursorReadingWithoutNestedInResultMap() throws Exception {
    cursorNoNestedItemReader.doOpen();
    try {
      List<Employee> employees = new ArrayList<>();
      Employee employee = cursorNoNestedItemReader.read();
      while (employee != null) {
        employee.setSalary(employee.getSalary() * 2);
        employees.add(employee);
        employee = cursorNoNestedItemReader.read();
      }
      writer.write(employees);

      assertThat((Integer) session.selectOne("checkSalarySum")).isEqualTo(20000);
      assertThat((Integer) session.selectOne("checkEmployeeCount")).isEqualTo(employees.size());
    } finally {
      cursorNoNestedItemReader.doClose();
    }
  }

  @Test
  @Transactional
  void checkCursorReadingWithNestedInResultMap() throws Exception {
    cursorNestedItemReader.doOpen();
    try {
      List<Employee> employees = new ArrayList<>();
      Employee employee = cursorNestedItemReader.read();
      while (employee != null) {
        employee.setSalary(employee.getSalary() * 2);
        employees.add(employee);
        employee = cursorNestedItemReader.read();
      }
      writer.write(employees);

      assertThat((Integer) session.selectOne("checkSalarySum")).isEqualTo(20000);
      assertThat((Integer) session.selectOne("checkEmployeeCount")).isEqualTo(employees.size());
    } finally {
      cursorNestedItemReader.doClose();
    }
  }
}

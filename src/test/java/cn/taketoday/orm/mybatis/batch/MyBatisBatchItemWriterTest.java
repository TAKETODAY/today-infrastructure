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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.executor.BatchResult;
import org.apache.ibatis.session.ExecutorType;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import cn.taketoday.orm.mybatis.SqlSessionTemplate;
import cn.taketoday.orm.mybatis.batch.domain.Employee;
import cn.taketoday.dao.EmptyResultDataAccessException;
import cn.taketoday.dao.InvalidDataAccessResourceUsageException;

/**
 * @author Putthiphong Boonphong
 */
class MyBatisBatchItemWriterTest {

  @Mock
  private SqlSessionTemplate mockSqlSessionTemplate;

  @InjectMocks
  private MyBatisBatchItemWriter<Employee> writer;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  void testZeroBatchResultShouldThrowException() {
    List<Employee> employees = Arrays.asList(new Employee(), new Employee());
    List<BatchResult> batchResults = Lists.emptyList();

    given(mockSqlSessionTemplate.flushStatements()).willReturn(batchResults);

    assertThrows(InvalidDataAccessResourceUsageException.class, () -> writer.write(employees));
  }

  @Test
  void testZeroUpdateCountShouldThrowException() {
    List<Employee> employees = Arrays.asList(new Employee(), new Employee());

    BatchResult batchResult = new BatchResult(null, null);
    batchResult.setUpdateCounts(new int[] { 1, 0 });
    List<BatchResult> batchResults = Collections.singletonList(batchResult);

    given(mockSqlSessionTemplate.flushStatements()).willReturn(batchResults);

    assertThrows(EmptyResultDataAccessException.class, () -> writer.write(employees));
  }

  @Test
  void testItemToParameterConverterIsDefault() {
    this.writer.setAssertUpdates(false);
    this.writer.setStatementId("updateEmployee");

    Employee employee = new Employee();
    List<Employee> employees = Collections.singletonList(employee);
    writer.write(employees);

    Mockito.verify(this.mockSqlSessionTemplate).update("updateEmployee", employee);
  }

  @Test
  void testSetItemToParameterConverter() {
    this.writer.setAssertUpdates(false);
    this.writer.setStatementId("updateEmployee");
    this.writer.setItemToParameterConverter(item -> {
      Map<String, Object> parameter = new HashMap<>();
      parameter.put("item", item);
      parameter.put("now", LocalDateTime.now(Clock.fixed(Instant.ofEpochMilli(0), ZoneId.systemDefault())));
      return parameter;
    });

    Employee employee = new Employee();
    List<Employee> employees = Collections.singletonList(employee);
    writer.write(employees);

    Map<String, Object> parameter = new HashMap<>();
    parameter.put("item", employee);
    parameter.put("now", LocalDateTime.now(Clock.fixed(Instant.ofEpochMilli(0), ZoneId.systemDefault())));
    Mockito.verify(this.mockSqlSessionTemplate).update("updateEmployee", parameter);
  }

  @Test
  void testItemToParameterConverterIsNull() {
    given(mockSqlSessionTemplate.getExecutorType()).willReturn(ExecutorType.BATCH);
    this.writer.setStatementId("updateEmployee");
    writer.setItemToParameterConverter(null);

    assertThrows(IllegalArgumentException.class, () -> writer.afterPropertiesSet(),
        "A itemToParameterConverter is required.");

  }

}

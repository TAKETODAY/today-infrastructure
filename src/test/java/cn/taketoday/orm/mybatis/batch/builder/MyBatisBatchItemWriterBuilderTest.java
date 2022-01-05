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
package cn.taketoday.orm.mybatis.batch.builder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import javax.sql.DataSource;

import org.apache.ibatis.executor.BatchResult;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import cn.taketoday.orm.mybatis.SqlSessionTemplate;
import cn.taketoday.orm.mybatis.batch.MyBatisBatchItemWriter;

/**
 * Tests for {@link MyBatisBatchItemWriterBuilder}.
 *
 * @since 2.0.0
 * @author Kazuki Shimizu
 */
class MyBatisBatchItemWriterBuilderTest {

  @Mock
  private DataSource dataSource;

  @Mock
  private SqlSessionFactory sqlSessionFactory;

  @Mock
  private SqlSession sqlSession;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.initMocks(this);
    {
      Configuration configuration = new Configuration();
      Environment environment = new Environment("unittest", new JdbcTransactionFactory(), dataSource);
      configuration.setEnvironment(environment);
      Mockito.when(this.sqlSessionFactory.getConfiguration()).thenReturn(configuration);
      Mockito.when(this.sqlSessionFactory.openSession(ExecutorType.BATCH)).thenReturn(this.sqlSession);
    }
    {
      BatchResult result = new BatchResult(null, null);
      result.setUpdateCounts(new int[] { 1 });
      Mockito.when(this.sqlSession.flushStatements()).thenReturn(Collections.singletonList(result));
    }
  }

  @Test
  void testConfigurationUsingSqlSessionFactory() {

    // @formatter:off
    MyBatisBatchItemWriter<Foo> itemWriter = new MyBatisBatchItemWriterBuilder<Foo>()
            .sqlSessionFactory(this.sqlSessionFactory)
            .statementId("updateFoo")
            .build();
    // @formatter:on
    itemWriter.afterPropertiesSet();

    List<Foo> foos = getFoos();

    itemWriter.write(foos);

    Mockito.verify(this.sqlSession).update("updateFoo", foos.get(0));
    Mockito.verify(this.sqlSession).update("updateFoo", foos.get(1));
    Mockito.verify(this.sqlSession).update("updateFoo", foos.get(2));

  }

  @Test
  void testConfigurationUsingSqlSessionTemplate() {

    // @formatter:off
    MyBatisBatchItemWriter<Foo> itemWriter = new MyBatisBatchItemWriterBuilder<Foo>()
            .sqlSessionTemplate(new SqlSessionTemplate(this.sqlSessionFactory, ExecutorType.BATCH))
            .statementId("updateFoo")
            .build();
    // @formatter:on
    itemWriter.afterPropertiesSet();

    List<Foo> foos = getFoos();

    itemWriter.write(foos);

    Mockito.verify(this.sqlSession).update("updateFoo", foos.get(0));
    Mockito.verify(this.sqlSession).update("updateFoo", foos.get(1));
    Mockito.verify(this.sqlSession).update("updateFoo", foos.get(2));

  }

  @Test
  void testConfigurationAssertUpdatesIsFalse() {

    Mockito.when(this.sqlSession.flushStatements()).thenReturn(Collections.emptyList());

    // @formatter:off
    MyBatisBatchItemWriter<Foo> itemWriter = new MyBatisBatchItemWriterBuilder<Foo>()
            .sqlSessionTemplate(new SqlSessionTemplate(this.sqlSessionFactory, ExecutorType.BATCH))
            .statementId("updateFoo")
            .assertUpdates(false)
            .build();
    // @formatter:on
    itemWriter.afterPropertiesSet();

    List<Foo> foos = getFoos();

    itemWriter.write(foos);

    Mockito.verify(this.sqlSession).update("updateFoo", foos.get(0));
    Mockito.verify(this.sqlSession).update("updateFoo", foos.get(1));
    Mockito.verify(this.sqlSession).update("updateFoo", foos.get(2));

  }

  @Test
  void testConfigurationSetItemToParameterConverter() {

    // @formatter:off
    MyBatisBatchItemWriter<Foo> itemWriter = new MyBatisBatchItemWriterBuilder<Foo>()
            .sqlSessionFactory(this.sqlSessionFactory)
            .statementId("updateFoo")
            .itemToParameterConverter(item -> {
                Map<String, Object> parameter = new HashMap<>();
                parameter.put("item", item);
                parameter.put("now", LocalDateTime.now(Clock.fixed(Instant.ofEpochMilli(0), ZoneId.systemDefault())));
                return parameter;
            })
            .build();
    // @formatter:on
    itemWriter.afterPropertiesSet();

    List<Foo> foos = getFoos();

    itemWriter.write(foos);

    Map<String, Object> parameter = new HashMap<>();
    parameter.put("now", LocalDateTime.now(Clock.fixed(Instant.ofEpochMilli(0), ZoneId.systemDefault())));
    parameter.put("item", foos.get(0));
    Mockito.verify(this.sqlSession).update("updateFoo", parameter);
    parameter.put("item", foos.get(1));
    Mockito.verify(this.sqlSession).update("updateFoo", parameter);
    parameter.put("item", foos.get(2));
    Mockito.verify(this.sqlSession).update("updateFoo", parameter);
  }

  private List<Foo> getFoos() {
    return Arrays.asList(new Foo("foo1"), new Foo("foo2"), new Foo("foo3"));
  }

  private static class Foo {
    private final String name;

    Foo(String name) {
      this.name = name;
    }

    public String getName() {
      return this.name;
    }
  }

}

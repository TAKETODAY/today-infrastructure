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

import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import cn.taketoday.batch.item.ExecutionContext;
import cn.taketoday.batch.item.ItemStreamException;

/**
 * Tests for {@link MyBatisCursorItemReader}.
 */
class MyBatisCursorItemReaderTest {

  @Mock
  private SqlSessionFactory sqlSessionFactory;

  @Mock
  private SqlSession sqlSession;

  @Mock
  private Cursor<Object> cursor;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  void testCloseOnFailing() throws Exception {

    Mockito.when(this.sqlSessionFactory.openSession(ExecutorType.SIMPLE)).thenReturn(this.sqlSession);
    Mockito.when(this.cursor.iterator()).thenReturn(getFoos().iterator());
    Mockito.when(this.sqlSession.selectCursor("selectFoo", Collections.singletonMap("id", 1)))
        .thenThrow(new RuntimeException("error."));

    MyBatisCursorItemReader<Foo> itemReader = new MyBatisCursorItemReader<>();
    itemReader.setSqlSessionFactory(this.sqlSessionFactory);
    itemReader.setQueryId("selectFoo");
    itemReader.setParameterValues(Collections.singletonMap("id", 1));
    itemReader.afterPropertiesSet();

    ExecutionContext executionContext = new ExecutionContext();
    try {
      itemReader.open(executionContext);
      fail();
    } catch (ItemStreamException e) {
      Assertions.assertThat(e).hasMessage("Failed to initialize the reader").hasCause(new RuntimeException("error."));
    } finally {
      itemReader.close();
      Mockito.verify(this.sqlSession).close();
    }

  }

  @Test
  void testCloseBeforeOpen() {
    MyBatisCursorItemReader<Foo> itemReader = new MyBatisCursorItemReader<>();
    itemReader.close();
  }

  private List<Object> getFoos() {
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

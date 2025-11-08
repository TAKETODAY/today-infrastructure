/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.jdbc;

import org.junit.jupiter.api.Test;

import java.util.Map;

import infra.beans.BeanProperty;
import infra.jdbc.DefaultResultSetHandlerFactory.HandlerKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/7 15:37
 */
class DefaultResultSetHandlerFactoryTests {

  @Test
  void shouldCreateHandlerKeyWithValidParameters() {
    JdbcBeanMetadata metadata = new JdbcBeanMetadata(TestBean.class, true, false, true);
    RepositoryManager repositoryManager = mock(RepositoryManager.class);
    DefaultResultSetHandlerFactory<TestBean> factory = new DefaultResultSetHandlerFactory<>(metadata, repositoryManager, null);

    HandlerKey key1 = new HandlerKey("key1", factory);
    HandlerKey key2 = new HandlerKey("key1", factory);
    HandlerKey key3 = new HandlerKey("key2", factory);

    assertThat(key1).isEqualTo(key2);
    assertThat(key1).isNotEqualTo(key3);
    assertThat(key1.hashCode()).isEqualTo(key2.hashCode());
  }

  @Test
  void shouldHandlerKeyEqualsWithSameInstance() {
    JdbcBeanMetadata metadata = new JdbcBeanMetadata(TestBean.class, true, false, true);
    RepositoryManager repositoryManager = mock(RepositoryManager.class);
    DefaultResultSetHandlerFactory<TestBean> factory = new DefaultResultSetHandlerFactory<>(metadata, repositoryManager, null);
    HandlerKey key = new HandlerKey("key", factory);

    assertThat(key).isEqualTo(key);
    assertThat(key).isNotEqualTo(null);
    assertThat(key).isNotEqualTo(new Object());
  }




  static class TestBean {
    private String name;
    private int age;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public int getAge() {
      return age;
    }

    public void setAge(int age) {
      this.age = age;
    }
  }

}
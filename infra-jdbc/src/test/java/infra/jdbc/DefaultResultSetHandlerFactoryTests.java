/*
 * Copyright 2017 - 2026 the TODAY authors.
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
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

package infra.app.test.mock.mockito;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.reflect.Array;
import java.lang.reflect.Proxy;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.test.context.junit.jupiter.InfraExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

/**
 * Tests for {@link SpyBean @SpyBean} with a JDK proxy.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
@ExtendWith(InfraExtension.class)
class SpyBeanWithJdkProxyTests {

  @Autowired
  private ExampleService service;

  @SpyBean
  private ExampleRepository repository;

  @Test
  void jdkProxyCanBeSpied() {
    Example example = this.service.find("id");
    assertThat(example.id).isEqualTo("id");
    then(this.repository).should().find("id");
  }

  @Configuration(proxyBeanMethods = false)
  @Import(ExampleService.class)
  static class Config {

    @Bean
    ExampleRepository dateService() {
      ExampleRepositoryImpl impl = new ExampleRepositoryImpl();
      return (ExampleRepository) Proxy.newProxyInstance(
              getClass().getClassLoader(), new Class<?>[] { ExampleRepository.class },
              (proxy, method, args) -> {

                if (method.getName().equals("find")
                        && Array.getLength(args) == 1 && args[0] instanceof String id) {
                  return new Example(id);
                }

                return method.invoke(impl, args);
              });
    }

  }

  static class ExampleService {

    private final ExampleRepository repository;

    ExampleService(ExampleRepository repository) {
      this.repository = repository;
    }

    Example find(String id) {
      return this.repository.find(id);
    }

  }

  interface ExampleRepository {

    Example find(String id);

  }

  public static class ExampleRepositoryImpl implements ExampleRepository {

    @Override
    public Example find(String id) {
      throw new UnsupportedOperationException();
    }

  }

  static class Example {

    private final String id;

    Example(String id) {
      this.id = id;
    }

  }

}

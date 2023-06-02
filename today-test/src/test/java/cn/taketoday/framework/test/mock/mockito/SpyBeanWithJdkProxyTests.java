/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.framework.test.mock.mockito;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.reflect.Array;
import java.lang.reflect.Proxy;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.test.context.junit.jupiter.InfraExtension;

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

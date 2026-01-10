/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.test.context.web;

import org.junit.Test;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 4.0
 */
@ContextConfiguration
public class BasicAnnotationConfigWacTests extends AbstractBasicWacTests {

  @Configuration
  static class Config {

    @Bean
    public String foo() {
      return "enigma";
    }

    @Bean
    public MockContextAwareBean mockContextAwareBean() {
      return new MockContextAwareBean();
    }
  }

  @Autowired
  protected MockContextAwareBean mockContextAwareBean;

  @Test
  public void fooEnigmaAutowired() {
    assertThat(foo).isEqualTo("enigma");
  }

  @Test
  public void mockContextAwareBeanProcessed() {
    assertThat(mockContextAwareBean).isNotNull();
    assertThat(mockContextAwareBean.mockContext).isNotNull();
  }

}

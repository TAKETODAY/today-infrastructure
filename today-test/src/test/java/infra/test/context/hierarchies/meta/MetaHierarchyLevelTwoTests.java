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

package infra.test.context.hierarchies.meta;

import org.junit.jupiter.api.Test;

import infra.beans.factory.annotation.Autowired;
import infra.context.ApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.Profile;
import infra.test.context.ActiveProfiles;
import infra.test.context.ContextConfiguration;
import infra.test.context.aot.DisabledInAotMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 4.0
 */
@ContextConfiguration
@ActiveProfiles("prod")
@DisabledInAotMode // @ContextHierarchy is not supported in AOT.
class MetaHierarchyLevelTwoTests extends MetaHierarchyLevelOneTests {

  @Configuration
  @Profile("prod")
  static class Config {

    @Bean
    String bar() {
      return "Prod Bar";
    }
  }

  @Autowired
  protected ApplicationContext context;

  @Autowired
  private String bar;

  @Test
  void bar() {
    assertThat(bar).isEqualTo("Prod Bar");
  }

  @Test
  void contextHierarchy() {
    assertThat(context).as("child ApplicationContext").isNotNull();
    assertThat(context.getParent()).as("parent ApplicationContext").isNotNull();
    assertThat(context.getParent().getParent()).as("grandparent ApplicationContext").isNull();
  }

}

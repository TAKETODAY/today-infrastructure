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

package infra.test.context.junit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JUnit 4 based integration test which verifies that
 * {@link ContextConfiguration @ContextConfiguration} is optional.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 4.0
 */
@RunWith(InfraRunner.class)
public class OptionalContextConfigurationInfraRunnerTests {

  @Autowired
  String foo;

  @Test
  public void contextConfigurationAnnotationIsOptional() {
    assertThat(foo).isEqualTo("foo");
  }

  @Configuration
  static class Config {

    @Bean
    String foo() {
      return "foo";
    }
  }

}

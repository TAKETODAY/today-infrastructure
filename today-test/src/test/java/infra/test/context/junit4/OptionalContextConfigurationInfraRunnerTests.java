/*
 * Copyright 2017 - 2026 the original author or authors.
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

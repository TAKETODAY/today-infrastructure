/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.test.context.aot.samples.basic;

import infra.beans.factory.annotation.Autowired;
import infra.beans.factory.annotation.Value;
import infra.context.ApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.test.context.TestPropertySource;
import infra.test.context.aot.samples.common.MessageService;
import infra.test.context.junit.jupiter.JUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Uses configuration identical to {@link BasicInfraJupiterTests} and
 * {@link BasicInfraJupiterImportedConfigTests} EXCEPT that this class is
 * annotated with {@link Import @Import} to register an additional bean.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@JUnitConfig(BasicTestConfiguration.class)
@Import(BasicInfraJupiterImportedConfigTests.ImportedConfig.class)
@TestPropertySource(properties = "test.engine = jupiter")
public class BasicInfraJupiterImportedConfigTests {

  @Autowired
  ApplicationContext context;

  @Autowired
  MessageService messageService;

  @Autowired
  String enigma;

  @Value("${test.engine}")
  String testEngine;

  @org.junit.jupiter.api.Test
  void test() {
    assertThat(messageService.generateMessage()).isEqualTo("Hello, AOT!");
    assertThat(enigma).isEqualTo("imported!");
    assertThat(testEngine).isEqualTo("jupiter");
    assertThat(context.getEnvironment().getProperty("test.engine"))
            .as("@TestPropertySource").isEqualTo("jupiter");
  }

  @Configuration(proxyBeanMethods = false)
  static class ImportedConfig {

    @Bean
    String enigma() {
      return "imported!";
    }
  }

}

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

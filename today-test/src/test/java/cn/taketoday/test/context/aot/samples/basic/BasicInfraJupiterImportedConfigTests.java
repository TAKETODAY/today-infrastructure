/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.test.context.aot.samples.basic;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.factory.annotation.Value;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.test.context.TestPropertySource;
import cn.taketoday.test.context.aot.samples.common.MessageService;
import cn.taketoday.test.context.junit.jupiter.JUnitConfig;

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

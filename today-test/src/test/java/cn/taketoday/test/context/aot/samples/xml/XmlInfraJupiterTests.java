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

package cn.taketoday.test.context.aot.samples.xml;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.factory.annotation.Value;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.test.context.TestPropertySource;
import cn.taketoday.test.context.aot.samples.common.MessageService;
import cn.taketoday.test.context.junit.jupiter.JUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 4.0
 */
@JUnitConfig(locations = "test-config.xml")
@TestPropertySource(properties = "test.engine = jupiter")
public class XmlInfraJupiterTests {

  @Autowired
  ApplicationContext context;

  @Autowired
  MessageService messageService;

  @Value("${test.engine}")
  String testEngine;

  @org.junit.jupiter.api.Test
  void test() {
    assertThat(testEngine)
            .as("@Value").isEqualTo("jupiter");
    assertThat(context.getEnvironment().getProperty("test.engine"))
            .as("Environment").isEqualTo("jupiter");

    assertThat(messageService.generateMessage()).isEqualTo("Hello, AOT!");
  }

}

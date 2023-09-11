/*
 * Copyright 2017 - 2023 the original author or authors.
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
import cn.taketoday.test.context.TestPropertySource;
import cn.taketoday.test.context.aot.samples.common.MessageService;
import cn.taketoday.test.context.aot.samples.management.ManagementConfiguration;
import cn.taketoday.test.context.env.YamlTestProperties;
import cn.taketoday.test.context.junit.jupiter.JUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Uses configuration identical to {@link BasicInfraJupiterTests}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@JUnitConfig({ BasicTestConfiguration.class, ManagementConfiguration.class })
@TestPropertySource(properties = "test.engine = jupiter")
// We cannot use `classpath*:` in AOT tests until gh-31088 is resolved.
// @YamlTestProperties("classpath*:**/aot/samples/basic/test?.yaml")
@YamlTestProperties({
        "classpath:cn/taketoday/test/context/aot/samples/basic/test1.yaml",
        "classpath:cn/taketoday/test/context/aot/samples/basic/test2.yaml"
})
public class BasicInfraJupiterSharedConfigTests {

  @Autowired
  ApplicationContext context;

  @Autowired
  MessageService messageService;

  @Value("${test.engine}")
  String testEngine;

  @org.junit.jupiter.api.Test
  void test() {
    assertThat(messageService.generateMessage()).isEqualTo("Hello, AOT!");
    assertThat(testEngine).isEqualTo("jupiter");
    BasicInfraJupiterTests.assertEnvProperties(context);
  }

}

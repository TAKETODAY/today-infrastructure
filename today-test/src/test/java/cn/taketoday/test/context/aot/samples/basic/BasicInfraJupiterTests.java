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

import org.junit.jupiter.api.Nested;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.factory.annotation.Value;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.core.env.Environment;
import cn.taketoday.test.context.ActiveProfiles;
import cn.taketoday.test.context.TestExecutionListeners;
import cn.taketoday.test.context.TestPropertySource;
import cn.taketoday.test.context.aot.samples.common.MessageService;
import cn.taketoday.test.context.aot.samples.management.ManagementConfiguration;
import cn.taketoday.test.context.env.YamlTestProperties;
import cn.taketoday.test.context.junit.jupiter.JUnitConfig;
import cn.taketoday.test.context.support.AbstractTestExecutionListener;

import static cn.taketoday.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 4.0
 */
@JUnitConfig({ BasicTestConfiguration.class, ManagementConfiguration.class })
@TestExecutionListeners(listeners = BasicInfraJupiterTests.DummyTestExecutionListener.class, mergeMode = MERGE_WITH_DEFAULTS)
@TestPropertySource(properties = "test.engine = jupiter")
// We cannot use `classpath*:` in AOT tests until gh-31088 is resolved.
// @YamlTestProperties("classpath*:**/aot/samples/basic/test?.yaml")
@YamlTestProperties({
        "classpath:cn/taketoday/test/context/aot/samples/basic/test1.yaml",
        "classpath:cn/taketoday/test/context/aot/samples/basic/test2.yaml"
})
public class BasicInfraJupiterTests {

  @org.junit.jupiter.api.Test
  void test(@Autowired ApplicationContext context, @Autowired MessageService messageService,
          @Value("${test.engine}") String testEngine) {
    assertThat(messageService.generateMessage()).isEqualTo("Hello, AOT!");
    assertThat(testEngine).isEqualTo("jupiter");
    assertEnvProperties(context);
  }

  @Nested
  @TestPropertySource(properties = "foo=bar")
  @ActiveProfiles(resolver = SpanishActiveProfilesResolver.class)
  public class NestedTests {

    @org.junit.jupiter.api.Test
    void test(@Autowired ApplicationContext context, @Autowired MessageService messageService,
            @Value("${test.engine}") String testEngine, @Value("${foo}") String foo) {
      assertThat(messageService.generateMessage()).isEqualTo("¡Hola, AOT!");
      assertThat(foo).isEqualTo("bar");
      assertThat(testEngine).isEqualTo("jupiter");
      assertEnvProperties(context);
    }

  }

  static void assertEnvProperties(ApplicationContext context) {
    Environment env = context.getEnvironment();
    assertThat(env.getProperty("test.engine")).as("@TestPropertySource").isEqualTo("jupiter");
    assertThat(env.getProperty("test1.prop")).as("@TestPropertySource").isEqualTo("yaml");
    assertThat(env.getProperty("test2.prop")).as("@TestPropertySource").isEqualTo("yaml");
  }

  public static class DummyTestExecutionListener extends AbstractTestExecutionListener {
  }

}


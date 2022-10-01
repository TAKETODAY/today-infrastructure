/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.test.context.junit.jupiter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.platform.testkit.engine.EngineTestKit;

import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.lang.TodayStrategies;
import cn.taketoday.test.context.TestConstructor;

import static cn.taketoday.test.context.TestConstructor.TEST_CONSTRUCTOR_AUTOWIRE_MODE_PROPERTY_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.EventConditions.test;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.instanceOf;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.message;

/**
 * Integration tests for {@link TestConstructor @TestConstructor} support.
 *
 * @author Sam Brannen
 * @since 4.0
 */
class TestConstructorIntegrationTests {

  @BeforeEach
  @AfterEach
  void clearSpringProperty() {
    setProperty(null);
  }

  @Test
  void autowireModeNotSetToAll() {
    EngineTestKit.engine("junit-jupiter")
            .selectors(selectClass(AutomaticallyAutowiredTestCase.class))
            .execute()
            .testEvents()
            .assertStatistics(stats -> stats.started(1).succeeded(0).failed(1))
            .assertThatEvents().haveExactly(1, event(test("test"),
                    finishedWithFailure(
                            instanceOf(ParameterResolutionException.class),
                            message(msg -> msg.matches(".+for parameter \\[java\\.lang\\.String .+\\] in constructor.+")))));
  }

  @Test
  void autowireModeSetToAllViaTodayStrategies() {
    setProperty("all");

    EngineTestKit.engine("junit-jupiter")
            .selectors(selectClass(AutomaticallyAutowiredTestCase.class))
            .execute()
            .testEvents()
            .assertStatistics(stats -> stats.started(1).succeeded(1).failed(0));
  }

  @Test
  void autowireModeSetToAllViaJUnitPlatformConfigurationParameter() {
    EngineTestKit.engine("junit-jupiter")
            .selectors(selectClass(AutomaticallyAutowiredTestCase.class))
            .configurationParameter(TEST_CONSTRUCTOR_AUTOWIRE_MODE_PROPERTY_NAME, "all")
            .execute()
            .testEvents()
            .assertStatistics(stats -> stats.started(1).succeeded(1).failed(0));
  }

  private void setProperty(String flag) {
    TodayStrategies.setProperty(TEST_CONSTRUCTOR_AUTOWIRE_MODE_PROPERTY_NAME, flag);
  }

  @JUnitConfig
  @FailingTestCase
  static class AutomaticallyAutowiredTestCase {

    private final String foo;

    AutomaticallyAutowiredTestCase(String foo) {
      this.foo = foo;
    }

    @Test
    void test() {
      assertThat(foo).isEqualTo("bar");
    }

    @Configuration
    static class Config {

      @Bean
      String foo() {
        return "bar";
      }
    }
  }

}

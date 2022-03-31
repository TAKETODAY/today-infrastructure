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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.testkit.engine.EngineTestKit;

import java.util.concurrent.atomic.AtomicBoolean;

import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Integration tests which verify support for {@link EnabledIf @EnabledIf} in
 * conjunction with {@link DirtiesContext @DirtiesContext} and the
 * {@link ApplicationExtension} in a JUnit Jupiter environment.
 *
 * @author Sam Brannen
 * @see DisabledIfAndDirtiesContextTests
 * @since 5.2.14
 */
class EnabledIfAndDirtiesContextTests {

  private static AtomicBoolean contextClosed = new AtomicBoolean();

  @BeforeEach
  void reset() {
    contextClosed.set(false);
  }

  @Test
  void contextShouldBeClosedForEnabledTestClass() {
    assertThat(contextClosed).as("context closed").isFalse();
    EngineTestKit.engine("junit-jupiter").selectors(
                    selectClass(EnabledAndDirtiesContextTestCase.class))//
            .execute()//
            .testEvents()//
            .assertStatistics(stats -> stats.started(1).succeeded(1).failed(0));
    assertThat(contextClosed).as("context closed").isTrue();
  }

  @Test
  void contextShouldBeClosedForDisabledTestClass() {
    assertThat(contextClosed).as("context closed").isFalse();
    EngineTestKit.engine("junit-jupiter").selectors(
                    selectClass(DisabledAndDirtiesContextTestCase.class))//
            .execute()//
            .testEvents()//
            .assertStatistics(stats -> stats.started(0).succeeded(0).failed(0));
    assertThat(contextClosed).as("context closed").isTrue();
  }

  @JUnitConfig(Config.class)
  @EnabledIf(expression = "true", loadContext = true)
  @DirtiesContext
  static class EnabledAndDirtiesContextTestCase {

    @Test
    void test() {
      /* no-op */
    }
  }

  @JUnitConfig(Config.class)
  @EnabledIf(expression = "false", loadContext = true)
  @DirtiesContext
  static class DisabledAndDirtiesContextTestCase {

    @Test
    void test() {
      fail("This test must be disabled");
    }
  }

  @Configuration
  static class Config {

    @Bean
    DisposableBean disposableBean() {
      return () -> contextClosed.set(true);
    }
  }

}

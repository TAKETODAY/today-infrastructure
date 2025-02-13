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

package infra.test.context.junit.jupiter.parallel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

import java.lang.reflect.Parameter;

import infra.beans.factory.annotation.Autowired;
import infra.context.ApplicationContext;
import infra.context.annotation.Configuration;
import infra.test.context.junit.jupiter.JUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

/**
 * Integration tests which verify that {@code @BeforeEach} and {@code @AfterEach} methods
 * that accept {@code @Autowired} arguments can be executed in parallel without issues
 * regarding concurrent access to the {@linkplain Parameter parameters} of such methods.
 *
 * @author Sam Brannen
 * @since 4.0
 */
class ParallelExecutionInfraExtensionTests {

  private static final int NUM_TESTS = 1000;

  @RepeatedTest(10)
  void runTestsInParallel() {
    Launcher launcher = LauncherFactory.create();
    SummaryGeneratingListener listener = new SummaryGeneratingListener();
    launcher.registerTestExecutionListeners(listener);

    LauncherDiscoveryRequest request = request()//
            .configurationParameter("junit.jupiter.conditions.deactivate", "org.junit.jupiter.engine.extension.DisabledCondition")//
            .configurationParameter("junit.jupiter.execution.parallel.enabled", "true")//
            .configurationParameter("junit.jupiter.execution.parallel.config.dynamic.factor", "10")//
            .selectors(selectClass(TestCase.class))//
            .build();

    launcher.execute(request);

    assertThat(listener.getSummary().getTestsSucceededCount()).as(
            "number of tests executed successfully").isEqualTo(NUM_TESTS);
  }

  @JUnitConfig
  @Disabled
  static class TestCase {

    @BeforeEach
    void beforeEach(@Autowired ApplicationContext context) {
    }

    @RepeatedTest(NUM_TESTS)
    void repeatedTest(@Autowired ApplicationContext context) {
    }

    @AfterEach
    void afterEach(@Autowired ApplicationContext context) {
    }

    @Configuration
    static class Config {
    }
  }

}

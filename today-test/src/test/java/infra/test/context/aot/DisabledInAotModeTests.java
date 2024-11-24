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

package infra.test.context.aot;

import org.junit.jupiter.api.Test;
import org.junit.platform.testkit.engine.EngineTestKit;

import infra.aot.AotDetector;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.testkit.engine.EventConditions.container;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.skippedWithReason;

/**
 * Tests for {@link DisabledInAotMode @DisabledInAotMode}.
 *
 * @author Sam Brannen
 */
class DisabledInAotModeTests {

  @Test
  void defaultDisabledReason() {
    runTestsInAotMode(DefaultReasonTestCase.class, "Disabled in Infra AOT mode");
  }

  @Test
  void customDisabledReason() {
    runTestsInAotMode(CustomReasonTestCase.class, "Disabled in Infra AOT mode ==> @ContextHierarchy is not supported in AOT");
  }

  private static void runTestsInAotMode(Class<?> testClass, String expectedReason) {
    try {
      System.setProperty(AotDetector.AOT_ENABLED, "true");

      EngineTestKit.engine("junit-jupiter")
              .selectors(selectClass(testClass))
              .execute()
              .allEvents()
              .assertThatEvents().haveExactly(1,
                      event(container(testClass.getSimpleName()), skippedWithReason(expectedReason)));
    }
    finally {
      System.clearProperty(AotDetector.AOT_ENABLED);
    }
  }

  @DisabledInAotMode
  static class DefaultReasonTestCase {

    @Test
    void test() {
    }
  }

  @DisabledInAotMode("@ContextHierarchy is not supported in AOT")
  static class CustomReasonTestCase {

    @Test
    void test() {
    }
  }

}

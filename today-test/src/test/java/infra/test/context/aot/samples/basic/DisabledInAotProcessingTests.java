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

import org.junit.jupiter.api.Test;

import infra.aot.AotDetector;
import infra.aot.hint.RuntimeHints;
import infra.beans.factory.annotation.Autowired;
import infra.beans.factory.config.BeanFactoryPostProcessor;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.lang.Assert;
import infra.test.context.TestExecutionListeners;
import infra.test.context.aot.AotTestExecutionListener;
import infra.test.context.aot.DisabledInAotMode;
import infra.test.context.aot.TestContextAotGenerator;
import infra.test.context.junit.jupiter.JUnitConfig;

import static infra.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;
import static infra.test.context.aot.samples.basic.DisabledInAotProcessingTests.BrokenAotTestExecutionListener;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code @DisabledInAotMode} test class which verifies that the application context
 * for the test class is skipped during AOT processing.
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@JUnitConfig
@DisabledInAotMode
@TestExecutionListeners(listeners = BrokenAotTestExecutionListener.class, mergeMode = MERGE_WITH_DEFAULTS)
public class DisabledInAotProcessingTests {

  @Test
  void disabledInAotMode(@Autowired String enigma) {
    assertThat(AotDetector.useGeneratedArtifacts()).as("Should be disabled in AOT mode").isFalse();
    assertThat(enigma).isEqualTo("puzzle");
  }

  @Configuration(proxyBeanMethods = false)
  static class Config {

    @Bean
    String enigma() {
      return "puzzle";
    }

    @Bean
    static BeanFactoryPostProcessor bfppBrokenDuringAotProcessing() {
      boolean runningDuringAotProcessing = StackWalker.getInstance().walk(stream ->
              stream.anyMatch(stackFrame -> stackFrame.getClassName().equals(TestContextAotGenerator.class.getName())));

      return beanFactory -> Assert.state(!runningDuringAotProcessing, "Should not be used during AOT processing");
    }
  }

  static class BrokenAotTestExecutionListener implements AotTestExecutionListener {

    @Override
    public void processAheadOfTime(RuntimeHints runtimeHints, Class<?> testClass, ClassLoader classLoader) {
      throw new UnsupportedOperationException("Broken AotTestExecutionListener");
    }
  }

}

/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

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

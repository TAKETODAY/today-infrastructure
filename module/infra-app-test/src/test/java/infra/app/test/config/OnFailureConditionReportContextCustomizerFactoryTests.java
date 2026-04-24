/*
 * Copyright 2012-present the original author or authors.
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

package infra.app.test.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import infra.app.test.context.InfraTest;
import infra.app.test.system.CapturedOutput;
import infra.app.test.system.OutputCaptureExtension;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.ImportAutoConfiguration;
import infra.context.condition.ConditionalOnProperty;
import infra.test.classpath.resources.WithResource;
import infra.test.context.TestContextManager;
import infra.test.context.cache.ContextCache;
import infra.test.context.cache.DefaultCacheAwareContextLoaderDelegate;
import infra.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link OnFailureConditionReportContextCustomizerFactory}.
 *
 * @author Andy Wilkinson
 */
@ExtendWith(OutputCaptureExtension.class)
class OnFailureConditionReportContextCustomizerFactoryTests {

  @BeforeEach
  void clearCache() {
    ContextCache contextCache = (ContextCache) ReflectionTestUtils
            .getField(DefaultCacheAwareContextLoaderDelegate.class, "defaultContextCache");
    if (contextCache != null) {
      contextCache.reset();
    }
  }

  @Test
  void loadFailureShouldPrintReport(CapturedOutput output) {
    load();
    assertThat(output.getErr()).contains("TestAutoConfiguration matched");
    assertThat(output).contains("Error creating bean with name 'faultyBean'");
  }

  @Test
  @WithResource(name = "application.xml", content = "invalid xml")
  void loadFailureShouldNotPrintReportWhenApplicationPropertiesIsBroken(CapturedOutput output) {
    load();
    assertThat(output).doesNotContain("TestAutoConfiguration matched")
            .doesNotContain("Error creating bean with name 'faultyBean'")
            .contains("java.util.InvalidPropertiesFormatException");
  }

  @Test
  @WithResource(name = "application.properties", content = "infra.test.print-condition-evaluation-report=false")
  void loadFailureShouldNotPrintReportWhenDisabled(CapturedOutput output) {
    load();
    assertThat(output).doesNotContain("TestAutoConfiguration matched")
            .contains("Error creating bean with name 'faultyBean'");
  }

  private void load() {
    assertThatIllegalStateException()
            .isThrownBy(() -> new TestContextManager(FailingTests.class).getTestContext().getApplicationContext());
  }

  @InfraTest
  static class FailingTests {

    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration(TestAutoConfiguration.class)
    static class TestConfig {

      @Bean
      String faultyBean() {
        throw new IllegalStateException();
      }

    }

  }

  @ConditionalOnProperty(value = "com.example.test.enabled", matchIfMissing = true)
  static class TestAutoConfiguration {

  }

}

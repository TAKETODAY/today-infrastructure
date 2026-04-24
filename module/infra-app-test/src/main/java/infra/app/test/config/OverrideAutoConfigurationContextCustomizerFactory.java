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

import org.jspecify.annotations.Nullable;

import java.util.List;

import infra.aot.AotDetector;
import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.config.EnableAutoConfiguration;
import infra.test.context.ContextConfigurationAttributes;
import infra.test.context.ContextCustomizer;
import infra.test.context.ContextCustomizerFactory;
import infra.test.context.MergedContextConfiguration;
import infra.test.context.TestContextAnnotationUtils;
import infra.test.util.TestPropertyValues;

/**
 * {@link ContextCustomizerFactory} to support
 * {@link OverrideAutoConfiguration @OverrideAutoConfiguration}.
 *
 * @author Phillip Webb
 */
class OverrideAutoConfigurationContextCustomizerFactory implements ContextCustomizerFactory {

  @Override
  public @Nullable ContextCustomizer createContextCustomizer(Class<?> testClass,
          List<ContextConfigurationAttributes> configurationAttributes) {
    if (AotDetector.useGeneratedArtifacts()) {
      return null;
    }
    OverrideAutoConfiguration overrideAutoConfiguration = TestContextAnnotationUtils.findMergedAnnotation(testClass,
            OverrideAutoConfiguration.class);
    boolean enabled = (overrideAutoConfiguration == null) || overrideAutoConfiguration.enabled();
    return !enabled ? new DisableAutoConfigurationContextCustomizer() : null;
  }

  /**
   * {@link ContextCustomizer} to disable full auto-configuration.
   */
  private static final class DisableAutoConfigurationContextCustomizer implements ContextCustomizer {

    @Override
    public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
      TestPropertyValues.of(EnableAutoConfiguration.ENABLED_OVERRIDE_PROPERTY + "=false").applyTo(context);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
      return (obj != null) && (obj.getClass() == getClass());
    }

    @Override
    public int hashCode() {
      return getClass().hashCode();
    }

  }

}

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

package infra.test.context.cache;

import org.jspecify.annotations.Nullable;

import java.util.List;

import infra.context.ConfigurableApplicationContext;
import infra.context.support.AbstractApplicationContext;
import infra.test.context.ContextConfigurationAttributes;
import infra.test.context.ContextCustomizer;
import infra.test.context.ContextCustomizerFactory;
import infra.test.context.MergedContextConfiguration;

/**
 * @author Sam Brannen
 * @since 5.0
 */
class DisplayNameCustomizerFactory implements ContextCustomizerFactory {

  @Override
  public ContextCustomizer createContextCustomizer(Class<?> testClass,
          List<ContextConfigurationAttributes> configAttributes) {

    return new DisplayNameCustomizer(testClass.getSimpleName());
  }

  private static class DisplayNameCustomizer implements ContextCustomizer {

    private final String displayName;

    DisplayNameCustomizer(String displayName) {
      this.displayName = displayName;
    }

    @Override
    public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
      ((AbstractApplicationContext) context).setDisplayName(this.displayName);
    }

    @Override
    public boolean equals(@Nullable Object other) {
      return (this == other || (other != null && getClass() == other.getClass()));
    }

    @Override
    public int hashCode() {
      return getClass().hashCode();
    }
  }

}

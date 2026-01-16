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

package infra.web.server.reactive.context;

import org.jspecify.annotations.Nullable;

import infra.aot.AotDetector;
import infra.app.ApplicationContextFactory;
import infra.app.ApplicationType;
import infra.context.ConfigurableApplicationContext;
import infra.core.env.ConfigurableEnvironment;

/**
 * {@link ApplicationContextFactory} registered in {@code today.strategies} to support
 * {@link AnnotationConfigReactiveWebServerApplicationContext} and
 * {@link ReactiveWebServerApplicationContext}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
class ReactiveWebServerApplicationContextFactory implements ApplicationContextFactory {

  @Override
  public @Nullable Class<? extends ConfigurableEnvironment> getEnvironmentType(@Nullable ApplicationType applicationType) {
    return applicationType != ApplicationType.REACTIVE_WEB ? null : ApplicationReactiveWebEnvironment.class;
  }

  @Override
  public @Nullable ConfigurableEnvironment createEnvironment(@Nullable ApplicationType applicationType) {
    return applicationType != ApplicationType.REACTIVE_WEB ? null : new ApplicationReactiveWebEnvironment();
  }

  @Override
  public @Nullable ConfigurableApplicationContext create(@Nullable ApplicationType applicationType) {
    return applicationType != ApplicationType.REACTIVE_WEB ? null : createContext();
  }

  private ConfigurableApplicationContext createContext() {
    if (!AotDetector.useGeneratedArtifacts()) {
      return new AnnotationConfigReactiveWebServerApplicationContext();
    }
    return new ReactiveWebServerApplicationContext();
  }

}

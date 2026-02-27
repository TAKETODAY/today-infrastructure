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

package infra.http.service.config;

import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.Map;

import infra.aot.hint.RuntimeHints;
import infra.aot.hint.RuntimeHintsRegistrar;
import infra.context.annotation.ImportRuntimeHints;
import infra.context.properties.bind.Bindable;
import infra.context.properties.bind.BindableRuntimeHintsRegistrar;
import infra.context.properties.bind.Binder;
import infra.core.env.ConfigurableEnvironment;

/**
 * Properties for HTTP Service clients.
 *
 * @author Olga Maciaszek-Sharma
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
@ImportRuntimeHints(HttpServiceClientProperties.Hints.class)
public class HttpServiceClientProperties {

  private final Map<String, HttpClientProperties> properties;

  HttpServiceClientProperties(Map<String, HttpClientProperties> properties) {
    this.properties = properties;
  }

  /**
   * Return the {@link HttpClientProperties} for the given named client.
   *
   * @param name the service client name
   * @return the properties or {@code null}
   */
  public @Nullable HttpClientProperties get(String name) {
    return this.properties.get(name);
  }

  static HttpServiceClientProperties bind(ConfigurableEnvironment environment) {
    return new HttpServiceClientProperties(Binder.get(environment)
            .bind("http.service-client", Bindable.mapOf(String.class, HttpClientProperties.class))
            .orRequired(Collections.emptyMap()));
  }

  static class Hints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
      BindableRuntimeHintsRegistrar.forTypes(HttpClientProperties.class).registerHints(hints, classLoader);
    }

  }

}

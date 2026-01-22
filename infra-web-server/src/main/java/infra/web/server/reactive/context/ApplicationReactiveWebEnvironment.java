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

import infra.app.Application;
import infra.app.web.context.reactive.StandardReactiveWebEnvironment;
import infra.context.properties.source.ConfigurationPropertySources;
import infra.core.env.ConfigurablePropertyResolver;
import infra.core.env.PropertySources;

/**
 * {@link StandardReactiveWebEnvironment} for typical use in a typical
 * {@link Application}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/21 22:19
 */
class ApplicationReactiveWebEnvironment extends StandardReactiveWebEnvironment {

  @Nullable
  @Override
  protected String doGetActiveProfilesProperty() {
    return null;
  }

  @Nullable
  @Override
  protected String doGetDefaultProfilesProperty() {
    return null;
  }

  @Override
  protected ConfigurablePropertyResolver createPropertyResolver(PropertySources propertySources) {
    return ConfigurationPropertySources.createPropertyResolver(propertySources);
  }

}


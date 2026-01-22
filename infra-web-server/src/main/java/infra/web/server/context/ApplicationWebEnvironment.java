/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web.server.context;

import org.jspecify.annotations.Nullable;

import infra.app.web.context.StandardWebEnvironment;
import infra.context.properties.source.ConfigurationPropertySources;
import infra.core.env.ConfigurableEnvironment;
import infra.core.env.ConfigurablePropertyResolver;
import infra.core.env.PropertySources;

/**
 * Specialization of {@link ConfigurableEnvironment} for netty web application contexts.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/2/4 23:38
 */
class ApplicationWebEnvironment extends StandardWebEnvironment {

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

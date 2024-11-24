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

package infra.app;

import infra.context.properties.source.ConfigurationPropertySources;
import infra.core.env.ConfigurablePropertyResolver;
import infra.core.env.PropertySources;
import infra.core.env.StandardEnvironment;

/**
 * {@link StandardEnvironment} for typical use in a typical {@link Application}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/21 22:19
 */
class ApplicationEnvironment extends StandardEnvironment {

  @Override
  protected String doGetActiveProfilesProperty() {
    return null;
  }

  @Override
  protected String doGetDefaultProfilesProperty() {
    return null;
  }

  @Override
  protected ConfigurablePropertyResolver createPropertyResolver(PropertySources propertySources) {
    return ConfigurationPropertySources.createPropertyResolver(propertySources);
  }

}

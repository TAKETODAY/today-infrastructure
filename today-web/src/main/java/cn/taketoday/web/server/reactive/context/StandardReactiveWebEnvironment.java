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

package cn.taketoday.web.server.reactive.context;

import cn.taketoday.core.env.Environment;
import cn.taketoday.core.env.PropertySources;
import cn.taketoday.core.env.StandardEnvironment;

/**
 * {@link Environment} implementation to be used by {@code Reactive}-based web
 * applications. All web-related (reactive-based) {@code ApplicationContext} classes
 * initialize an instance by default.
 *
 * @author Phillip Webb
 * @since 4.0
 */
public class StandardReactiveWebEnvironment extends StandardEnvironment
        implements ConfigurableReactiveWebEnvironment {

  public StandardReactiveWebEnvironment() {
    super();
  }

  protected StandardReactiveWebEnvironment(PropertySources propertySources) {
    super(propertySources);
  }

}

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

package cn.taketoday.web.servlet;

import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.mock.ServletConfig;
import cn.taketoday.web.mock.ServletContext;

/**
 * Specialization of {@link ConfigurableEnvironment} allowing initialization of
 * servlet-related {@link cn.taketoday.core.env.PropertySource} objects at the
 * earliest moment that the {@link ServletContext} and (optionally) {@link ServletConfig}
 * become available.
 *
 * @author Chris Beams
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/20 17:07
 */
@Deprecated
public interface ConfigurableWebEnvironment extends ConfigurableEnvironment {

  /**
   * Replace any {@linkplain
   * cn.taketoday.core.env.PropertySource.StubPropertySource stub property source}
   * instances acting as placeholders with real servlet context/config property sources
   * using the given parameters.
   *
   * @param servletContext the {@link ServletContext} (may not be {@code null})
   * @param servletConfig the {@link ServletConfig} ({@code null} if not available)
   */
  void initPropertySources(@Nullable ServletContext servletContext, @Nullable ServletConfig servletConfig);

}

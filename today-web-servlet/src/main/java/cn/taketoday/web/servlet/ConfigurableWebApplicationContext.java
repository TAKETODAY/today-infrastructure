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

import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.lang.Nullable;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;

/**
 * Interface to be implemented by configurable web application contexts.
 *
 * <p>Note: The setters of this interface need to be called before an
 * invocation of the {@link #refresh} method inherited from
 * {@link cn.taketoday.context.ConfigurableApplicationContext}.
 * They do not cause an initialization of the context on their own.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/20 17:52
 */
@Deprecated
public interface ConfigurableWebApplicationContext extends WebApplicationContext, ConfigurableApplicationContext {

  /**
   * Name of the ServletConfig environment bean in the factory.
   *
   * @see jakarta.servlet.ServletConfig
   */
  String SERVLET_CONFIG_BEAN_NAME = "servletConfig";

  /**
   * Set the namespace for this web application context,
   * to be used for building a default context config location.
   * The root web application context does not have a namespace.
   */
  void setNamespace(@Nullable String namespace);

  /**
   * Return the namespace for this web application context, if any.
   */
  @Nullable
  String getNamespace();

  /**
   * Set the config locations for this web application context in init-param style,
   * i.e. with distinct locations separated by commas, semicolons or whitespace.
   * <p>If not set, the implementation is supposed to use a default for the
   * given namespace or the root web application context, as appropriate.
   */
  void setConfigLocation(String configLocation);

  /**
   * Set the config locations for this web application context.
   * <p>If not set, the implementation is supposed to use a default for the
   * given namespace or the root web application context, as appropriate.
   */
  void setConfigLocations(String... configLocations);

  /**
   * Return the config locations for this web application context,
   * or {@code null} if none specified.
   */
  @Nullable
  String[] getConfigLocations();

  /**
   * Set the ServletContext for this web application context.
   * <p>Does not cause an initialization of the context: refresh needs to be
   * called after the setting of all configuration properties.
   *
   * @see #refresh()
   */
  void setServletContext(@Nullable ServletContext servletContext);

  /**
   * Set the ServletConfig for this web application context.
   * Only called for a WebApplicationContext that belongs to a specific Servlet.
   *
   * @see #refresh()
   */
  void setServletConfig(@Nullable ServletConfig servletConfig);

  /**
   * Return the ServletConfig for this web application context, if any.
   */
  @Nullable
  ServletConfig getServletConfig();

}

/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.web.context;

import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.WebApplicationContext;

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
public interface ConfigurableWebApplicationContext extends WebApplicationContext, ConfigurableApplicationContext {

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

}

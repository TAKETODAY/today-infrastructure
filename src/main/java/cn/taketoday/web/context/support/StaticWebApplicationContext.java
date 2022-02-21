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

package cn.taketoday.web.context.support;

import cn.taketoday.context.support.StaticApplicationContext;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.context.ConfigurableWebApplicationContext;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/7 13:52
 */
public class StaticWebApplicationContext extends StaticApplicationContext implements ConfigurableWebApplicationContext {

  @Nullable
  private String namespace;
  private String[] configLocations;

  public StaticWebApplicationContext() { }

  @Override
  public String getContextPath() {
    return namespace;
  }

  @Override
  public void setNamespace(@Nullable String namespace) {
    this.namespace = namespace;
  }

  @Override
  @Nullable
  public String getNamespace() {
    return namespace;
  }

  @Override
  public void setConfigLocation(String configLocation) {
    this.configLocations = new String[] { configLocation };
  }

  @Override
  public void setConfigLocations(String... configLocations) {
    this.configLocations = configLocations;
  }

  @Override
  public String[] getConfigLocations() {
    return configLocations;
  }

}

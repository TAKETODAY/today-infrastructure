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

package cn.taketoday.web.servlet.support;

import cn.taketoday.core.env.EnumerablePropertySource;
import cn.taketoday.core.env.PropertySource;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import jakarta.servlet.ServletConfig;

/**
 * {@link PropertySource} that reads init parameters from a {@link ServletConfig} object.
 *
 * @author Chris Beams
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ServletContextPropertySource
 * @since 4.0 2022/2/20 17:10
 */
public class ServletConfigPropertySource extends EnumerablePropertySource<ServletConfig> {

  public ServletConfigPropertySource(String name, ServletConfig servletConfig) {
    super(name, servletConfig);
  }

  @Override
  public String[] getPropertyNames() {
    return CollectionUtils.toArray(source.getInitParameterNames(), Constant.EMPTY_STRING_ARRAY);
  }

  @Override
  @Nullable
  public String getProperty(String name) {
    return this.source.getInitParameter(name);
  }

}


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

package cn.taketoday.web.mock;

import cn.taketoday.core.env.EnumerablePropertySource;
import cn.taketoday.core.env.PropertySource;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.mock.api.MockContext;
import cn.taketoday.util.CollectionUtils;

/**
 * {@link PropertySource} that reads init parameters from a {@link MockContext} object.
 *
 * @author Chris Beams
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ServletConfigPropertySource
 * @since 4.0 2022/2/20 17:10
 */
public class ServletContextPropertySource extends EnumerablePropertySource<MockContext> {

  public ServletContextPropertySource(String name, MockContext mockContext) {
    super(name, mockContext);
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

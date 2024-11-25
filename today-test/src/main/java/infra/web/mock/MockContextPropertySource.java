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

package infra.web.mock;

import infra.core.env.EnumerablePropertySource;
import infra.core.env.PropertySource;
import infra.lang.Constant;
import infra.lang.Nullable;
import infra.mock.api.MockContext;
import infra.util.CollectionUtils;

/**
 * {@link PropertySource} that reads init parameters from a {@link MockContext} object.
 *
 * @author Chris Beams
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see MockConfigPropertySource
 * @since 4.0 2022/2/20 17:10
 */
public class MockContextPropertySource extends EnumerablePropertySource<MockContext> {

  public MockContextPropertySource(String name, MockContext mockContext) {
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
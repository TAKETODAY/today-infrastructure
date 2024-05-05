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

package cn.taketoday.test.web.mock.setup;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.lang.Nullable;
import cn.taketoday.test.web.mock.request.RequestPostProcessor;

/**
 * An empty method implementation of {@link MockMvcConfigurer}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class MockMvcConfigurerAdapter implements MockMvcConfigurer {

  @Override
  public void afterConfigurerAdded(ConfigurableMockMvcBuilder<?> builder) {
  }

  @Override
  @Nullable
  public RequestPostProcessor beforeMockMvcCreated(ConfigurableMockMvcBuilder<?> builder, ApplicationContext cxt) {
    return null;
  }

}

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

package cn.taketoday.test.web.mock.client;

import cn.taketoday.test.web.mock.setup.ConfigurableMockMvcBuilder;
import cn.taketoday.test.web.mock.setup.DefaultMockMvcBuilder;
import cn.taketoday.test.web.mock.setup.MockMvcBuilders;
import cn.taketoday.web.mock.WebApplicationContext;

/**
 * Simple wrapper around a {@link DefaultMockMvcBuilder}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
class ApplicationContextMockMvcSpec extends AbstractMockMvcServerSpec<ApplicationContextMockMvcSpec> {

  private final DefaultMockMvcBuilder mockMvcBuilder;

  public ApplicationContextMockMvcSpec(WebApplicationContext context) {
    this.mockMvcBuilder = MockMvcBuilders.webAppContextSetup(context);
  }

  @Override
  protected ConfigurableMockMvcBuilder<?> getMockMvcBuilder() {
    return this.mockMvcBuilder;
  }

}

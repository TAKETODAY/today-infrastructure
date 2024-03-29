/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.test.web.servlet.client;

import cn.taketoday.test.web.servlet.setup.ConfigurableMockMvcBuilder;
import cn.taketoday.test.web.servlet.setup.DefaultMockMvcBuilder;
import cn.taketoday.test.web.servlet.setup.MockMvcBuilders;
import cn.taketoday.web.servlet.WebApplicationContext;

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

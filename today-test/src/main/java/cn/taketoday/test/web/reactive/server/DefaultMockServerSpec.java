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

package cn.taketoday.test.web.reactive.server;

import cn.taketoday.lang.Assert;
import cn.taketoday.web.server.WebHandler;
import cn.taketoday.web.server.adapter.WebHttpHandlerBuilder;

/**
 * Simple extension of {@link AbstractMockServerSpec} that is given a target
 * {@link WebHandler}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
class DefaultMockServerSpec extends AbstractMockServerSpec<DefaultMockServerSpec> {

  private final WebHandler webHandler;

  DefaultMockServerSpec(WebHandler webHandler) {
    Assert.notNull(webHandler, "WebHandler is required");
    this.webHandler = webHandler;
  }

  @Override
  protected WebHttpHandlerBuilder initHttpHandlerBuilder() {
    return WebHttpHandlerBuilder.webHandler(this.webHandler);
  }

}

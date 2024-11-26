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
package infra.web.handler;

import java.util.List;

import infra.lang.Assert;
import infra.web.HandlerMapping;
import infra.web.RequestContext;

import static java.util.Objects.requireNonNull;

/**
 * Composite HandlerMapping
 *
 * @author TODAY 2019-12-08 23:15
 */
public class HandlerRegistries implements HandlerMapping {

  private final HandlerMapping[] handlerRegistries;

  public HandlerRegistries(HandlerMapping... registries) {
    Assert.notNull(registries, "handler-registries is required");
    this.handlerRegistries = registries;
  }

  public HandlerRegistries(List<HandlerMapping> registries) {
    this(requireNonNull(registries).toArray(new HandlerMapping[registries.size()]));
  }

  @Override
  public Object getHandler(final RequestContext request) throws Exception {
    for (final HandlerMapping registry : handlerRegistries) {
      final Object ret = registry.getHandler(request);
      if (ret != null) {
        return ret;
      }
    }
    return null;
  }

}

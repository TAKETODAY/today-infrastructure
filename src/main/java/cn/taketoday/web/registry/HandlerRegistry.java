/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.web.registry;

import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;

/**
 * For registering handler
 *
 * @author TODAY 2019-12-08 23:06
 */
@FunctionalInterface
public interface HandlerRegistry {

  /**
   * Lookup current request context's handler
   * <p>
   * <b>NOTE</b> : cannot throws any exception
   * </p>
   *
   * @param context Current request context
   * @return Target handler. If returns {@code null} indicates no handler
   */
  @Nullable
  Object lookup(RequestContext context);
}

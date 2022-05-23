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

package cn.taketoday.web;

import cn.taketoday.http.HttpStatus;
import cn.taketoday.web.annotation.ResponseStatus;

/**
 * For {@link HandlerAdapter} not found
 *
 * @author TODAY 2021/4/26 22:26
 * @since 3.0
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class HandlerAdapterNotFoundException extends FrameworkConfigurationException {
  private final Object handler;

  public HandlerAdapterNotFoundException(Object handler) {
    super("No HandlerAdapter for handler: [" + handler + ']');
    this.handler = handler;
  }

  public Object getHandler() {
    return handler;
  }
}

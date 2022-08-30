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

package cn.taketoday.web.service.invoker;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.service.annotation.HttpExchange;

/**
 * Resolve an argument from an {@link HttpExchange @HttpExchange}-annotated method
 * to one or more HTTP request values.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface HttpServiceArgumentResolver {

  /**
   * Resolve the argument value.
   *
   * @param argument the argument value
   * @param parameter the method parameter for the argument
   * @param requestValues builder to add HTTP request values to
   * @return {@code true} if the argument was resolved, {@code false} otherwise
   */
  boolean resolve(@Nullable Object argument, MethodParameter parameter, HttpRequestValues.Builder requestValues);

}

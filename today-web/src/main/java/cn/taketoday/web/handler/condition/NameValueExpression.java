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

package cn.taketoday.web.handler.condition;

import cn.taketoday.lang.Nullable;
import cn.taketoday.web.annotation.RequestMapping;

/**
 * A contract for {@code "name!=value"} style expression used to specify request
 * parameters and request header conditions in {@code @RequestMapping}.
 *
 * @param <T> the value type
 * @author Rossen Stoyanchev
 * @see RequestMapping#params()
 * @see RequestMapping#headers()
 * @since 4.0
 */
public interface NameValueExpression<T> {

  String getName();

  @Nullable
  T getValue();

  boolean isNegated();

}

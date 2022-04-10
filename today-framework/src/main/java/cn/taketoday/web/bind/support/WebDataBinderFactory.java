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

package cn.taketoday.web.bind.support;

import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.bind.WebDataBinder;

/**
 * A factory for creating a {@link WebDataBinder} instance for a named target object.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 3.1
 * @since 4.0 2022/4/8 23:00
 */
public interface WebDataBinderFactory {

  /**
   * Create a {@link WebDataBinder} for the given object.
   *
   * @param webRequest the current request
   * @param target the object to create a data binder for,
   * or {@code null} if creating a binder for a simple type
   * @param objectName the name of the target object
   * @return the created {@link WebDataBinder} instance, never null
   * @throws Throwable raised if the creation and initialization of the data binder fails
   */
  WebDataBinder createBinder(RequestContext webRequest, @Nullable Object target, String objectName)
          throws Throwable;

}


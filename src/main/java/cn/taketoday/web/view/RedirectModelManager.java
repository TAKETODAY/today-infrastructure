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

package cn.taketoday.web.view;

import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;

/**
 * RedirectModel saving strategy
 *
 * @author TODAY 2021/4/2 21:52
 * @since 3.0
 */
public interface RedirectModelManager {

  String BEAN_NAME = "redirectModelManager";

  /**
   * Find a RedirectModel saved by a previous request that matches to the current
   * request, remove it from underlying storage, and also remove other expired
   * RedirectModel instances.
   * <p>This method is invoked in the beginning of every request in contrast
   * to {@link #saveRedirectModel}, which is invoked only when there are
   * flash attributes to be saved - i.e. before a redirect.
   *
   * @param context Current request context
   * @return a RedirectModel matching the current request or {@code null}
   */
  @Nullable
  RedirectModel retrieveAndUpdate(RequestContext context);

  /**
   * Set a {@link RedirectModel} to current request context
   *
   * @param context current request context
   * @param redirectModel value
   */
  void saveRedirectModel(RequestContext context, @Nullable RedirectModel redirectModel);

}

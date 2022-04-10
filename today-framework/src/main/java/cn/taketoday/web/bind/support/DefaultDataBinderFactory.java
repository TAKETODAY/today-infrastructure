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
import cn.taketoday.web.bind.RequestContextDataBinder;
import cn.taketoday.web.bind.WebDataBinder;

/**
 * Create a {@link RequestContextDataBinder} instance and initialize it with a
 * {@link WebBindingInitializer}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class DefaultDataBinderFactory implements WebDataBinderFactory {

  @Nullable
  private final WebBindingInitializer initializer;

  /**
   * Create a new {@code DefaultDataBinderFactory} instance.
   *
   * @param initializer for global data binder initialization
   * (or {@code null} if none)
   */
  public DefaultDataBinderFactory(@Nullable WebBindingInitializer initializer) {
    this.initializer = initializer;
  }

  /**
   * Create a new {@link WebDataBinder} for the given target object and
   * initialize it through a {@link WebBindingInitializer}.
   *
   * @throws Exception in case of invalid state or arguments
   */
  @Override
  public final WebDataBinder createBinder(
          RequestContext request, @Nullable Object target, String objectName) throws Throwable {

    WebDataBinder dataBinder = createBinderInstance(target, objectName, request);
    if (initializer != null) {
      initializer.initBinder(dataBinder);
    }
    initBinder(dataBinder, request);
    return dataBinder;
  }

  /**
   * Extension point to create the WebDataBinder instance.
   * By default this is {@code RequestContextDataBinder}.
   *
   * @param target the binding target or {@code null} for type conversion only
   * @param objectName the binding target object name
   * @param request the current request
   * @throws Exception in case of invalid state or arguments
   */
  protected WebDataBinder createBinderInstance(
          @Nullable Object target, String objectName, RequestContext request) throws Exception {

    return new RequestContextDataBinder(target, objectName);
  }

  /**
   * Extension point to further initialize the created data binder instance
   * (e.g. with {@code @InitBinder} methods) after "global" initialization
   * via {@link WebBindingInitializer}.
   *
   * @param dataBinder the data binder instance to customize
   * @param request the current request
   * @throws Exception if initialization fails
   */
  protected void initBinder(WebDataBinder dataBinder, RequestContext request)
          throws Throwable {

  }

}

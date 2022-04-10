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

package cn.taketoday.web.handler.method;

import java.util.List;

import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.bind.ServletRequestDataBinder;
import cn.taketoday.web.bind.support.WebBindingInitializer;

/**
 * Creates a {@code ServletRequestDataBinder}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/8 23:16
 */
public class ServletRequestDataBinderFactory extends InitBinderDataBinderFactory {

  /**
   * Create a new instance.
   *
   * @param binderMethods one or more {@code @InitBinder} methods
   * @param initializer provides global data binder initialization
   */
  public ServletRequestDataBinderFactory(@Nullable List<InvocableHandlerMethod> binderMethods,
          @Nullable WebBindingInitializer initializer) {

    super(binderMethods, initializer);
  }

  /**
   * Returns an instance of {@link ServletRequestDataBinder}.
   */
  @Override
  protected ServletRequestDataBinder createBinderInstance(
          @Nullable Object target, String objectName, RequestContext request) throws Exception {

    return new ServletRequestDataBinder(target, objectName);
  }

}

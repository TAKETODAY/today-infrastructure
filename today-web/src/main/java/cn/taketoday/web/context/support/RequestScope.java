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

package cn.taketoday.web.context.support;

import java.util.function.Supplier;

import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextHolder;
import cn.taketoday.web.RequestContextUtils;

/**
 * RequestScope beans just available in current {@link RequestContext}
 *
 * <p>
 * like spring's RequestScope
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/19 21:40
 */
public class RequestScope extends AbstractRequestContextScope<RequestContext> {

  @Override
  public Object get(String beanName, Supplier<?> objectFactory) {
    RequestContext context = RequestContextHolder.getRequired();
    return doGetBean(context, beanName, objectFactory);
  }

  @Override
  public Object remove(String name) {
    RequestContext context = RequestContextHolder.getRequired();
    return remove(context, name);
  }

  @Override
  protected void setAttribute(RequestContext context, String beanName, Object scopedObject) {
    context.setAttribute(beanName, scopedObject);
  }

  @Override
  protected Object getAttribute(String beanName, RequestContext context) {
    return context.getAttribute(beanName);
  }

  @Override
  protected void removeAttribute(RequestContext context, String name) {
    context.removeAttribute(name);
  }

  @Nullable
  @Override
  public Object resolveContextualObject(String key) {
    if (RequestContext.SCOPE_REQUEST.equals(key)) {
      return RequestContextHolder.get();
    }
    else if (RequestContext.SCOPE_SESSION.equals(key)) {
      var request = RequestContextHolder.get();
      if (request != null) {
        return RequestContextUtils.getSession(request, true);
      }
    }
    return null;
  }

  @Override
  public void registerDestructionCallback(String name, Runnable callback) {
    var request = RequestContextHolder.getRequired();
    request.registerRequestDestructionCallback(name, callback);
  }

}

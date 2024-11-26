/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.web.context.support;

import java.util.function.Supplier;

import infra.lang.Nullable;
import infra.web.RequestContext;
import infra.web.RequestContextHolder;
import infra.web.RequestContextUtils;

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

  public static final RequestScope instance = new RequestScope();

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
  protected Object getAttribute(RequestContext context, String beanName) {
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
    request.registerDestructionCallback(name, callback);
  }

}

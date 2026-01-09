/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.context.support;

import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

import infra.web.RequestContext;
import infra.web.RequestContextHolder;
import infra.web.RequestContextUtils;

/**
 * RequestScope beans just available in current {@link RequestContext}
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/19 21:40
 */
public class RequestScope extends AbstractWebContextScope<RequestContext> {

  public static final RequestScope instance = new RequestScope();

  @Override
  public Object get(String beanName, Supplier<?> objectFactory) {
    RequestContext context = RequestContextHolder.getRequired();
    return doGetBean(context, beanName, objectFactory);
  }

  @Nullable
  @Override
  public Object remove(String name) {
    RequestContext context = RequestContextHolder.getRequired();
    return remove(context, name);
  }

  @Override
  protected void setAttribute(RequestContext context, String beanName, Object scopedObject) {
    context.setAttribute(beanName, scopedObject);
  }

  @Nullable
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

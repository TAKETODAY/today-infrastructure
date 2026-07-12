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

import infra.web.HttpContext;
import infra.web.HttpContextHolder;

/**
 * RequestScope beans just available in current {@link HttpContext}
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/19 21:40
 */
public class RequestScope extends AbstractWebContextScope<HttpContext> {

  public static final RequestScope instance = new RequestScope();

  @Override
  public Object get(String beanName, Supplier<?> objectFactory) {
    HttpContext context = HttpContextHolder.required();
    return doGetBean(context, beanName, objectFactory);
  }

  @Override
  public @Nullable Object remove(String name) {
    HttpContext context = HttpContextHolder.required();
    return remove(context, name);
  }

  @Override
  protected void setAttribute(HttpContext context, String beanName, Object scopedObject) {
    context.setAttribute(beanName, scopedObject);
  }

  @Override
  protected @Nullable Object getAttribute(HttpContext context, String beanName) {
    return context.getAttribute(beanName);
  }

  @Override
  protected void removeAttribute(HttpContext context, String name) {
    context.removeAttribute(name);
  }

  @Override
  public @Nullable Object resolveContextualObject(String key) {
    if (HttpContext.SCOPE_REQUEST.equals(key)) {
      return HttpContextHolder.current();
    }
    else if (HttpContext.SCOPE_SESSION.equals(key)) {
      var request = HttpContextHolder.current();
      if (request != null) {
        return request.getSession(true);
      }
    }
    return null;
  }

  @Override
  public void registerDestructionCallback(String name, Runnable callback) {
    var request = HttpContextHolder.required();
    request.registerCallback(HttpContext.Lifecycle.COMPLETED, name, callback);
  }

}

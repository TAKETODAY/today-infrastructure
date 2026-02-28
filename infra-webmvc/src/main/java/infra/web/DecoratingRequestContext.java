/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web;

import infra.context.ApplicationContext;

/**
 * A {@link RequestContext} implementation that decorates another {@link RequestContext}.
 *
 * <p>This class provides a base for request context decorators that wrap an existing
 * {@link RequestContext} instance and potentially enhance or modify its behavior.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/6/13 23:38
 */
public class DecoratingRequestContext extends DecorableRequestContext {

  protected final RequestContext delegate;

  public DecoratingRequestContext(RequestContext delegate) {
    this.delegate = delegate;
  }

  public DecoratingRequestContext(RequestContext delegate, ApplicationContext context, DispatcherHandler dispatcherHandler) {
    super(context, dispatcherHandler);
    this.delegate = delegate;
  }

  @Override
  public final RequestContext delegate() {
    return delegate;
  }

}

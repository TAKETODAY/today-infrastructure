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

package infra.web;

import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

import infra.context.ApplicationContext;
import infra.lang.Assert;

/**
 * RequestContext decorator that makes all beans in a
 * given WebApplicationContext accessible as request attributes,
 * through lazy checking once an attribute gets accessed.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/5 14:27
 */
public class ContextExposingRequestContext extends RequestContextDecorator {

  private final ApplicationContext webApplicationContext;

  @Nullable
  private final Set<String> exposedContextBeanNames;

  @Nullable
  private Set<String> explicitAttributes;

  /**
   * Create a new ContextExposingRequestContext for the given request.
   *
   * @param originalRequest the original RequestContext
   * @param context the WebApplicationContext that this request runs in
   * @param exposedContextBeanNames the names of beans in the context which
   * are supposed to be exposed (if this is non-null, only the beans in this
   * Set are eligible for exposure as attributes)
   */
  public ContextExposingRequestContext(RequestContext originalRequest,
          ApplicationContext context, @Nullable Set<String> exposedContextBeanNames) {
    super(originalRequest);
    Assert.notNull(context, "WebApplicationContext is required");
    this.webApplicationContext = context;
    this.exposedContextBeanNames = exposedContextBeanNames;
  }

  /**
   * Return the WebApplicationContext that this request runs in.
   */
  @Override
  public ApplicationContext getApplicationContext() {
    return this.webApplicationContext;
  }

  @Override
  @Nullable
  public Object getAttribute(String name) {
    if ((explicitAttributes == null || !explicitAttributes.contains(name))
            && (exposedContextBeanNames == null || exposedContextBeanNames.contains(name))
            && webApplicationContext.containsBean(name)) {
      return webApplicationContext.getBean(name);
    }
    else {
      return super.getAttribute(name);
    }
  }

  @Override
  public void setAttribute(String name, @Nullable Object value) {
    super.setAttribute(name, value);
    if (this.explicitAttributes == null) {
      this.explicitAttributes = new HashSet<>(8);
    }
    this.explicitAttributes.add(name);
  }

}

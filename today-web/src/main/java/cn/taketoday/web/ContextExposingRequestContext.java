/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.web;

import java.io.Serial;
import java.util.HashSet;
import java.util.Set;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * RequestContext decorator that makes all beans in a
 * given WebApplicationContext accessible as request attributes,
 * through lazy checking once an attribute gets accessed.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/5 14:27
 */
public class ContextExposingRequestContext extends RequestContextDecorator {

  @Serial
  private static final long serialVersionUID = 1L;

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
   */
  public ContextExposingRequestContext(RequestContext originalRequest, ApplicationContext context) {
    this(originalRequest, context, null);
  }

  /**
   * Create a new ContextExposingRequestContext for the given request.
   *
   * @param originalRequest the original RequestContext
   * @param context the WebApplicationContext that this request runs in
   * @param exposedContextBeanNames the names of beans in the context which
   * are supposed to be exposed (if this is non-null, only the beans in this
   * Set are eligible for exposure as attributes)
   */
  public ContextExposingRequestContext(
          RequestContext originalRequest, ApplicationContext context,
          @Nullable Set<String> exposedContextBeanNames) {
    super(originalRequest);
    Assert.notNull(context, "WebApplicationContext is required");
    this.webApplicationContext = context;
    this.exposedContextBeanNames = exposedContextBeanNames;
  }

  /**
   * Return the WebApplicationContext that this request runs in.
   */
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
  public void setAttribute(String name, Object value) {
    super.setAttribute(name, value);
    if (this.explicitAttributes == null) {
      this.explicitAttributes = new HashSet<>(8);
    }
    this.explicitAttributes.add(name);
  }

}

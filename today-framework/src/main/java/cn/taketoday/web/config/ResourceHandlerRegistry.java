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

package cn.taketoday.web.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.beans.factory.BeanInitializationException;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.core.Ordered;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.accept.ContentNegotiationManager;
import cn.taketoday.web.handler.RequestHandler;
import cn.taketoday.web.registry.AbstractHandlerRegistry;
import cn.taketoday.web.registry.HandlerRegistry;
import cn.taketoday.web.registry.SimpleUrlHandlerRegistry;
import cn.taketoday.web.resource.ResourceHttpRequestHandler;

/**
 * Stores registrations of resource handlers for serving static resources such
 * as images, css files and others through Framework MVC including setting cache
 * headers optimized for efficient loading in a web browser. Resources can be
 * served out of locations under web application root, from the classpath, and
 * others.
 *
 * <p>To create a resource handler, use {@link #addResourceHandler(String...)}
 * providing the URL path patterns for which the handler should be invoked to
 * serve static resources (e.g. {@code "/resources/**"}).
 *
 * <p>Then use additional methods on the returned
 * {@link ResourceHandlerRegistration} to add one or more locations from which
 * to serve static content from (e.g. {{@code "/"},
 * {@code "classpath:/META-INF/public-web-resources/"}}) or to specify a cache
 * period for served resources.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see DefaultServletHandlerConfigurer
 * @since 4.0 2022/2/15 17:07
 */
public class ResourceHandlerRegistry {

  private final ApplicationContext applicationContext;

  @Nullable
  private final ContentNegotiationManager contentNegotiationManager;

  private final List<ResourceHandlerRegistration> registrations = new ArrayList<>();

  private int order = Ordered.LOWEST_PRECEDENCE - 1;

  /**
   * Create a new resource handler registry for the given application context.
   *
   * @param applicationContext the application context
   */
  public ResourceHandlerRegistry(ApplicationContext applicationContext) {
    this(applicationContext, null);
  }

  /**
   * Create a new resource handler registry for the given application context.
   *
   * @param applicationContext the application context
   * @param contentNegotiationManager the content negotiation manager to use
   */
  public ResourceHandlerRegistry(
          ApplicationContext applicationContext, @Nullable ContentNegotiationManager contentNegotiationManager) {
    Assert.notNull(applicationContext, "ApplicationContext is required");
    this.applicationContext = applicationContext;
    this.contentNegotiationManager = contentNegotiationManager;
  }

  /**
   * Add a resource handler to serve static resources. The handler is invoked
   * for requests that match one of the specified URL path patterns.
   * <p>Patterns such as {@code "/static/**"} or {@code "/css/{filename:\\w+\\.css}"}
   * are supported.
   */
  public ResourceHandlerRegistration addResourceHandler(String... pathPatterns) {
    ResourceHandlerRegistration registration = new ResourceHandlerRegistration(pathPatterns);
    this.registrations.add(registration);
    return registration;
  }

  /**
   * Whether a resource handler has already been registered for the given path pattern.
   */
  public boolean hasMappingForPattern(String pathPattern) {
    for (ResourceHandlerRegistration registration : this.registrations) {
      if (Arrays.asList(registration.getPathPatterns()).contains(pathPattern)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Specify the order to use for resource handling relative to other {@link HandlerRegistry HandlerRegistries}
   * configured in the MVC application context.
   * <p>The default value used is {@code Integer.MAX_VALUE-1}.
   */
  public ResourceHandlerRegistry setOrder(int order) {
    this.order = order;
    return this;
  }

  /**
   * Return a handler mapping with the mapped resource handlers; or {@code null} in case
   * of no registrations.
   */
  @Nullable
  protected AbstractHandlerRegistry getHandlerRegistry() {
    if (this.registrations.isEmpty()) {
      return null;
    }
    Map<String, RequestHandler> urlMap = new LinkedHashMap<>();
    for (ResourceHandlerRegistration registration : this.registrations) {
      ResourceHttpRequestHandler handler = getRequestHandler(registration);
      for (String pathPattern : registration.getPathPatterns()) {
        urlMap.put(pathPattern, handler);
      }
    }
    return new SimpleUrlHandlerRegistry(urlMap, this.order);
  }

  private ResourceHttpRequestHandler getRequestHandler(ResourceHandlerRegistration registration) {
    ResourceHttpRequestHandler handler = registration.getRequestHandler();
    if (this.contentNegotiationManager != null) {
      handler.setContentNegotiationManager(this.contentNegotiationManager);
    }
    handler.setApplicationContext(this.applicationContext);
    try {
      handler.afterPropertiesSet();
    }
    catch (Throwable ex) {
      throw new BeanInitializationException("Failed to init ResourceHttpRequestHandler", ex);
    }
    return handler;
  }

}

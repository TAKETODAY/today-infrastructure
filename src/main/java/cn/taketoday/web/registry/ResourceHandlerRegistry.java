/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.web.registry;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.beans.Autowired;
import cn.taketoday.cache.ConcurrentMapCache;
import cn.taketoday.core.NonNull;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.config.WebApplicationInitializer;
import cn.taketoday.web.handler.PatternHandler;
import cn.taketoday.web.handler.ResourceMapping;
import cn.taketoday.web.handler.ResourceMatchResult;
import cn.taketoday.web.handler.ResourceRequestHandler;
import cn.taketoday.web.interceptor.HandlerInterceptor;
import cn.taketoday.web.resource.DefaultResourceResolver;
import cn.taketoday.web.resource.WebResourceResolver;

import static cn.taketoday.core.ConfigurationException.nonNull;

/**
 * @author TODAY <br>
 * 2019-05-15 21:34
 * @since 2.3.7
 */
public class ResourceHandlerRegistry
        extends MappedHandlerRegistry implements WebApplicationInitializer {

  private int contextPathLength;
  private WebResourceResolver resourceResolver;
  private final ArrayList<ResourceMapping> resourceMappings = new ArrayList<>();

  public ResourceHandlerRegistry() {
    this(new DefaultResourceResolver());
  }

  public ResourceHandlerRegistry(@Autowired(required = false) WebResourceResolver resourceResolver) {
    setResourceResolver(nonNull(resourceResolver, "web resource resolver must not be null"));
  }

  public ResourceMapping addResourceMapping(String... pathPatterns) {
    ResourceMapping resourceMapping = new ResourceMapping(null, pathPatterns);
    getResourceMappings().add(resourceMapping);
    return resourceMapping;
  }

  @SafeVarargs
  public final <T extends HandlerInterceptor> ResourceMapping addResourceMapping(Class<T>... handlerInterceptors) {
    final HandlerMethodRegistry registry = obtainApplicationContext().getBean(HandlerMethodRegistry.class);

    final HandlerInterceptor[] interceptors = registry.getInterceptors(handlerInterceptors);
    ResourceMapping resourceMapping = new ResourceMapping(ObjectUtils.isEmpty(interceptors) ? null : interceptors);
    getResourceMappings().add(resourceMapping);
    return resourceMapping;
  }

  public boolean isEmpty() {
    return resourceMappings.isEmpty();
  }

  public List<ResourceMapping> getResourceMappings() {
    return resourceMappings;
  }

  @Override
  protected String computeKey(RequestContext context) {
    return requestPath(context.getRequestPath(), contextPathLength);
  }

  @Override
  protected Object lookupHandler(final String handlerKey, final RequestContext context) {
    final Object handler = super.lookupHandler(handlerKey, context);
    if (handler instanceof ResourceMatchResult) {
      context.setAttribute(ResourceMatchResult.RESOURCE_MATCH_RESULT, handler);
      return ((ResourceMatchResult) handler).getHandler();
    }
    else if (handler instanceof ResourceRequestHandler) {
      context.setAttribute(
              ResourceMatchResult.RESOURCE_MATCH_RESULT,
              new ResourceMatchResult(handlerKey,
                                      handlerKey,
                                      getPathMatcher(),
                                      ((ResourceRequestHandler) handler)
              )
      );
    }
    return handler;
  }

  @Override
  protected Object lookupPatternHandler(final String handlerKey, final RequestContext context) {
    final PatternHandler matched = matchingPatternHandler(handlerKey);
    if (matched != null) {
      return new ResourceMatchResult(handlerKey,
                                     matched.getPattern(),
                                     getPathMatcher(),
                                     (ResourceRequestHandler) matched.getHandler()
      );
    }
    return null;
  }

  @NonNull
  @Override
  protected ConcurrentMapCache createPatternMatchingCache() {
    return new ConcurrentMapCache(CACHE_NAME, 64);
  }

  /**
   * Get request path
   *
   * @param requestURI
   *         Current requestURI
   * @param length
   *         context path length
   *
   * @return Decoded request path
   */
  protected String requestPath(final String requestURI, final int length) {
    return StringUtils.decodeUrl(length == 0 ? requestURI : requestURI.substring(length));
  }

  public WebResourceResolver getResourceResolver() {
    return resourceResolver;
  }

  public void setResourceResolver(WebResourceResolver resourceResolver) {
    this.resourceResolver = resourceResolver;
  }

  @Override
  public void onStartup(WebApplicationContext context) throws Throwable {
    this.contextPathLength = context.getContextPath().length();
    final WebResourceResolver resourceResolver = getResourceResolver();
    final List<ResourceMapping> resourceMappings = getResourceMappings();

    AnnotationAwareOrderComparator.sort(resourceMappings);

    for (final ResourceMapping resourceMapping : resourceMappings) {
      final String[] pathPatterns = resourceMapping.getPathPatterns();
      registerHandler(new ResourceRequestHandler(resourceMapping, resourceResolver), pathPatterns);
    }
    // @since 4.0 trimToSize
    CollectionUtils.trimToSize(patternHandlers);
    CollectionUtils.trimToSize(this.resourceMappings);
  }

}

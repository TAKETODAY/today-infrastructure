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

package cn.taketoday.web.resource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.beans.BeansException;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.aware.ApplicationContextAware;
import cn.taketoday.context.event.ApplicationListener;
import cn.taketoday.context.event.ContextRefreshedEvent;
import cn.taketoday.core.AntPathMatcher;
import cn.taketoday.core.PathMatcher;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.registry.SimpleUrlHandlerRegistry;
import jakarta.servlet.http.HttpServletRequest;

/**
 * A central component to use to obtain the public URL path that clients should
 * use to access a static resource.
 *
 * <p>This class is aware of Spring MVC handler mappings used to serve static
 * resources and uses the {@code ResourceResolver} chains of the configured
 * {@code ResourceHttpRequestHandler}s to make its decisions.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 4.0
 */
public class ResourceUrlProvider implements ApplicationListener<ContextRefreshedEvent>, ApplicationContextAware {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  @Nullable
  private ApplicationContext applicationContext;

  private PathMatcher pathMatcher = new AntPathMatcher();

  private final Map<String, ResourceHttpRequestHandler> handlerMap = new LinkedHashMap<>();

  private boolean autodetect = true;

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  /**
   * Configure a {@code PathMatcher} to use when comparing target lookup path
   * against resource mappings.
   */
  public void setPathMatcher(PathMatcher pathMatcher) {
    this.pathMatcher = pathMatcher;
  }

  /**
   * Return the configured {@code PathMatcher}.
   */
  public PathMatcher getPathMatcher() {
    return this.pathMatcher;
  }

  /**
   * Manually configure the resource mappings.
   * <p><strong>Note:</strong> by default resource mappings are auto-detected
   * from the Spring {@code ApplicationContext}. However if this property is
   * used, the auto-detection is turned off.
   */
  public void setHandlerMap(@Nullable Map<String, ResourceHttpRequestHandler> handlerMap) {
    if (handlerMap != null) {
      this.handlerMap.clear();
      this.handlerMap.putAll(handlerMap);
      this.autodetect = false;
    }
  }

  /**
   * Return the resource mappings, either manually configured or auto-detected
   * when the Spring {@code ApplicationContext} is refreshed.
   */
  public Map<String, ResourceHttpRequestHandler> getHandlerMap() {
    return this.handlerMap;
  }

  /**
   * Return {@code false} if resource mappings were manually configured,
   * {@code true} otherwise.
   */
  public boolean isAutodetect() {
    return this.autodetect;
  }

  @Override
  public void onApplicationEvent(ContextRefreshedEvent event) {
    if (event.getApplicationContext() == this.applicationContext && isAutodetect()) {
      this.handlerMap.clear();
      detectResourceHandlers(this.applicationContext);
      if (!this.handlerMap.isEmpty()) {
        this.autodetect = false;
      }
    }
  }

  protected void detectResourceHandlers(ApplicationContext appContext) {
    Map<String, SimpleUrlHandlerRegistry> beans = appContext.getBeansOfType(SimpleUrlHandlerRegistry.class);
    ArrayList<SimpleUrlHandlerRegistry> mappings = new ArrayList<>(beans.values());
    AnnotationAwareOrderComparator.sort(mappings);

    for (SimpleUrlHandlerRegistry mapping : mappings) {
      for (String pattern : mapping.getHandlerMap().keySet()) {
        Object handler = mapping.getHandlerMap().get(pattern);
        if (handler instanceof ResourceHttpRequestHandler resourceHandler) {
          this.handlerMap.put(pattern, resourceHandler);
        }
      }
    }

    if (this.handlerMap.isEmpty()) {
      logger.trace("No resource handling mappings found");
    }
  }

  /**
   * A variation on {@link #getForLookupPath(String)} that accepts a full request
   * URL path (i.e. including context and servlet path) and returns the full request
   * URL path to expose for public use.
   *
   * @param request the current request
   * @param requestUrl the request URL path to resolve
   * @return the resolved public URL path, or {@code null} if unresolved
   */
  @Nullable
  public final String getForRequestUrl(HttpServletRequest request, String requestUrl) {
    int prefixIndex = getLookupPathIndex(request);
    int suffixIndex = getEndPathIndex(requestUrl);
    if (prefixIndex >= suffixIndex) {
      return null;
    }
    String prefix = requestUrl.substring(0, prefixIndex);
    String suffix = requestUrl.substring(suffixIndex);
    String lookupPath = requestUrl.substring(prefixIndex, suffixIndex);
    String resolvedLookupPath = getForLookupPath(lookupPath);
    return (resolvedLookupPath != null ? prefix + resolvedLookupPath + suffix : null);
  }

  private int getLookupPathIndex(HttpServletRequest request) {
    UrlPathHelper pathHelper = getUrlPathHelper();
    if (request.getAttribute(UrlPathHelper.PATH_ATTRIBUTE) == null) {
      pathHelper.resolveAndCacheLookupPath(request);
    }
    String requestUri = pathHelper.getRequestUri(request);
    String lookupPath = UrlPathHelper.getResolvedLookupPath(request);
    return requestUri.indexOf(lookupPath);
  }

  private int getEndPathIndex(String lookupPath) {
    int suffixIndex = lookupPath.length();
    int queryIndex = lookupPath.indexOf('?');
    if (queryIndex > 0) {
      suffixIndex = queryIndex;
    }
    int hashIndex = lookupPath.indexOf('#');
    if (hashIndex > 0) {
      suffixIndex = Math.min(suffixIndex, hashIndex);
    }
    return suffixIndex;
  }

  /**
   * Compare the given path against configured resource handler mappings and
   * if a match is found use the {@code ResourceResolver} chain of the matched
   * {@code ResourceHttpRequestHandler} to resolve the URL path to expose for
   * public use.
   * <p>It is expected that the given path is what Spring MVC would use for
   * request mapping purposes, i.e. excluding context and servlet path portions.
   * <p>If several handler mappings match, the handler used will be the one
   * configured with the most specific pattern.
   *
   * @param lookupPath the lookup path to check
   * @return the resolved public URL path, or {@code null} if unresolved
   */
  @Nullable
  public final String getForLookupPath(String lookupPath) {
    // Clean duplicate slashes or pathWithinPattern won't match lookupPath
    String previous;
    do {
      previous = lookupPath;
      lookupPath = StringUtils.replace(lookupPath, "//", "/");
    }
    while (!lookupPath.equals(previous));

    List<String> matchingPatterns = new ArrayList<>();
    for (String pattern : this.handlerMap.keySet()) {
      if (getPathMatcher().match(pattern, lookupPath)) {
        matchingPatterns.add(pattern);
      }
    }

    if (!matchingPatterns.isEmpty()) {
      Comparator<String> patternComparator = getPathMatcher().getPatternComparator(lookupPath);
      matchingPatterns.sort(patternComparator);
      for (String pattern : matchingPatterns) {
        String pathWithinMapping = getPathMatcher().extractPathWithinPattern(pattern, lookupPath);
        String pathMapping = lookupPath.substring(0, lookupPath.indexOf(pathWithinMapping));
        ResourceHttpRequestHandler handler = this.handlerMap.get(pattern);
        ResourceResolverChain chain = new DefaultResourceResolverChain(handler.getResourceResolvers());
        String resolved = chain.resolveUrlPath(pathWithinMapping, handler.getLocations());
        if (resolved == null) {
          continue;
        }
        return pathMapping + resolved;
      }
    }

    if (logger.isTraceEnabled()) {
      logger.trace("No match for \"{}\"", lookupPath);
    }

    return null;
  }

}

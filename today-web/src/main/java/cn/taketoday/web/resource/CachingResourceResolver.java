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

package cn.taketoday.web.resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import cn.taketoday.cache.Cache;
import cn.taketoday.cache.CacheManager;
import cn.taketoday.core.io.Resource;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContext;

/**
 * A {@link cn.taketoday.web.resource.ResourceResolver} that
 * resolves resources from a {@link Cache} or otherwise
 * delegates to the resolver chain and saves the result in the cache.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class CachingResourceResolver extends AbstractResourceResolver {

  /**
   * The prefix used for resolved resource cache keys.
   */
  public static final String RESOLVED_RESOURCE_CACHE_KEY_PREFIX = "resolvedResource:";

  /**
   * The prefix used for resolved URL path cache keys.
   */
  public static final String RESOLVED_URL_PATH_CACHE_KEY_PREFIX = "resolvedUrlPath:";

  private final Cache cache;

  private final List<String> contentCodings = new ArrayList<>(EncodedResourceResolver.DEFAULT_CODINGS);

  public CachingResourceResolver(Cache cache) {
    Assert.notNull(cache, "Cache is required");
    this.cache = cache;
  }

  public CachingResourceResolver(CacheManager cacheManager, String cacheName) {
    Cache cache = cacheManager.getCache(cacheName);
    if (cache == null) {
      throw new IllegalArgumentException("Cache '%s' not found".formatted(cacheName));
    }
    this.cache = cache;
  }

  /**
   * Return the configured {@code Cache}.
   */
  public Cache getCache() {
    return this.cache;
  }

  /**
   * Configure the supported content codings from the
   * {@literal "Accept-Encoding"} header for which to cache resource variations.
   * <p>The codings configured here are generally expected to match those
   * configured on {@link EncodedResourceResolver#setContentCodings(List)}.
   * <p>By default this property is set to {@literal ["br", "gzip"]} based on
   * the value of {@link EncodedResourceResolver#DEFAULT_CODINGS}.
   *
   * @param codings one or more supported content codings
   */
  public void setContentCodings(List<String> codings) {
    Assert.notEmpty(codings, "At least one content coding expected");
    this.contentCodings.clear();
    this.contentCodings.addAll(codings);
  }

  /**
   * Return a read-only list with the supported content codings.
   */
  public List<String> getContentCodings() {
    return Collections.unmodifiableList(this.contentCodings);
  }

  @Override
  protected Resource resolveResourceInternal(@Nullable RequestContext request,
          String requestPath, List<? extends Resource> locations, ResourceResolvingChain chain) {

    String key = computeKey(request, requestPath);
    Resource resource = this.cache.get(key, Resource.class);

    if (resource != null) {
      if (logger.isTraceEnabled()) {
        logger.trace("Resource resolved from cache");
      }
      return resource;
    }

    resource = chain.resolveResource(request, requestPath, locations);
    if (resource != null) {
      this.cache.put(key, resource);
    }

    return resource;
  }

  protected String computeKey(@Nullable RequestContext request, String requestPath) {
    if (request != null) {
      String codingKey = getContentCodingKey(request);
      if (StringUtils.hasText(codingKey)) {
        return RESOLVED_RESOURCE_CACHE_KEY_PREFIX + requestPath + "+encoding=" + codingKey;
      }
    }
    return RESOLVED_RESOURCE_CACHE_KEY_PREFIX + requestPath;
  }

  @Nullable
  private String getContentCodingKey(RequestContext request) {
    String header = request.getHeaders().getFirst(HttpHeaders.ACCEPT_ENCODING);
    if (StringUtils.hasText(header)) {
      return Arrays.stream(StringUtils.tokenizeToStringArray(header, ","))
              .map(token -> {
                int index = token.indexOf(';');
                return (index >= 0 ? token.substring(0, index) : token).trim().toLowerCase();
              })
              .filter(this.contentCodings::contains)
              .sorted()
              .collect(Collectors.joining(","));
    }
    return null;
  }

  @Override
  protected String resolveUrlPathInternal(
          String resourceUrlPath, List<? extends Resource> locations, ResourceResolvingChain chain) {

    String key = RESOLVED_URL_PATH_CACHE_KEY_PREFIX + resourceUrlPath;
    String resolvedUrlPath = this.cache.get(key, String.class);
    if (resolvedUrlPath != null) {
      if (logger.isTraceEnabled()) {
        logger.trace("Path resolved from cache");
      }
      return resolvedUrlPath;
    }

    resolvedUrlPath = chain.resolveUrlPath(resourceUrlPath, locations);
    if (resolvedUrlPath != null) {
      this.cache.put(key, resolvedUrlPath);
    }

    return resolvedUrlPath;
  }

}

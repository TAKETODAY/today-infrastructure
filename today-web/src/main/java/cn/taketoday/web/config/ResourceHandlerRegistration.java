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

package cn.taketoday.web.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import cn.taketoday.cache.Cache;
import cn.taketoday.cache.concurrent.ConcurrentMapCache;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.UrlResource;
import cn.taketoday.http.CacheControl;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.NotFoundHandler;
import cn.taketoday.web.resource.PathResourceResolver;
import cn.taketoday.web.resource.ResourceHttpRequestHandler;

/**
 * Encapsulates information required to create a resource handler.
 *
 * @author Rossen Stoyanchev
 * @author Keith Donald
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/15 17:06
 */
public class ResourceHandlerRegistration {

  private final String[] pathPatterns;

  private final List<String> locationValues = new ArrayList<>();

  private final List<Resource> locationsResources = new ArrayList<>();

  @Nullable
  private Integer cachePeriod;

  @Nullable
  private CacheControl cacheControl;

  @Nullable
  private ResourceChainRegistration resourceChainRegistration;

  private boolean useLastModified = true;

  private boolean optimizeLocations = false;

  @Nullable
  private Function<Resource, String> etagGenerator;

  @Nullable
  NotFoundHandler notFoundHandler;

  /**
   * Create a {@link ResourceHandlerRegistration} instance.
   *
   * @param pathPatterns one or more resource URL path patterns
   */
  public ResourceHandlerRegistration(String... pathPatterns) {
    Assert.notEmpty(pathPatterns, "At least one path pattern is required for resource handling.");
    this.pathPatterns = pathPatterns;
  }

  /**
   * Add one or more resource locations from which to serve static content.
   * Each location must point to a valid directory. Multiple locations may
   * be specified as a comma-separated list, and the locations will be checked
   * for a given resource in the order specified.
   * <p>For example, {{@code "/"}, {@code "classpath:/META-INF/public-web-resources/"}}
   * allows resources to be served both from the web application root and
   * from any JAR on the classpath that contains a
   * {@code /META-INF/public-web-resources/} directory, with resources in the
   * web application root taking precedence.
   * <p>For {@link UrlResource URL-based resources}
   * (e.g. files, HTTP URLs, etc) this method supports a special prefix to
   * indicate the charset associated with the URL so that relative paths
   * appended to it can be encoded correctly, e.g.
   * {@code [charset=Windows-31J]https://example.org/path}.
   *
   * @return the same {@link ResourceHandlerRegistration} instance, for
   * chained method invocation
   */
  public ResourceHandlerRegistration addResourceLocations(String... locations) {
    this.locationValues.addAll(Arrays.asList(locations));
    return this;
  }

  /**
   * Configure locations to serve static resources from based on pre-resolved
   * {@code Resource} references.
   *
   * @param locations the resource locations to use
   * @return the same {@link ResourceHandlerRegistration} instance, for
   * chained method invocation
   */
  public ResourceHandlerRegistration addResourceLocations(Resource... locations) {
    this.locationsResources.addAll(Arrays.asList(locations));
    return this;
  }

  /**
   * Specify the cache period for the resources served by the resource handler, in seconds. The default is to not
   * send any cache headers but to rely on last-modified timestamps only. Set to 0 in order to send cache headers
   * that prevent caching, or to a positive number of seconds to send cache headers with the given max-age value.
   *
   * @param cachePeriod the time to cache resources in seconds
   * @return the same {@link ResourceHandlerRegistration} instance, for chained method invocation
   */
  public ResourceHandlerRegistration setCachePeriod(Integer cachePeriod) {
    this.cachePeriod = cachePeriod;
    return this;
  }

  /**
   * Specify the {@link cn.taketoday.http.CacheControl} which should be used
   * by the resource handler.
   * <p>Setting a custom value here will override the configuration set with {@link #setCachePeriod}.
   *
   * @param cacheControl the CacheControl configuration to use
   * @return the same {@link ResourceHandlerRegistration} instance, for chained method invocation
   */
  public ResourceHandlerRegistration setCacheControl(CacheControl cacheControl) {
    this.cacheControl = cacheControl;
    return this;
  }

  /**
   * Set whether the {@link Resource#lastModified()} information should be used to drive HTTP responses.
   * <p>This configuration is set to {@code true} by default.
   *
   * @param useLastModified whether the "last modified" resource information should be used
   * @return the same {@link ResourceHandlerRegistration} instance, for chained method invocation
   * @see ResourceHttpRequestHandler#setUseLastModified
   */
  public ResourceHandlerRegistration setUseLastModified(boolean useLastModified) {
    this.useLastModified = useLastModified;
    return this;
  }

  /**
   * Configure a generator function that will be used to create the ETag information,
   * given a {@link Resource} that is about to be written to the response.
   * <p>This function should return a String that will be used as an argument in
   * {@link cn.taketoday.web.RequestContext#checkNotModified(String)}, or {@code null} if no value
   * can be generated for the given resource.
   *
   * @param etagGenerator the HTTP ETag generator function to use.
   * @see ResourceHttpRequestHandler#setEtagGenerator(Function)
   */
  public ResourceHandlerRegistration setEtagGenerator(@Nullable Function<Resource, String> etagGenerator) {
    this.etagGenerator = etagGenerator;
    return this;
  }

  /**
   * Set whether to optimize the specified locations through an existence check on startup,
   * filtering non-existing directories upfront so that they do not have to be checked
   * on every resource access.
   * <p>The default is {@code false}, for defensiveness against zip files without directory
   * entries which are unable to expose the existence of a directory upfront. Switch this flag to
   * {@code true} for optimized access in case of a consistent jar layout with directory entries.
   *
   * @param optimizeLocations whether to optimize the locations through an existence check on startup
   * @return the same {@link ResourceHandlerRegistration} instance, for chained method invocation
   * @see ResourceHttpRequestHandler#setOptimizeLocations
   */
  public ResourceHandlerRegistration setOptimizeLocations(boolean optimizeLocations) {
    this.optimizeLocations = optimizeLocations;
    return this;
  }

  /**
   * Configure a chain of resource resolvers and transformers to use. This
   * can be useful, for example, to apply a version strategy to resource URLs.
   * <p>If this method is not invoked, by default only a simple
   * {@link PathResourceResolver} is used in order to match URL paths to
   * resources under the configured locations.
   *
   * @param cacheResources whether to cache the result of resource resolution;
   * setting this to "true" is recommended for production (and "false" for
   * development, especially when applying a version strategy)
   * @return the same {@link ResourceHandlerRegistration} instance, for chained method invocation
   */
  public ResourceChainRegistration resourceChain(boolean cacheResources) {
    this.resourceChainRegistration = new ResourceChainRegistration(cacheResources);
    return this.resourceChainRegistration;
  }

  /**
   * Configure a chain of resource resolvers and transformers to use. This
   * can be useful, for example, to apply a version strategy to resource URLs.
   * <p>If this method is not invoked, by default only a simple
   * {@link PathResourceResolver} is used in order to match URL paths to
   * resources under the configured locations.
   *
   * @param cacheResources whether to cache the result of resource resolution;
   * setting this to "true" is recommended for production (and "false" for
   * development, especially when applying a version strategy
   * @param cache the cache to use for storing resolved and transformed resources;
   * by default a {@link ConcurrentMapCache}
   * is used. Since Resources aren't serializable and can be dependent on the
   * application host, one should not use a distributed cache but rather an
   * in-memory cache.
   * @return the same {@link ResourceHandlerRegistration} instance, for chained method invocation
   */
  public ResourceChainRegistration resourceChain(boolean cacheResources, Cache cache) {
    this.resourceChainRegistration = new ResourceChainRegistration(cacheResources, cache);
    return this.resourceChainRegistration;
  }

  /**
   * Set not found handler
   * <p>
   * handle resource not found
   *
   * @param notFoundHandler HttpRequestHandler
   */
  public ResourceHandlerRegistration notFoundHandler(@Nullable NotFoundHandler notFoundHandler) {
    this.notFoundHandler = notFoundHandler;
    return this;
  }

  /**
   * Return the URL path patterns for the resource handler.
   */
  protected String[] getPathPatterns() {
    return this.pathPatterns;
  }

  /**
   * Return a {@link ResourceHttpRequestHandler} instance.
   */
  protected ResourceHttpRequestHandler getRequestHandler() {
    ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
    if (resourceChainRegistration != null) {
      handler.setResourceResolvers(resourceChainRegistration.getResourceResolvers());
      handler.setResourceTransformers(resourceChainRegistration.getResourceTransformers());
    }
    handler.setLocationValues(locationValues);
    handler.setLocations(locationsResources);
    if (cacheControl != null) {
      handler.setCacheControl(cacheControl);
    }
    else if (cachePeriod != null) {
      handler.setCacheSeconds(cachePeriod);
    }
    if (notFoundHandler != null) {
      handler.setNotFoundHandler(notFoundHandler);
    }
    handler.setEtagGenerator(etagGenerator);
    handler.setUseLastModified(useLastModified);
    handler.setOptimizeLocations(optimizeLocations);
    return handler;
  }

}

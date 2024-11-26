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

package infra.web.config;

import java.util.ArrayList;
import java.util.List;

import infra.cache.Cache;
import infra.cache.concurrent.ConcurrentMapCache;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.util.ClassUtils;
import infra.web.resource.CachingResourceResolver;
import infra.web.resource.CachingResourceTransformer;
import infra.web.resource.CssLinkResourceTransformer;
import infra.web.resource.LiteWebJarsResourceResolver;
import infra.web.resource.PathResourceResolver;
import infra.web.resource.ResourceResolver;
import infra.web.resource.ResourceTransformer;
import infra.web.resource.VersionResourceResolver;
import infra.web.resource.WebJarsResourceResolver;

/**
 * Assists with the registration of resource resolvers and transformers.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/15 17:04
 */
public class ResourceChainRegistration {

  private static final String DEFAULT_CACHE_NAME = "resource-chain-cache";

  private static final boolean isWebJarAssetLocatorPresent = ClassUtils.isPresent(
          "org.webjars.WebJarAssetLocator", ResourceChainRegistration.class.getClassLoader());

  private static final boolean isWebJarVersionLocatorPresent = ClassUtils.isPresent(
          "org.webjars.WebJarVersionLocator", ResourceChainRegistration.class.getClassLoader());

  private final List<ResourceResolver> resolvers = new ArrayList<>(4);

  private final List<ResourceTransformer> transformers = new ArrayList<>(4);

  private boolean hasVersionResolver;

  private boolean hasPathResolver;

  private boolean hasCssLinkTransformer;

  private boolean hasWebjarsResolver;

  public ResourceChainRegistration(boolean cacheResources) {
    this(cacheResources, (cacheResources ? new ConcurrentMapCache(DEFAULT_CACHE_NAME) : null));
  }

  public ResourceChainRegistration(boolean cacheResources, @Nullable Cache cache) {
    Assert.isTrue(!cacheResources || cache != null, "'cache' is required when cacheResources=true");
    if (cacheResources) {
      this.resolvers.add(new CachingResourceResolver(cache));
      this.transformers.add(new CachingResourceTransformer(cache));
    }
  }

  /**
   * Add a resource resolver to the chain.
   *
   * @param resolver the resolver to add
   * @return the current instance for chained method invocation
   */
  public ResourceChainRegistration addResolver(ResourceResolver resolver) {
    Assert.notNull(resolver, "The provided ResourceResolver should not be null");
    this.resolvers.add(resolver);
    if (resolver instanceof VersionResourceResolver) {
      this.hasVersionResolver = true;
    }
    else if (resolver instanceof PathResourceResolver) {
      this.hasPathResolver = true;
    }
    else if (resolver instanceof WebJarsResourceResolver || resolver instanceof LiteWebJarsResourceResolver) {
      this.hasWebjarsResolver = true;
    }
    return this;
  }

  /**
   * Add a resource transformer to the chain.
   *
   * @param transformer the transformer to add
   * @return the current instance for chained method invocation
   */
  public ResourceChainRegistration addTransformer(ResourceTransformer transformer) {
    Assert.notNull(transformer, "The provided ResourceTransformer should not be null");
    this.transformers.add(transformer);
    if (transformer instanceof CssLinkResourceTransformer) {
      this.hasCssLinkTransformer = true;
    }
    return this;
  }

  protected List<ResourceResolver> getResourceResolvers() {
    if (!this.hasPathResolver) {
      ArrayList<ResourceResolver> result = new ArrayList<>(this.resolvers);
      if (isWebJarVersionLocatorPresent && !this.hasWebjarsResolver) {
        result.add(new LiteWebJarsResourceResolver());
      }
      else if (isWebJarAssetLocatorPresent && !this.hasWebjarsResolver) {
        result.add(new WebJarsResourceResolver());
      }
      result.add(new PathResourceResolver());
      return result;
    }
    return this.resolvers;
  }

  protected List<ResourceTransformer> getResourceTransformers() {
    if (this.hasVersionResolver && !this.hasCssLinkTransformer) {
      List<ResourceTransformer> result = new ArrayList<>(this.transformers);
      boolean hasTransformers = !this.transformers.isEmpty();
      boolean hasCaching = hasTransformers && this.transformers.get(0) instanceof CachingResourceTransformer;
      result.add(hasCaching ? 1 : 0, new CssLinkResourceTransformer());
      return result;
    }
    return this.transformers;
  }

}

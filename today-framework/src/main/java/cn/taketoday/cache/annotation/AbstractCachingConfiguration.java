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

package cn.taketoday.cache.annotation;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import cn.taketoday.beans.factory.ObjectProvider;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.cache.CacheManager;
import cn.taketoday.cache.interceptor.CacheErrorHandler;
import cn.taketoday.cache.interceptor.CacheResolver;
import cn.taketoday.cache.interceptor.KeyGenerator;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.aware.ImportAware;
import cn.taketoday.core.annotation.AnnotationAttributes;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.function.SingletonSupplier;

/**
 * Abstract base {@code @Configuration} class providing common structure
 * for enabling Framework's annotation-driven cache management capability.
 *
 * @author Chris Beams
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @see EnableCaching
 * @since 4.0
 */
@Configuration(proxyBeanMethods = false)
public abstract class AbstractCachingConfiguration implements ImportAware {

  @Nullable
  protected AnnotationAttributes enableCaching;

  @Nullable
  protected Supplier<CacheManager> cacheManager;

  @Nullable
  protected Supplier<CacheResolver> cacheResolver;

  @Nullable
  protected Supplier<KeyGenerator> keyGenerator;

  @Nullable
  protected Supplier<CacheErrorHandler> errorHandler;

  @Override
  public void setImportMetadata(AnnotationMetadata importMetadata) {
    this.enableCaching = AnnotationAttributes.fromMap(
            importMetadata.getAnnotationAttributes(EnableCaching.class.getName()));
    if (this.enableCaching == null) {
      throw new IllegalArgumentException(
              "@EnableCaching is not present on importing class " + importMetadata.getClassName());
    }
  }

  @Autowired
  void setConfigurers(ObjectProvider<CachingConfigurer> configurers) {
    Supplier<CachingConfigurer> configurer = () -> {
      List<CachingConfigurer> candidates = configurers.stream().toList();
      if (CollectionUtils.isEmpty(candidates)) {
        return null;
      }
      if (candidates.size() > 1) {
        throw new IllegalStateException(candidates.size() + " implementations of " +
                "CachingConfigurer were found when only 1 was expected. " +
                "Refactor the configuration such that CachingConfigurer is " +
                "implemented only once or not at all.");
      }
      return candidates.get(0);
    };
    useCachingConfigurer(new CachingConfigurerSupplier(configurer));
  }

  /**
   * Extract the configuration from the nominated {@link CachingConfigurer}.
   */
  protected void useCachingConfigurer(CachingConfigurerSupplier cachingConfigurerSupplier) {
    this.cacheManager = cachingConfigurerSupplier.adapt(CachingConfigurer::cacheManager);
    this.cacheResolver = cachingConfigurerSupplier.adapt(CachingConfigurer::cacheResolver);
    this.keyGenerator = cachingConfigurerSupplier.adapt(CachingConfigurer::keyGenerator);
    this.errorHandler = cachingConfigurerSupplier.adapt(CachingConfigurer::errorHandler);
  }

  protected static class CachingConfigurerSupplier {

    private final Supplier<CachingConfigurer> supplier;

    public CachingConfigurerSupplier(Supplier<CachingConfigurer> supplier) {
      this.supplier = SingletonSupplier.from(supplier);
    }

    /**
     * Adapt the {@link CachingConfigurer} supplier to another supplier
     * provided by the specified mapping function. If the underlying
     * {@link CachingConfigurer} is {@code null}, {@code null} is returned
     * and the mapping function is not invoked.
     *
     * @param provider the provider to use to adapt the supplier
     * @param <T> the type of the supplier
     * @return another supplier mapped by the specified function
     */
    @Nullable
    public <T> Supplier<T> adapt(Function<CachingConfigurer, T> provider) {
      return () -> {
        CachingConfigurer cachingConfigurer = this.supplier.get();
        return (cachingConfigurer != null ? provider.apply(cachingConfigurer) : null);
      };
    }

  }

}

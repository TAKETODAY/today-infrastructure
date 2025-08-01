/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.cache.annotation;

import java.util.function.Function;
import java.util.function.Supplier;

import infra.beans.BeansException;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryAware;
import infra.beans.factory.annotation.DisableDependencyInjection;
import infra.cache.CacheManager;
import infra.cache.interceptor.CacheErrorHandler;
import infra.cache.interceptor.CacheResolver;
import infra.cache.interceptor.KeyGenerator;
import infra.context.annotation.Configuration;
import infra.context.annotation.ImportAware;
import infra.core.annotation.AnnotationAttributes;
import infra.core.type.AnnotationMetadata;
import infra.lang.Nullable;
import infra.util.ObjectUtils;
import infra.util.function.SingletonSupplier;

/**
 * Abstract base {@code @Configuration} class providing common structure
 * for enabling Infra annotation-driven cache management capability.
 *
 * @author Chris Beams
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see EnableCaching
 * @since 4.0
 */
@DisableDependencyInjection
@Configuration(proxyBeanMethods = false)
public abstract class AbstractCachingConfiguration implements ImportAware, BeanFactoryAware {

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
            importMetadata.getAnnotationAttributes(EnableCaching.class));
    if (this.enableCaching == null) {
      throw new IllegalArgumentException(
              "@EnableCaching is not present on importing class " + importMetadata.getClassName());
    }
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
    useCachingConfigurer(new CachingConfigurerSupplier(() -> {
      var candidates = beanFactory.getBeanNamesForType(CachingConfigurer.class);
      if (ObjectUtils.isEmpty(candidates)) {
        return null;
      }
      if (candidates.length > 1) {
        throw new IllegalStateException(candidates.length + " implementations of " +
                "CachingConfigurer were found when only 1 was expected. " +
                "Refactor the configuration such that CachingConfigurer is " +
                "implemented only once or not at all.");
      }
      return beanFactory.getBean(candidates[0], CachingConfigurer.class);
    }));
  }

  /**
   * Extract the configuration from the nominated {@link CachingConfigurer}.
   */
  protected void useCachingConfigurer(CachingConfigurerSupplier supplier) {
    this.cacheManager = supplier.adapt(CachingConfigurer::cacheManager);
    this.cacheResolver = supplier.adapt(CachingConfigurer::cacheResolver);
    this.keyGenerator = supplier.adapt(CachingConfigurer::keyGenerator);
    this.errorHandler = supplier.adapt(CachingConfigurer::errorHandler);
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

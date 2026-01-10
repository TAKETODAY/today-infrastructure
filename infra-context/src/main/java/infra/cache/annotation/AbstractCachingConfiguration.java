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

package infra.cache.annotation;

import org.jspecify.annotations.Nullable;

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
    useCachingConfigurer(new CachingConfigurerSupplier((Supplier<@Nullable CachingConfigurer>) () -> {
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
  @SuppressWarnings("NullAway") // https://github.com/uber/NullAway/issues/1128
  protected void useCachingConfigurer(CachingConfigurerSupplier supplier) {
    this.cacheManager = supplier.adapt(CachingConfigurer::cacheManager);
    this.cacheResolver = supplier.adapt(CachingConfigurer::cacheResolver);
    this.keyGenerator = supplier.adapt(CachingConfigurer::keyGenerator);
    this.errorHandler = supplier.adapt(CachingConfigurer::errorHandler);
  }

  protected static class CachingConfigurerSupplier {

    private final SingletonSupplier<CachingConfigurer> supplier;

    @SuppressWarnings("NullAway")
    public CachingConfigurerSupplier(Supplier<@Nullable CachingConfigurer> supplier) {
      this.supplier = SingletonSupplier.ofNullable(supplier);
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
    public <T> Supplier<@Nullable T> adapt(Function<CachingConfigurer, @Nullable T> provider) {
      return () -> {
        CachingConfigurer cachingConfigurer = this.supplier.get();
        return (cachingConfigurer != null ? provider.apply(cachingConfigurer) : null);
      };
    }

  }

}

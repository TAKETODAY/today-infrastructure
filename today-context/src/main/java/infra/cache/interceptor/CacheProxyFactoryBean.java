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

package infra.cache.interceptor;

import infra.aop.Pointcut;
import infra.aop.framework.AbstractSingletonProxyFactoryBean;
import infra.aop.framework.ProxyFactoryBean;
import infra.aop.support.DefaultPointcutAdvisor;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryAware;
import infra.beans.factory.SmartInitializingSingleton;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.cache.CacheManager;
import infra.cache.annotation.Cacheable;

/**
 * Proxy factory bean for simplified declarative caching handling.
 * This is a convenient alternative to a standard AOP
 * {@link ProxyFactoryBean}
 * with a separate {@link CacheInterceptor} definition.
 *
 * <p>This class is designed to facilitate declarative cache demarcation: namely, wrapping
 * a singleton target object with a caching proxy, proxying all the interfaces that the
 * target implements. Exists primarily for third-party framework integration.
 * <strong>Users should favor the {@code cache:} XML namespace
 * {@link Cacheable @Cacheable} annotation.</strong>
 * See the
 * <a href="https://docs.today-tech.cn/today-infrastructure/integration.html#cache-annotations">declarative annotation-based caching</a>
 * section of the Framework reference documentation for more information.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ProxyFactoryBean
 * @see CacheInterceptor
 * @since 4.0
 */
@SuppressWarnings("serial")
public class CacheProxyFactoryBean extends AbstractSingletonProxyFactoryBean
        implements BeanFactoryAware, SmartInitializingSingleton {

  private final CacheInterceptor cacheInterceptor = new CacheInterceptor();

  private Pointcut pointcut = Pointcut.TRUE;

  /**
   * Set one or more sources to find cache operations.
   *
   * @see CacheInterceptor#setCacheOperationSources
   */
  public void setCacheOperationSources(CacheOperationSource... cacheOperationSources) {
    this.cacheInterceptor.setCacheOperationSources(cacheOperationSources);
  }

  /**
   * Set the default {@link KeyGenerator} that this cache aspect should delegate to
   * if no specific key generator has been set for the operation.
   * <p>The default is a {@link SimpleKeyGenerator}.
   *
   * @see CacheInterceptor#setKeyGenerator
   * @since 4.0
   */
  public void setKeyGenerator(KeyGenerator keyGenerator) {
    this.cacheInterceptor.setKeyGenerator(keyGenerator);
  }

  /**
   * Set the default {@link CacheResolver} that this cache aspect should delegate
   * to if no specific cache resolver has been set for the operation.
   * <p>The default resolver resolves the caches against their names and the
   * default cache manager.
   *
   * @see CacheInterceptor#setCacheResolver
   * @since 4.0
   */
  public void setCacheResolver(CacheResolver cacheResolver) {
    this.cacheInterceptor.setCacheResolver(cacheResolver);
  }

  /**
   * Set the {@link CacheManager} to use to create a default {@link CacheResolver}.
   * Replace the current {@link CacheResolver}, if any.
   *
   * @see CacheInterceptor#setCacheManager
   */
  public void setCacheManager(CacheManager cacheManager) {
    this.cacheInterceptor.setCacheManager(cacheManager);
  }

  /**
   * Set a pointcut, i.e. a bean that triggers conditional invocation of the
   * {@link CacheInterceptor} depending on the method and attributes passed.
   * <p>Note: Additional interceptors are always invoked.
   *
   * @see #setPreInterceptors
   * @see #setPostInterceptors
   */
  public void setPointcut(Pointcut pointcut) {
    this.pointcut = pointcut;
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    this.cacheInterceptor.setBeanFactory(beanFactory);
  }

  @Override
  public void afterSingletonsInstantiated() {
    this.cacheInterceptor.afterSingletonsInstantiated();
  }

  @Override
  public void afterSingletonsInstantiated(ConfigurableBeanFactory beanFactory) {
    cacheInterceptor.afterSingletonsInstantiated(beanFactory);
  }

  @Override
  protected Object createMainInterceptor() {
    this.cacheInterceptor.afterPropertiesSet();
    return new DefaultPointcutAdvisor(this.pointcut, this.cacheInterceptor);
  }

}

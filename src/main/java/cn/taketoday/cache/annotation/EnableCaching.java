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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.cache.CacheManager;
import cn.taketoday.cache.interceptor.CacheInterceptor;
import cn.taketoday.cache.interceptor.KeyGenerator;
import cn.taketoday.cache.interceptor.SimpleKeyGenerator;
import cn.taketoday.context.annotation.AdviceMode;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.core.Ordered;

/**
 * Enables Framework's annotation-driven cache management capability, similar to the
 * support found in Framework's {@code <cache:*>} XML namespace. To be used together
 * with @{@link cn.taketoday.context.annotation.Configuration Configuration}
 * classes as follows:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableCaching
 * public class AppConfig {
 *
 *     &#064;Bean
 *     public MyService myService() {
 *         // configure and return a class having &#064;Cacheable methods
 *         return new MyService();
 *     }
 *
 *     &#064;Bean
 *     public CacheManager cacheManager() {
 *         // configure and return an implementation of Framework's CacheManager SPI
 *         SimpleCacheManager cacheManager = new SimpleCacheManager();
 *         cacheManager.setCaches(Arrays.asList(new ConcurrentMapCache("default")));
 *         return cacheManager;
 *     }
 * }</pre>
 *
 * <p>For reference, the example above can be compared to the following Spring XML
 * configuration:
 *
 * <pre class="code">
 * &lt;beans&gt;
 *
 *     &lt;cache:annotation-driven/&gt;
 *
 *     &lt;bean id="myService" class="com.foo.MyService"/&gt;
 *
 *     &lt;bean id="cacheManager" class="cn.taketoday.cache.support.SimpleCacheManager"&gt;
 *         &lt;property name="caches"&gt;
 *             &lt;set&gt;
 *                 &lt;bean class="cn.taketoday.cache.concurrent.ConcurrentMapCacheFactoryBean"&gt;
 *                     &lt;property name="name" value="default"/&gt;
 *                 &lt;/bean&gt;
 *             &lt;/set&gt;
 *         &lt;/property&gt;
 *     &lt;/bean&gt;
 *
 * &lt;/beans&gt;
 * </pre>
 *
 * In both of the scenarios above, {@code @EnableCaching} and {@code
 * <cache:annotation-driven/>} are responsible for registering the necessary Spring
 * components that power annotation-driven cache management, such as the
 * {@link CacheInterceptor CacheInterceptor} and the
 * proxy- or AspectJ-based advice that weaves the interceptor into the call stack when
 * {@link Cacheable @Cacheable} methods are invoked.
 *
 * <p>If the JSR-107 API and Framework's JCache implementation are present, the necessary
 * components to manage standard cache annotations are also registered. This creates the
 * proxy- or AspectJ-based advice that weaves the interceptor into the call stack when
 * methods annotated with {@code CacheResult}, {@code CachePut}, {@code CacheRemove} or
 * {@code CacheRemoveAll} are invoked.
 *
 * <p><strong>A bean of type {@link CacheManager CacheManager}
 * must be registered</strong>, as there is no reasonable default that the framework can
 * use as a convention. And whereas the {@code <cache:annotation-driven>} element assumes
 * a bean <em>named</em> "cacheManager", {@code @EnableCaching} searches for a cache
 * manager bean <em>by type</em>. Therefore, naming of the cache manager bean method is
 * not significant.
 *
 * <p>For those that wish to establish a more direct relationship between
 * {@code @EnableCaching} and the exact cache manager bean to be used,
 * the {@link CachingConfigurer} callback interface may be implemented.
 * Notice the {@code @Override}-annotated methods below:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableCaching
 * public class AppConfig implements CachingConfigurer {
 *
 *     &#064;Bean
 *     public MyService myService() {
 *         // configure and return a class having &#064;Cacheable methods
 *         return new MyService();
 *     }
 *
 *     &#064;Bean
 *     &#064;Override
 *     public CacheManager cacheManager() {
 *         // configure and return an implementation of Framework's CacheManager SPI
 *         SimpleCacheManager cacheManager = new SimpleCacheManager();
 *         cacheManager.setCaches(Arrays.asList(new ConcurrentMapCache("default")));
 *         return cacheManager;
 *     }
 *
 *     &#064;Bean
 *     &#064;Override
 *     public KeyGenerator keyGenerator() {
 *         // configure and return an implementation of Framework's KeyGenerator SPI
 *         return new MyKeyGenerator();
 *     }
 * }</pre>
 *
 * This approach may be desirable simply because it is more explicit, or it may be
 * necessary in order to distinguish between two {@code CacheManager} beans present in the
 * same container.
 *
 * <p>Notice also the {@code keyGenerator} method in the example above. This allows for
 * customizing the strategy for cache key generation, per Framework's {@link
 * KeyGenerator KeyGenerator} SPI. Normally,
 * {@code @EnableCaching} will configure Framework's
 * {@link SimpleKeyGenerator SimpleKeyGenerator}
 * for this purpose, but when implementing {@code CachingConfigurer}, a key generator
 * must be provided explicitly. Return {@code null} or {@code new SimpleKeyGenerator()}
 * from this method if no customization is necessary.
 *
 * <p>{@link CachingConfigurer} offers additional customization options:
 * that provides a default implementation for all methods which
 * can be useful if you do not need to customize everything. See {@link CachingConfigurer}
 * Javadoc for further details.
 *
 * <p>The {@link #mode} attribute controls how advice is applied: If the mode is
 * {@link AdviceMode#PROXY} (the default), then the other attributes control the behavior
 * of the proxying. Please note that proxy mode allows for interception of calls through
 * the proxy only; local calls within the same class cannot get intercepted that way.
 *
 * <p>Note that if the {@linkplain #mode} is set to {@link AdviceMode#ASPECTJ}, then the
 * value of the {@link #proxyTargetClass} attribute will be ignored. Note also that in
 * this case the {@code spring-aspects} module JAR must be present on the classpath, with
 * compile-time weaving or load-time weaving applying the aspect to the affected classes.
 * There is no proxy involved in such a scenario; local calls will be intercepted as well.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @see CachingConfigurer
 * @see CachingConfigurationSelector
 * @see ProxyCachingConfiguration
 * @see cn.taketoday.cache.aspectj.AspectJCachingConfiguration
 * @since 4.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(CachingConfigurationSelector.class)
public @interface EnableCaching {

  /**
   * Indicate whether subclass-based (CGLIB) proxies are to be created as opposed
   * to standard Java interface-based proxies. The default is {@code false}. <strong>
   * Applicable only if {@link #mode()} is set to {@link AdviceMode#PROXY}</strong>.
   * <p>Note that setting this attribute to {@code true} will affect <em>all</em>
   * Spring-managed beans requiring proxying, not just those marked with {@code @Cacheable}.
   * For example, other beans marked with Framework's {@code @Transactional} annotation will
   * be upgraded to subclass proxying at the same time. This approach has no negative
   * impact in practice unless one is explicitly expecting one type of proxy vs another,
   * e.g. in tests.
   */
  boolean proxyTargetClass() default false;

  /**
   * Indicate how caching advice should be applied.
   * <p><b>The default is {@link AdviceMode#PROXY}.</b>
   * Please note that proxy mode allows for interception of calls through the proxy
   * only. Local calls within the same class cannot get intercepted that way;
   * a caching annotation on such a method within a local call will be ignored
   * since Framework's interceptor does not even kick in for such a runtime scenario.
   * For a more advanced mode of interception, consider switching this to
   * {@link AdviceMode#ASPECTJ}.
   */
  AdviceMode mode() default AdviceMode.PROXY;

  /**
   * Indicate the ordering of the execution of the caching advisor
   * when multiple advices are applied at a specific joinpoint.
   * <p>The default is {@link Ordered#LOWEST_PRECEDENCE}.
   */
  int order() default Ordered.LOWEST_PRECEDENCE;

}

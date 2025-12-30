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

package infra.resilience.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.context.annotation.Import;
import infra.core.Ordered;

/**
 * Enables Infra core resilience features for method invocations:
 * {@link ConcurrencyLimit @ConcurrencyLimit}.
 *
 * <p>These annotations can also be individually enabled by
 * defining a {@link ConcurrencyLimitBeanPostProcessor}.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see ConcurrencyLimitBeanPostProcessor
 * @since 5.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(ResilientMethodsConfiguration.class)
public @interface EnableResilientMethods {

  /**
   * Indicate whether subclass-based (CGLIB) proxies are to be created as opposed
   * to standard Java interface-based proxies.
   * <p>The default is {@code false}.
   * <p>Note that setting this attribute to {@code true} will only affect
   * {@link ConcurrencyLimitBeanPostProcessor}.
   * <p>It is usually recommendable to rely on a global default proxy configuration
   * instead, with specific proxy requirements for certain beans expressed through
   * a {@link infra.context.annotation.Proxyable} annotation on
   * the affected bean classes.
   *
   * @see infra.aop.config.AopConfigUtils#forceAutoProxyCreatorToUseClassProxying
   */
  boolean proxyTargetClass() default false;

  /**
   * Indicate the order in which the  {@link ConcurrencyLimitBeanPostProcessor} should be applied.
   * <p>The default is {@link Ordered#LOWEST_PRECEDENCE} in order to run
   * after all other post-processors, so that they can add advisors to
   * existing proxies rather than double-proxy.
   */
  int order() default Ordered.LOWEST_PRECEDENCE;

}

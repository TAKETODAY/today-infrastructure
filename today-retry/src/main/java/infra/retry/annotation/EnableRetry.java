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

package infra.retry.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.context.annotation.EnableAspectJAutoProxy;
import infra.context.annotation.Import;
import infra.core.Ordered;
import infra.core.annotation.AliasFor;

/**
 * Global enabler for <code>@Retryable</code> annotations in Infra beans. If this is
 * declared on any <code>@Configuration</code> in the context then beans that have
 * retryable methods will be proxied and the retry handled according to the metadata in
 * the annotations.
 *
 * @author Dave Syer
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@EnableAspectJAutoProxy(proxyTargetClass = false)
@Import(RetryConfiguration.class)
public @interface EnableRetry {

  /**
   * Indicate whether subclass-based (CGLIB) proxies are to be created as opposed to
   * standard Java interface-based proxies. The default is {@code false}.
   *
   * @return whether to proxy or not to proxy the class
   */
  @AliasFor(annotation = EnableAspectJAutoProxy.class)
  boolean proxyTargetClass() default false;

  /**
   * Indicate the order in which the {@link RetryConfiguration} AOP <b>advice</b> should
   * be applied.
   * <p>
   * The default is {@code Ordered.LOWEST_PRECEDENCE - 1} in order to make sure the
   * advice is applied before other advices with {@link Ordered#LOWEST_PRECEDENCE} order
   * (e.g. an advice responsible for {@code @Transactional} behavior).
   */
  int order() default Ordered.LOWEST_PRECEDENCE - 1;
}

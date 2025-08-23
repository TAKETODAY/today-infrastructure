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

package infra.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Common annotation for suggesting a specific proxy type for a {@link Bean @Bean}
 * method or {@link infra.stereotype.Component @Component} class,
 * overriding a globally configured default.
 *
 * <p>Only actually applying in case of a bean actually getting auto-proxied in
 * the first place. Actual auto-proxying is dependent on external configuration.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see infra.aop.framework.autoproxy.AutoProxyUtils#PRESERVE_TARGET_CLASS_ATTRIBUTE
 * @see infra.aop.framework.autoproxy.AutoProxyUtils#EXPOSED_INTERFACES_ATTRIBUTE
 * @since 5.0
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Proxyable {

  /**
   * Suggest a specific proxy type, either {@link ProxyType#INTERFACES} for
   * a JDK dynamic proxy or {@link ProxyType#TARGET_CLASS} for a CGLIB proxy,
   * overriding a globally configured default.
   */
  ProxyType value() default ProxyType.DEFAULT;

  /**
   * Suggest a JDK dynamic proxy with specific interfaces to expose, overriding
   * a globally configured default.
   * <p>Only taken into account if {@link #value()} is not {@link ProxyType#TARGET_CLASS}.
   */
  Class<?>[] interfaces() default {};

}

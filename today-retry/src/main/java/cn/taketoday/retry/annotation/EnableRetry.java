/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.retry.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.context.annotation.EnableAspectJAutoProxy;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.annotation.AliasFor;

/**
 * Global enabler for <code>@Retryable</code> annotations in Infra beans. If this is
 * declared on any <code>@Configuration</code> in the context then beans that have
 * retryable methods will be proxied and the retry handled according to the metadata in
 * the annotations.
 *
 * @author Dave Syer
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

}

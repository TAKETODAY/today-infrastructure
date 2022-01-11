/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.context.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enable Aspect Oriented Programming
 *
 * @author TODAY <br>
 * 2020-02-06 20:02
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Import(AutoProxyRegistrar.class)
public @interface EnableAspectAutoProxy {

  /**
   * Return whether the AOP proxy will expose the AOP proxy for
   * each invocation.
   */
  boolean exposeProxy() default false;

  /**
   * Return whether to proxy the target class directly as well as any interfaces.
   */
  boolean proxyTargetClass() default true;

  /**
   * Indicate how advice should be applied.
   * <p><b>The default is {@link AdviceMode#PROXY}.</b>
   * Please note that proxy mode allows for interception of calls through the proxy
   * only. Local calls within the same class cannot get intercepted that way; an
   * annotation on such a method within a local call will be
   * ignored since Framework's interceptor does not even kick in for such a runtime
   * scenario. For a more advanced mode of interception, consider switching this to
   * {@link AdviceMode#ASPECTJ}.
   */
  AdviceMode mode() default AdviceMode.PROXY;

}

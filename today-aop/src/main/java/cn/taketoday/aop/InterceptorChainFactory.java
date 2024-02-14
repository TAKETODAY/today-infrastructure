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

package cn.taketoday.aop;

import org.aopalliance.intercept.MethodInterceptor;

import java.lang.reflect.Method;

import cn.taketoday.aop.framework.Advised;
import cn.taketoday.lang.Nullable;

/**
 * Factory interface for advisor chains.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface InterceptorChainFactory {

  MethodInterceptor[] EMPTY_INTERCEPTOR = new MethodInterceptor[0];

  /**
   * Determine a list of {@link org.aopalliance.intercept.MethodInterceptor} objects
   * for the given advisor chain configuration.
   *
   * @param config the AOP configuration in the form of an Advised object
   * @param method the proxied method
   * @param targetClass the target class (may be {@code null} to indicate a proxy without
   * target object, in which case the method's declaring class is the next best option)
   * @return an array of MethodInterceptors (may also include RuntimeMethodInterceptor)
   */
  MethodInterceptor[] getInterceptors(Advised config, Method method, @Nullable Class<?> targetClass);

}

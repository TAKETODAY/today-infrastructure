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

package cn.taketoday.aop;

import org.aopalliance.intercept.MethodInvocation;

import cn.taketoday.lang.Nullable;

/**
 * Extension of the AOP Alliance {@link org.aopalliance.intercept.MethodInvocation}
 * interface, allowing access to the proxy that the method invocation was made through.
 *
 * <p>Useful to be able to substitute return values with the proxy,
 * if necessary, for example if the invocation target returned itself.
 *
 * @author Juergen Hoeller
 * @author Adrian Colyer
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see cn.taketoday.aop.support.DelegatingIntroductionInterceptor
 * @since 4.0 2022/3/9 11:57
 */
public interface ProxyMethodInvocation extends MethodInvocation, TargetClassAware {

  /**
   * Return the proxy that this method invocation was made through.
   *
   * @return the original proxy object
   */
  Object getProxy();

  /**
   * Create a clone of this object. If cloning is done before {@code proceed()}
   * is invoked on this object, {@code proceed()} can be invoked once per clone
   * to invoke the joinpoint (and the rest of the advice chain) more than once.
   *
   * @return an invocable clone of this invocation.
   * {@code proceed()} can be called once per clone.
   */
  MethodInvocation invocableClone();

  /**
   * Create a clone of this object. If cloning is done before {@code proceed()}
   * is invoked on this object, {@code proceed()} can be invoked once per clone
   * to invoke the joinpoint (and the rest of the advice chain) more than once.
   *
   * @param arguments the arguments that the cloned invocation is supposed to use,
   * overriding the original arguments
   * @return an invocable clone of this invocation.
   * {@code proceed()} can be called once per clone.
   */
  MethodInvocation invocableClone(Object... arguments);

  /**
   * Set the arguments to be used on subsequent invocations in the any advice
   * in this chain.
   *
   * @param arguments the argument array
   */
  void setArguments(Object... arguments);

  /**
   * Add the specified user attribute with the given value to this invocation.
   * <p>Such attributes are not used within the AOP framework itself. They are
   * just kept as part of the invocation object, for use in special interceptors.
   *
   * @param key the name of the attribute
   * @param value the value of the attribute, or {@code null} to reset it
   */
  void setAttribute(String key, @Nullable Object value);

  /**
   * Return the value of the specified user attribute.
   *
   * @param key the name of the attribute
   * @return the value of the attribute, or {@code null} if not set
   * @see #setAttribute
   */
  @Nullable
  Object getAttribute(String key);

}

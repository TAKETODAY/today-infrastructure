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
package cn.taketoday.aop.interceptor;

import org.aopalliance.intercept.MethodInvocation;

import java.lang.reflect.Method;

import cn.taketoday.lang.Constant;

/**
 * Base class for monitoring interceptors, such as performance monitors.
 * Provides configurable "prefix and "suffix" properties that help to
 * classify/group performance monitoring results.
 *
 * <p>In their {@link #invokeUnderTrace} implementation, subclasses should call the
 * {@link #createInvocationTraceName} method to create a name for the given trace,
 * including information about the method invocation along with a prefix/suffix.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author TODAY
 * @see #setPrefix
 * @see #setSuffix
 * @see #createInvocationTraceName
 * @since 3.0
 */
@SuppressWarnings("serial")
public abstract class AbstractMonitoringInterceptor extends AbstractTraceInterceptor {

  private String prefix = Constant.BLANK;
  private String suffix = Constant.BLANK;
  private boolean logTargetClassInvocation = false;

  /**
   * Set the text that will get appended to the trace data.
   * <p>Default is none.
   */
  public void setPrefix(String prefix) {
    this.prefix = (prefix != null ? prefix : "");
  }

  /**
   * Return the text that will get appended to the trace data.
   */
  protected String getPrefix() {
    return this.prefix;
  }

  /**
   * Set the text that will get prepended to the trace data.
   * <p>Default is none.
   */
  public void setSuffix(String suffix) {
    this.suffix = (suffix != null ? suffix : "");
  }

  /**
   * Return the text that will get prepended to the trace data.
   */
  protected String getSuffix() {
    return this.suffix;
  }

  /**
   * Set whether to log the invocation on the target class, if applicable
   * (i.e. if the method is actually delegated to the target class).
   * <p>Default is "false", logging the invocation based on the proxy
   * interface/class name.
   */
  public void setLogTargetClassInvocation(boolean logTargetClassInvocation) {
    this.logTargetClassInvocation = logTargetClassInvocation;
  }

  /**
   * Create a {@code String} name for the given {@code MethodInvocation}
   * that can be used for trace/logging purposes. This name is made up of the
   * configured prefix, followed by the fully-qualified name of the method being
   * invoked, followed by the configured suffix.
   *
   * @see #setPrefix
   * @see #setSuffix
   */
  protected String createInvocationTraceName(MethodInvocation invocation) {
    Method method = invocation.getMethod();
    Class<?> clazz = method.getDeclaringClass();
    if (this.logTargetClassInvocation && clazz.isInstance(invocation.getThis())) {
      clazz = invocation.getThis().getClass();
    }
    String className = clazz.getName();
    return getPrefix() + className + '.' + method.getName() + getSuffix();
  }

}

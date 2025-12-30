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

package infra.context.event;

import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInvocation;

import infra.context.ApplicationEvent;
import infra.util.ClassUtils;

/**
 * Event indicating a method invocation that failed.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see EventPublicationInterceptor
 * @since 5.0
 */
@SuppressWarnings("serial")
public class MethodFailureEvent extends ApplicationEvent {

  private final Throwable failure;

  /**
   * Create a new event for the given method invocation.
   *
   * @param invocation the method invocation
   * @param failure the exception encountered
   */
  public MethodFailureEvent(MethodInvocation invocation, Throwable failure) {
    super(invocation);
    this.failure = failure;
  }

  /**
   * Return the method invocation that triggered this event.
   */
  @Override
  public MethodInvocation getSource() {
    return (MethodInvocation) super.getSource();
  }

  /**
   * Return the method that triggered this event.
   */
  public Method getMethod() {
    return getSource().getMethod();
  }

  /**
   * Return the exception encountered.
   */
  public Throwable getFailure() {
    return this.failure;
  }

  @Override
  public String toString() {
    return "%s: %s [%s]".formatted(getClass().getSimpleName(), ClassUtils.getQualifiedMethodName(getMethod()), getFailure());
  }

}

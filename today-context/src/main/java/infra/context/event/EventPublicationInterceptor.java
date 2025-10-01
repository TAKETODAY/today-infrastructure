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

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.function.Supplier;

import infra.beans.factory.InitializingBean;
import infra.context.ApplicationEvent;
import infra.context.ApplicationEventPublisher;
import infra.context.ApplicationEventPublisherAware;
import infra.context.ApplicationListener;
import infra.lang.Assert;

/**
 * {@link MethodInterceptor Interceptor} that publishes an
 * {@code ApplicationEvent} to all {@code ApplicationListeners}
 * registered with an {@code ApplicationEventPublisher} after each
 * <i>successful</i> method invocation.
 *
 * @author Dmitriy Kopylenko
 * @author Juergen Hoeller
 * @author Rick Evans
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #setApplicationEventClass
 * @see ApplicationEvent
 * @see ApplicationListener
 * @see ApplicationEventPublisher
 * @see infra.context.ApplicationContext
 * @since 4.0 2021/12/3 10:11
 */
public class EventPublicationInterceptor implements MethodInterceptor, ApplicationEventPublisherAware, InitializingBean {

  @Nullable
  private Constructor<?> applicationEventConstructor;

  @Nullable
  private ApplicationEventPublisher applicationEventPublisher;

  @Nullable
  private Supplier<?> applicationEventSupplier;

  /**
   * Set the application event class to publish.
   * <p>The event class <b>must</b> have a constructor with a single
   * {@code Object} argument for the event source. The interceptor
   * will pass in the invoked object.
   *
   * @throws IllegalArgumentException if the supplied {@code Class} is
   * {@code null} or if it is not an {@code ApplicationEvent} subclass or
   * if it does not expose a constructor that takes a single {@code Object} argument
   */
  public void setApplicationEventClass(Class<?> applicationEventClass) {
    if (Modifier.isAbstract(applicationEventClass.getModifiers())) {
      throw new IllegalArgumentException("'applicationEventClass' cannot be abstract");
    }
    try {
      this.applicationEventConstructor = applicationEventClass.getConstructor(Object.class);
    }
    catch (NoSuchMethodException ex) {
      throw new IllegalArgumentException("ApplicationEvent class [%s] does not have the required Object constructor: %s"
              .formatted(applicationEventClass.getName(), ex));
    }
  }

  /**
   * Set the event object supplier.
   */
  public void setApplicationEventSupplier(@Nullable Supplier<?> applicationEventSupplier) {
    this.applicationEventSupplier = applicationEventSupplier;
  }

  @Override
  public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
    this.applicationEventPublisher = applicationEventPublisher;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    if (applicationEventConstructor == null && applicationEventSupplier == null) {
      throw new IllegalArgumentException("applicationEventConstructor and applicationEventSupplier must not be null at same time");
    }
  }

  @Override
  @Nullable
  public Object invoke(MethodInvocation invocation) throws Throwable {
    Object retVal = invocation.proceed();

    Assert.state(applicationEventPublisher != null, "No ApplicationEventPublisher available");

    Object event = getEventObject(invocation);
    applicationEventPublisher.publishEvent(event);
    return retVal;
  }

  private Object getEventObject(MethodInvocation invocation) throws Exception {
    if (applicationEventSupplier != null) {
      return applicationEventSupplier.get();
    }
    if (applicationEventConstructor != null) {
      return applicationEventConstructor.newInstance(invocation.getThis());
    }
    throw new IllegalStateException("Event object cannot be determined");
  }

}

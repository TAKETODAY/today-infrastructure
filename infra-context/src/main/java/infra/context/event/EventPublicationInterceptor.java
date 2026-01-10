/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.context.event;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Constructor;

import infra.beans.BeanUtils;
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

  private ApplicationEventFactory applicationEventFactory = (invocation, returnValue) -> null;

  private @Nullable ApplicationEventPublisher applicationEventPublisher;

  /**
   * Set the application event class to publish after each successful invocation.
   * <p>The event class <b>must</b> have a constructor with a single
   * {@code Object} argument for the event source. The interceptor
   * will pass in the invoked object.
   *
   * @throws IllegalArgumentException if the supplied {@code Class} is
   * {@code null} or if it is not an {@code ApplicationEvent} subclass or
   * if it does not expose a constructor that takes a single {@code Object} argument
   * @see #setApplicationEventFactory
   */
  public void setApplicationEventClass(Class<? extends ApplicationEvent> applicationEventClass) {
    if (ApplicationEvent.class == applicationEventClass
            || !ApplicationEvent.class.isAssignableFrom(applicationEventClass)) {
      throw new IllegalArgumentException("'applicationEventClass' needs to extend ApplicationEvent");
    }
    try {
      Constructor<? extends ApplicationEvent> ctor = applicationEventClass.getConstructor(Object.class);
      this.applicationEventFactory = ((invocation, returnValue) ->
              BeanUtils.newInstance(ctor, invocation.getThis()));
    }
    catch (NoSuchMethodException ex) {
      throw new IllegalArgumentException("ApplicationEvent class [" +
              applicationEventClass.getName() + "] does not have the required Object constructor: " + ex);
    }
  }

  /**
   * Specify a factory function for {@link ApplicationEvent} instances built from a
   * {@link MethodInvocation}, representing each <i>successful</i> method invocation.
   *
   * @see #setApplicationEventClass
   * @since 5.0
   */
  public void setApplicationEventFactory(ApplicationEventFactory applicationEventFactory) {
    this.applicationEventFactory = applicationEventFactory;
  }

  @Override
  public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
    this.applicationEventPublisher = applicationEventPublisher;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    if (this.applicationEventPublisher == null) {
      throw new IllegalArgumentException("Property 'applicationEventPublisher' is required");
    }
  }

  @Override
  public @Nullable Object invoke(MethodInvocation invocation) throws Throwable {
    Assert.state(this.applicationEventPublisher != null, "No ApplicationEventPublisher available");

    Object retVal;
    try {
      retVal = invocation.proceed();
    }
    catch (Throwable ex) {
      // Publish event after failed invocation.
      ApplicationEvent event = this.applicationEventFactory.onFailure(invocation, ex);
      if (event != null) {
        this.applicationEventPublisher.publishEvent(event);
      }
      throw ex;
    }

    // Publish event after successful invocation.
    ApplicationEvent event = this.applicationEventFactory.onSuccess(invocation, retVal);
    if (event != null) {
      this.applicationEventPublisher.publishEvent(event);
    }
    return retVal;
  }

  /**
   * Callback interface for building an {@link ApplicationEvent} after a method invocation.
   *
   * @since 5.0
   */
  @FunctionalInterface
  public interface ApplicationEventFactory {

    /**
     * Build an {@link ApplicationEvent} for the given successful method invocation.
     * <p>This is the primary method to implement since there is no such default event.
     * This may also return {@code null} for not publishing an event on success at all.
     *
     * @param invocation the successful method invocation
     * @param returnValue the value that the method returned, if any
     * @return the event to publish, or {@code null} for none
     */
    @Nullable
    ApplicationEvent onSuccess(MethodInvocation invocation, @Nullable Object returnValue);

    /**
     * Build an {@link ApplicationEvent} for the given failed method invocation.
     * <p>The default implementation builds a common {@link MethodFailureEvent}.
     * This can be overridden to build a custom event instead, or to return
     * {@code null} for not publishing an event on failure at all.
     *
     * @param invocation the failed method invocation
     * @param failure the exception thrown from the method
     * @return the event to publish, or {@code null} for none
     */
    default @Nullable ApplicationEvent onFailure(MethodInvocation invocation, Throwable failure) {
      return new MethodFailureEvent(invocation, failure);
    }
  }

}

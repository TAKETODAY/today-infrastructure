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
package cn.taketoday.context.event;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EventObject;
import java.util.List;
import java.util.concurrent.CompletionStage;

import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.beans.ArgumentsResolver;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.expression.AnnotatedElementKey;
import cn.taketoday.core.ConfigurationException;
import cn.taketoday.core.Order;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.ReactiveAdapter;
import cn.taketoday.core.ReactiveAdapterRegistry;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.core.reflect.MethodInvoker;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.util.concurrent.ListenableFuture;

/**
 * @author TODAY 2021/11/5 11:51
 * @since 4.0
 */
public class ApplicationListenerMethodAdapter implements GenericApplicationListener<Object>, Ordered {
  private static final Logger log = LoggerFactory.getLogger(ApplicationListenerMethodAdapter.class);

  private static final boolean reactiveStreamsPresent = ClassUtils.isPresent(
          "org.reactivestreams.Publisher", MethodEventDrivenPostProcessor.class.getClassLoader());

  private final Method targetMethod;
  private MethodInvoker methodInvoker;

  private ApplicationContext context;
  private EventExpressionEvaluator evaluator;
  private ArgumentsResolver argumentsResolver;

  @Nullable
  private final String condition;
  private final String beanName;

  private final int order;

  @Nullable
  private final String listenerId;

  private final List<ResolvableType> declaredEventTypes;

  private final AnnotatedElementKey methodKey;

  /**
   * Construct a new MethodApplicationListener.
   *
   * @param beanName the name of the bean to invoke the listener method on
   * @param targetClass the target class that the method is declared on
   * @param method the listener method to invoke
   */
  public ApplicationListenerMethodAdapter(String beanName, Class<?> targetClass, Method method) {
    this.beanName = beanName;
    this.targetMethod = !Proxy.isProxyClass(targetClass)
                        ? AopUtils.getMostSpecificMethod(method, targetClass) : method;
    this.methodKey = new AnnotatedElementKey(this.targetMethod, targetClass);

    MergedAnnotations annotations = MergedAnnotations.from(targetMethod);

    MergedAnnotation<Order> order = annotations.get(Order.class);
    this.order = order.getValue(Integer.class)
            .orElse(Ordered.LOWEST_PRECEDENCE);

    MergedAnnotation<EventListener> annotation = annotations.get(EventListener.class);
    this.declaredEventTypes = getEventTypes(annotation, targetMethod);

    this.condition = annotation.getValue("condition", String.class)
            .filter(StringUtils::hasText)
            .orElse(null);
    this.listenerId = annotation.isPresent() ? annotation.getString("id") : null;

  }

  protected void init(ApplicationContext context, EventExpressionEvaluator evaluator) {
    this.context = context;
    this.evaluator = evaluator;
    this.argumentsResolver = targetMethod.getParameterCount() == 0
                             ? null
                             : context.getArgumentsResolver();

  }

  protected List<ResolvableType> getEventTypes(MergedAnnotation<EventListener> eventListener, Method declaredMethod) {
    return getEventTypes(eventListener.getClassArray(MergedAnnotation.VALUE), declaredMethod);
  }

  protected List<ResolvableType> getEventTypes(Class<?>[] eventTypes, Method declaredMethod) {
    if (ObjectUtils.isNotEmpty(eventTypes)) {
      ArrayList<ResolvableType> types = new ArrayList<>(eventTypes.length);
      for (Class<?> eventType : eventTypes) {
        types.add(ResolvableType.fromClass(eventType));
      }
      return types;
    }
    Class<?>[] parameterTypes = declaredMethod.getParameterTypes();
    if (parameterTypes.length == 0) {
      throw new ConfigurationException("cannot determine event type on method: " + declaredMethod);
    }
    else if (parameterTypes.length == 1) {
      return Collections.singletonList(ResolvableType.forParameter(declaredMethod, 0));
    }
    else {
      // search EventObject
      int idx = 0;
      for (Class<?> parameterType : parameterTypes) {
        // lookup EventObject
        if (EventObject.class.isAssignableFrom(parameterType)) {
          idx++;
          break;
        }
      }
      return Collections.singletonList(ResolvableType.forParameter(declaredMethod, idx));
    }
  }

  @Override
  public int getOrder() {
    return order;
  }

  /**
   * Return the target bean instance to use.
   */
  protected Object getTargetBean() {
    Assert.state(context != null, "No ApplicationContext set");
    return context.getBean(beanName);
  }

  /**
   * Return the target listener method.
   */
  protected Method getTargetMethod() {
    return this.targetMethod;
  }

  /**
   * Return the condition to use.
   * <p>Matches the {@code condition} attribute of the {@link EventListener}
   * annotation or any matching attribute on a composed annotation that
   * is meta-annotated with {@code @EventListener}.
   */
  @Nullable
  protected String getCondition() {
    return this.condition;
  }

  @Override
  public boolean supportsEventType(ResolvableType eventType) {
    for (ResolvableType declaredEventType : this.declaredEventTypes) {
      if (declaredEventType.isAssignableFrom(eventType)) {
        return true;
      }
    }
    return eventType.hasUnresolvableGenerics();
  }

  @Override
  public boolean supportsSourceType(@Nullable Class<?> sourceType) {
    return true;
  }

  /**
   * Process the specified {@link Object}, checking if the condition
   * matches and handling a non-null result, if any.
   */
  @Override
  public void onApplicationEvent(Object event) { // any event type
    Object[] parameter = resolveArguments(argumentsResolver, event);
    if (shouldInvoke(event, parameter)) {
      if (methodInvoker == null) {
        methodInvoker = MethodInvoker.fromMethod(targetMethod);
      }
      Object result = methodInvoker.invoke(getTargetBean(), parameter);
      if (result != null) {
        handleResult(result);
      }
      else {
        log.trace("No result object given - no result to handle");
      }
    }
  }

  private boolean shouldInvoke(Object event, @Nullable Object[] args) {
    if (condition != null) {
      return evaluator.condition(
              condition, event, targetMethod, methodKey, args, context);
    }
    return true;
  }

  @Nullable
  private Object[] resolveArguments(ArgumentsResolver resolver, Object event) {
    if (resolver != null) {
      return resolver.resolve(targetMethod, context, new Object[] { event });
    }
    return null;
  }

  @Override
  @Nullable
  public String getListenerId() {
    return listenerId;
  }

  protected void handleResult(Object result) {
    if (reactiveStreamsPresent && ReactiveDelegate.subscribeToPublisher(this, result)) {
      if (log.isTraceEnabled()) {
        log.trace("Adapted to reactive result: {}", result);
      }
    }
    else if (result instanceof CompletionStage) {
      ((CompletionStage<?>) result).whenComplete((event, ex) -> {
        if (ex != null) {
          handleAsyncError(ex);
        }
        else if (event != null) {
          publishEvent(event);
        }
      });
    }
    else if (result instanceof ListenableFuture) {
      ((ListenableFuture<?>) result).addCallback(this::publishEvents, this::handleAsyncError);
    }
    else {
      publishEvents(result);
    }
  }

  private void publishEvents(Object result) {
    if (result.getClass().isArray()) {
      Object[] events = ObjectUtils.toObjectArray(result);
      for (Object event : events) {
        publishEvent(event);
      }
    }
    else if (result instanceof Collection<?> events) {
      for (Object event : events) {
        publishEvent(event);
      }
    }
    else {
      publishEvent(result);
    }
  }

  private void publishEvent(@Nullable Object event) {
    if (event != null) {
      context.publishEvent(event);
    }
  }

  protected void handleAsyncError(Throwable t) {
    log.error("Unexpected error occurred in asynchronous listener", t);
  }

  private static class ReactiveDelegate {

    public static boolean subscribeToPublisher(ApplicationListenerMethodAdapter listener, Object result) {
      ReactiveAdapter adapter = ReactiveAdapterRegistry.getSharedInstance().getAdapter(result.getClass());
      if (adapter != null) {
        adapter.toPublisher(result).subscribe(new EventPublicationSubscriber(listener));
        return true;
      }
      return false;
    }
  }

  private static record EventPublicationSubscriber(ApplicationListenerMethodAdapter listener)
          implements Subscriber<Object> {

    @Override
    public void onSubscribe(Subscription s) {
      s.request(Integer.MAX_VALUE);
    }

    @Override
    public void onNext(Object o) {
      listener.publishEvents(o);
    }

    @Override
    public void onError(Throwable t) {
      listener.handleAsyncError(t);
    }

    @Override
    public void onComplete() { }
  }

}

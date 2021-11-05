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

import cn.taketoday.beans.ArgumentsResolver;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.expression.ExpressionEvaluator;
import cn.taketoday.core.ReactiveAdapter;
import cn.taketoday.core.ReactiveAdapterRegistry;
import cn.taketoday.core.reflect.MethodInvoker;
import cn.taketoday.expression.ExpressionContext;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.util.concurrent.ListenableFuture;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

/**
 * @author TODAY 2021/11/5 11:51
 * @since 4.0
 */
public class MethodApplicationListener implements ApplicationListener<Object>, EventProvider {
  private static final Logger log = LoggerFactory.getLogger(MethodApplicationListener.class);

  private static final boolean reactiveStreamsPresent = ClassUtils.isPresent(
          "org.reactivestreams.Publisher", MethodEventDrivenPostProcessor.class.getClassLoader());

  private final Method targetMethod;
  private final Class<?>[] eventTypes;
  private final BeanFactory beanFactory;
  private final MethodInvoker methodInvoker;
  private final Supplier<Object> beanSupplier;
  private final ArgumentsResolver argumentsResolver;

  private final ApplicationContext context;

  @Nullable
  private final String condition;
  private final ExpressionEvaluator evaluator;

  MethodApplicationListener(
          Supplier<Object> beanSupplier,
          Method targetMethod, Class<?>[] eventTypes, BeanFactory beanFactory, ApplicationContext context, @Nullable String condition) {
    this.beanSupplier = beanSupplier;
    this.eventTypes = eventTypes;
    this.beanFactory = beanFactory;
    this.targetMethod = targetMethod;
    this.methodInvoker = MethodInvoker.fromMethod(targetMethod);
    this.context = context;

    this.argumentsResolver = targetMethod.getParameterCount() == 0
            ? null
            : beanFactory.getArgumentsResolver();
    if (StringUtils.hasText(condition)) {
      this.condition = condition;
    }
    else {
      this.condition = null;
    }
    this.evaluator = context.getExpressionEvaluator();
  }

  @Override
  public void onApplicationEvent(Object event) { // any event type
    Object[] parameter = resolveArguments(argumentsResolver, event);
    if (shouldHandle(event, parameter)) {
      Object result = methodInvoker.invoke(beanSupplier.get(), parameter);
      if (result != null) {
        handleResult(result);
      }
      else {
        log.trace("No result object given - no result to handle");
      }
    }
  }

  private boolean shouldHandle(Object event, @Nullable Object[] args) {
    if (condition != null) {
      ExpressionContext parentExpressionContext = evaluator.getParentExpressionContext();
      EventExpressionContext context = new EventExpressionContext(parentExpressionContext);
      HashMap<String, Object> beans = new HashMap<>();
      // TODO condition args name mapping
      beans.put(Constant.KEY_ROOT, new EventRootObject(event, args));
      return evaluator.evaluate(condition, context, boolean.class);
    }
    return true;
  }


  static class EventRootObject {
    final Object event;
    final Object[] args;

    EventRootObject(Object event, Object[] args) {
      this.event = event;
      this.args = args;
    }

    public Object getEvent() {
      return event;
    }

    public Object[] getArgs() {
      return args;
    }

  }

  @Nullable
  private Object[] resolveArguments(ArgumentsResolver resolver, Object event) {
    if (resolver != null) {
      return resolver.resolve(targetMethod, beanFactory, new Object[] { event });
    }
    return null;
  }

  @Override
  public Class<?>[] getSupportedEvent() {
    return eventTypes;
  }


  protected void handleResult(Object result) {
    if (reactiveStreamsPresent && new ReactiveResultHandler().subscribeToPublisher(result)) {
      if (log.isTraceEnabled()) {
        log.trace("Adapted to reactive result: " + result);
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
    else if (result instanceof Collection<?>) {
      Collection<?> events = (Collection<?>) result;
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

  private class ReactiveResultHandler {

    public boolean subscribeToPublisher(Object result) {
      ReactiveAdapter adapter = ReactiveAdapterRegistry.getSharedInstance().getAdapter(result.getClass());
      if (adapter != null) {
        adapter.toPublisher(result).subscribe(new EventPublicationSubscriber());
        return true;
      }
      return false;
    }
  }

  private class EventPublicationSubscriber implements Subscriber<Object> {

    @Override
    public void onSubscribe(Subscription s) {
      s.request(Integer.MAX_VALUE);
    }

    @Override
    public void onNext(Object o) {
      publishEvents(o);
    }

    @Override
    public void onError(Throwable t) {
      handleAsyncError(t);
    }

    @Override
    public void onComplete() {
    }
  }

}

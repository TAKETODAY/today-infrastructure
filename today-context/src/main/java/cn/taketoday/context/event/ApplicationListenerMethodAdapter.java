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

package cn.taketoday.context.event;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.CompletionStage;

import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationEvent;
import cn.taketoday.context.PayloadApplicationEvent;
import cn.taketoday.context.expression.AnnotatedElementKey;
import cn.taketoday.core.BridgeMethodResolver;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.ReactiveAdapter;
import cn.taketoday.core.ReactiveAdapterRegistry;
import cn.taketoday.core.ReactiveStreams;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.core.annotation.MergedAnnotations.SearchStrategy;
import cn.taketoday.core.annotation.Order;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.util.concurrent.Future;
import cn.taketoday.util.concurrent.FutureListener;

/**
 * {@link GenericApplicationListener} adapter that delegates the processing of
 * an event to an {@link EventListener} annotated method.
 *
 * <p>Delegates to {@link #processEvent(ApplicationEvent)} to give subclasses
 * a chance to deviate from the default. Unwraps the content of a
 * {@link PayloadApplicationEvent} if necessary to allow a method declaration
 * to define any arbitrary event type. If a condition is defined, it is
 * evaluated prior to invoking the underlying method.
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/11/5 11:51
 */
public class ApplicationListenerMethodAdapter implements GenericApplicationListener, Ordered, FutureListener<Future<?>> {

  private static final Logger log = LoggerFactory.getLogger(ApplicationListenerMethodAdapter.class);

  private final Method method;

  private final Method targetMethod;

  /**
   * <p>Matches the {@code condition} attribute of the {@link EventListener}
   * annotation or any matching attribute on a composed annotation that
   * is meta-annotated with {@code @EventListener}.
   */
  @Nullable
  protected final String condition;

  private final String beanName;

  private final int order;

  /**
   * Whether default execution is applicable for the target listener.
   *
   * @see #onApplicationEvent
   * @see EventListener#defaultExecution()
   */
  protected final boolean defaultExecution;

  private final List<ResolvableType> declaredEventTypes;

  private final AnnotatedElementKey methodKey;

  @Nullable
  private volatile String listenerId;

  private ApplicationContext context;

  @Nullable
  private EventExpressionEvaluator evaluator;

  /**
   * Construct a new MethodApplicationListener.
   *
   * @param beanName the name of the bean to invoke the listener method on
   * @param targetClass the target class that the method is declared on
   * @param method the listener method to invoke
   */
  public ApplicationListenerMethodAdapter(String beanName, Class<?> targetClass, Method method) {
    this.beanName = beanName;
    this.method = BridgeMethodResolver.findBridgedMethod(method);
    this.targetMethod = Proxy.isProxyClass(targetClass)
            ? this.method : AopUtils.getMostSpecificMethod(method, targetClass);
    this.methodKey = new AnnotatedElementKey(this.targetMethod, targetClass);

    MergedAnnotations annotations = MergedAnnotations.from(targetMethod, SearchStrategy.TYPE_HIERARCHY);
    MergedAnnotation<EventListener> annotation = annotations.get(EventListener.class);
    this.declaredEventTypes = resolveDeclaredEventTypes(method, annotation);

    this.condition = annotation.getValue("condition", String.class)
            .filter(StringUtils::hasText)
            .orElse(null);

    this.defaultExecution = annotation.getBoolean("defaultExecution");
    if (annotation.isPresent()) {
      String id = annotation.getString("id");
      this.listenerId = !id.isEmpty() ? id : null;
    }

    MergedAnnotation<Order> order = annotations.get(Order.class);
    this.order = order.getValue(Integer.class)
            .orElse(Ordered.LOWEST_PRECEDENCE);

  }

  protected void init(ApplicationContext context, @Nullable EventExpressionEvaluator evaluator) {
    this.context = context;
    this.evaluator = evaluator;
  }

  private static List<ResolvableType> resolveDeclaredEventTypes(
          Method method, @Nullable MergedAnnotation<EventListener> ann) {
    int count = method.getParameterCount();
    if (count > 1) {
      throw new IllegalStateException(
              "Maximum one parameter is allowed for event listener method: " + method);
    }
    if (ann != null) {
      Class<?>[] classes = ann.getClassArray("event");
      if (classes.length > 0) {
        ArrayList<ResolvableType> types = new ArrayList<>(classes.length);
        for (Class<?> eventType : classes) {
          types.add(ResolvableType.forClass(eventType));
        }
        return types;
      }
    }

    if (count == 0) {
      throw new IllegalStateException(
              "Event parameter is mandatory for event listener method: " + method);
    }
    return Collections.singletonList(ResolvableType.forParameter(method, 0));
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

  @Override
  public boolean supportsEventType(ResolvableType eventType) {
    for (ResolvableType declaredEventType : this.declaredEventTypes) {
      if (eventType.hasUnresolvableGenerics() ?
              declaredEventType.toClass().isAssignableFrom(eventType.toClass()) :
              declaredEventType.isAssignableFrom(eventType)) {
        return true;
      }
      if (PayloadApplicationEvent.class.isAssignableFrom(eventType.toClass())) {
        ResolvableType payloadType = eventType.as(PayloadApplicationEvent.class).getGeneric();
        if (declaredEventType.isAssignableFrom(payloadType)) {
          return true;
        }
        if (payloadType.resolve() == null) {
          // Always accept such event when the type is erased
          return true;
        }
      }
    }
    return false;
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
  public void onApplicationEvent(ApplicationEvent event) { // any event type
    if (defaultExecution) {
      processEvent(event);
    }
  }

  /**
   * Process the specified {@link ApplicationEvent}, checking if the condition
   * matches and handling a non-null result, if any.
   *
   * @param event the event to process through the listener method
   */
  public void processEvent(ApplicationEvent event) {
    Object[] args = resolveArguments(event);
    if (shouldInvoke(event, args)) {
      Object result = doInvoke(args);
      if (result != null) {
        handleResult(result);
      }
      else {
        log.trace("No result object given - no result to handle");
      }
    }
  }

  private boolean shouldInvoke(Object event, @Nullable Object[] args) {
    if (args == null) {
      return false;
    }
    if (condition != null) {
      Assert.notNull(this.evaluator, "EventExpressionEvaluator is required");
      return this.evaluator.condition(
              condition, event, this.targetMethod, this.methodKey, args);
    }
    return true;
  }

  /**
   * Resolve the method arguments to use for the specified {@link ApplicationEvent}.
   * <p>These arguments will be used to invoke the method handled by this instance.
   * Can return {@code null} to indicate that no suitable arguments could be resolved
   * and therefore the method should not be invoked at all for the specified event.
   */
  @Nullable
  protected Object[] resolveArguments(ApplicationEvent event) {
    ResolvableType declaredEventType = getResolvableType(event);
    if (declaredEventType == null) {
      return null;
    }
    if (method.getParameterCount() == 0) {
      return Constant.EMPTY_OBJECTS;
    }
    Class<?> declaredEventClass = declaredEventType.toClass();
    if (!ApplicationEvent.class.isAssignableFrom(declaredEventClass)
            && event instanceof PayloadApplicationEvent) {
      Object payload = ((PayloadApplicationEvent<?>) event).getPayload();
      if (declaredEventClass.isInstance(payload)) {
        return new Object[] { payload };
      }
    }
    return new Object[] { event };
  }

  @Nullable
  private ResolvableType getResolvableType(ApplicationEvent event) {
    ResolvableType payloadType = null;
    if (event instanceof PayloadApplicationEvent<?> payloadEvent) {
      ResolvableType eventType = payloadEvent.getResolvableType();
      if (eventType != null) {
        payloadType = eventType.as(PayloadApplicationEvent.class).getGeneric();
      }
    }
    for (ResolvableType declaredEventType : this.declaredEventTypes) {
      Class<?> eventClass = declaredEventType.toClass();
      if (!ApplicationEvent.class.isAssignableFrom(eventClass) &&
              payloadType != null && declaredEventType.isAssignableFrom(payloadType)) {
        return declaredEventType;
      }
      if (eventClass.isInstance(event)) {
        return declaredEventType;
      }
    }
    return null;
  }

  /**
   * Invoke the event listener method with the given argument values.
   */
  @Nullable
  protected Object doInvoke(Object[] args) {
    Object bean = getTargetBean();
    // Detect package-protected NullBean instance through equals(null) check
    if (bean == null) {
      return null;
    }
    ReflectionUtils.makeAccessible(method);
    try {
      return method.invoke(bean, args);
    }
    catch (IllegalArgumentException ex) {
      assertTargetBean(method, bean, args);
      throw new IllegalStateException(getInvocationErrorMessage(bean, ex.getMessage(), args), ex);
    }
    catch (IllegalAccessException ex) {
      throw new IllegalStateException(getInvocationErrorMessage(bean, ex.getMessage(), args), ex);
    }
    catch (InvocationTargetException ex) {
      // Throw underlying exception
      Throwable targetException = ex.getTargetException();
      if (targetException instanceof RuntimeException) {
        throw (RuntimeException) targetException;
      }
      else {
        String msg = getInvocationErrorMessage(bean, "Failed to invoke event listener method", args);
        throw new UndeclaredThrowableException(targetException, msg);
      }
    }
  }

  @Override
  @NonNull
  public String getListenerId() {
    String id = this.listenerId;
    if (id == null) {
      id = getDefaultListenerId();
      this.listenerId = id;
    }
    return id;
  }

  /**
   * Determine the default id for the target listener, to be applied in case of
   * no {@link EventListener#id() annotation-specified id value}.
   * <p>The default implementation builds a method name with parameter types.
   *
   * @see #getListenerId()
   */
  protected String getDefaultListenerId() {
    Method method = getTargetMethod();
    StringJoiner sj = new StringJoiner(",", "(", ")");
    for (Class<?> paramType : method.getParameterTypes()) {
      sj.add(paramType.getName());
    }
    return ClassUtils.getQualifiedMethodName(method) + sj;
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  protected void handleResult(Object result) {
    if (ReactiveStreams.isPresent && ReactiveDelegate.subscribeToPublisher(this, result)) {
      if (log.isTraceEnabled()) {
        log.trace("Adapted to reactive result: {}", result);
      }
    }
    else if (result instanceof CompletionStage<?> stage) {
      stage.whenComplete((event, ex) -> {
        if (ex != null) {
          handleAsyncError(ex);
        }
        else if (event != null) {
          publishEvents(event);
        }
      });
    }
    else if (result instanceof Future d) {
      d.onCompleted(this);
    }
    else {
      publishEvents(result);
    }
  }

  @Override
  public void operationComplete(Future<?> future) {
    Throwable cause = future.getCause();
    if (cause != null) {
      handleAsyncError(cause);
    }
    else {
      publishEvents(future.obtain());
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

  /**
   * Add additional details such as the bean type and method signature to
   * the given error message.
   *
   * @param message error message to append the HandlerMethod details to
   */
  protected String getDetailedErrorMessage(Object bean, String message) {
    return "%s\nHandlerMethod details: \nBean [%s]\nMethod [%s]\n"
            .formatted(message, bean.getClass().getName(), this.targetMethod.toGenericString());
  }

  /**
   * Assert that the target bean class is an instance of the class where the given
   * method is declared. In some cases the actual bean instance at event-
   * processing time may be a JDK dynamic proxy (lazy initialization, prototype
   * beans, and others). Event listener beans that require proxying should prefer
   * class-based proxy mechanisms.
   */
  private void assertTargetBean(Method method, Object targetBean, Object[] args) {
    Class<?> methodDeclaringClass = method.getDeclaringClass();
    Class<?> targetBeanClass = targetBean.getClass();
    if (!methodDeclaringClass.isAssignableFrom(targetBeanClass)) {
      String msg = ("The event listener method class '%s' is not an instance of the actual bean class '%s'. " +
              "If the bean requires proxying (e.g. due to @Transactional), please use class-based proxying.")
              .formatted(methodDeclaringClass.getName(), targetBeanClass.getName());
      throw new IllegalStateException(getInvocationErrorMessage(targetBean, msg, args));
    }
  }

  private String getInvocationErrorMessage(Object bean, String message, Object[] resolvedArgs) {
    StringBuilder sb = new StringBuilder(getDetailedErrorMessage(bean, message));
    sb.append("Resolved arguments: \n");
    for (int i = 0; i < resolvedArgs.length; i++) {
      sb.append('[').append(i).append("] ");
      if (resolvedArgs[i] == null) {
        sb.append("[null] \n");
      }
      else {
        sb.append("[type=").append(resolvedArgs[i].getClass().getName()).append("] ");
        sb.append("[value=").append(resolvedArgs[i]).append("]\n");
      }
    }
    return sb.toString();
  }

  /**
   * Inner class to avoid a hard dependency on the Reactive Streams API at runtime.
   */
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

  /**
   * Reactive Streams Subscriber for publishing follow-up events.
   */
  private record EventPublicationSubscriber(ApplicationListenerMethodAdapter listener)
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

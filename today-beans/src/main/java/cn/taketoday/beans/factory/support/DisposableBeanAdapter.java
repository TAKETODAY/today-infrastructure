/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.beans.factory.support;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import cn.taketoday.beans.factory.BeanDefinitionValidationException;
import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.beans.factory.config.DestructionAwareBeanPostProcessor;
import cn.taketoday.core.ReactiveAdapter;
import cn.taketoday.core.ReactiveAdapterRegistry;
import cn.taketoday.core.ReactiveStreams;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * Adapter that implements the {@link DisposableBean} and {@link Runnable}
 * interfaces performing various destruction steps on a given bean instance:
 * <ul>
 * <li>DestructionBeanPostProcessors;
 * <li>the bean implementing DisposableBean itself;
 * <li>a custom destroy method specified on the bean definition.
 * </ul>
 *
 * @author Juergen Hoeller
 * @author Costin Leau
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see AbstractBeanFactory
 * @see DisposableBean
 * @see DestructionAwareBeanPostProcessor
 * @since 4.0 2021/10/24 22:56
 */
final class DisposableBeanAdapter implements DisposableBean, Runnable, Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private static final Logger log = LoggerFactory.getLogger(DisposableBeanAdapter.class);

  private static final String CLOSE_METHOD_NAME = "close";
  private static final String DESTROY_METHOD_NAME = "destroy";
  private static final String SHUTDOWN_METHOD_NAME = "shutdown";

  private final Object bean;
  private final String beanName;

  private final boolean nonPublicAccessAllowed;
  private final boolean invokeDisposableBean;

  private boolean invokeAutoCloseable;

  @Nullable
  private String[] destroyMethodNames;

  @Nullable
  private transient Method[] destroyMethods;

  @Nullable
  private final List<DestructionAwareBeanPostProcessor> beanPostProcessors;

  /**
   * Create a new DisposableBeanAdapter for the given bean.
   *
   * @param bean the bean instance (never {@code null})
   * @param beanName the name of the bean
   * @param beanDefinition the merged bean definition
   * @param postProcessors the List of BeanPostProcessors
   * (potentially DestructionAwareBeanPostProcessor), if any
   */
  public DisposableBeanAdapter(String beanName, Object bean, RootBeanDefinition beanDefinition,
          List<DestructionAwareBeanPostProcessor> postProcessors) {

    Assert.notNull(bean, "Disposable bean is required");
    this.bean = bean;
    this.beanName = beanName;
    this.nonPublicAccessAllowed = beanDefinition.isNonPublicAccessAllowed();
    this.invokeDisposableBean = bean instanceof DisposableBean
            && !beanDefinition.hasAnyExternallyManagedDestroyMethod(DESTROY_METHOD_NAME);

    String[] destroyMethodNames = inferDestroyMethodsIfNecessary(bean.getClass(), beanDefinition);
    if (ObjectUtils.isNotEmpty(destroyMethodNames)
            && !(invokeDisposableBean && DESTROY_METHOD_NAME.equals(destroyMethodNames[0]))
            && !beanDefinition.hasAnyExternallyManagedDestroyMethod(destroyMethodNames[0])) {

      this.invokeAutoCloseable = bean instanceof AutoCloseable && CLOSE_METHOD_NAME.equals(destroyMethodNames[0]);
      if (!invokeAutoCloseable) {
        this.destroyMethodNames = destroyMethodNames;
        var destroyMethods = new ArrayList<Method>(destroyMethodNames.length);
        for (String destroyMethodName : destroyMethodNames) {
          Method destroyMethod = determineDestroyMethod(destroyMethodName);
          if (destroyMethod == null) {
            if (beanDefinition.isEnforceDestroyMethod()) {
              throw new BeanDefinitionValidationException("Could not find a destroy method named '" +
                      destroyMethodName + "' on bean with name '" + beanName + "'");
            }
          }
          else {
            if (destroyMethod.getParameterCount() > 0) {
              Class<?>[] paramTypes = destroyMethod.getParameterTypes();
              if (paramTypes.length > 1) {
                throw new BeanDefinitionValidationException("Method '" + destroyMethodName + "' of bean '" +
                        beanName + "' has more than one parameter - not supported as destroy method");
              }
              else if (paramTypes.length == 1 && boolean.class != paramTypes[0]) {
                throw new BeanDefinitionValidationException("Method '" + destroyMethodName + "' of bean '" +
                        beanName + "' has a non-boolean parameter - not supported as destroy method");
              }
            }
            destroyMethod = ReflectionUtils.getInterfaceMethodIfPossible(destroyMethod, bean.getClass());
            destroyMethods.add(destroyMethod);
          }
        }
        this.destroyMethods = destroyMethods.toArray(new Method[0]);
      }
    }

    this.beanPostProcessors = filterPostProcessors(postProcessors, bean);
  }

  /**
   * Create a new DisposableBeanAdapter for the given bean.
   *
   * @param bean the bean instance (never {@code null})
   * @param postProcessors the List of BeanPostProcessors
   * (potentially DestructionAwareBeanPostProcessor), if any
   */
  public DisposableBeanAdapter(Object bean, List<DestructionAwareBeanPostProcessor> postProcessors) {
    Assert.notNull(bean, "Disposable bean is required");
    this.bean = bean;
    this.nonPublicAccessAllowed = true;
    this.beanName = bean.getClass().getName();
    this.invokeDisposableBean = bean instanceof DisposableBean;
    this.beanPostProcessors = filterPostProcessors(postProcessors, bean);
  }

  /**
   * Create a new DisposableBeanAdapter for the given bean.
   */
  private DisposableBeanAdapter(Object bean, String beanName,
          boolean nonPublicAccessAllowed, boolean invokeDisposableBean, boolean invokeAutoCloseable,
          @Nullable String[] destroyMethodNames, @Nullable List<DestructionAwareBeanPostProcessor> postProcessors) {

    this.bean = bean;
    this.beanName = beanName;
    this.nonPublicAccessAllowed = nonPublicAccessAllowed;
    this.invokeDisposableBean = invokeDisposableBean;
    this.invokeAutoCloseable = invokeAutoCloseable;
    this.destroyMethodNames = destroyMethodNames;
    this.beanPostProcessors = postProcessors;
  }

  @Override
  public void run() {
    destroy();
  }

  @Override
  public void destroy() {
    if (CollectionUtils.isNotEmpty(beanPostProcessors)) {
      for (DestructionAwareBeanPostProcessor processor : beanPostProcessors) {
        processor.postProcessBeforeDestruction(bean, beanName);
      }
    }

    if (this.invokeDisposableBean) {
      if (log.isTraceEnabled()) {
        log.trace("Invoking destroy() on bean with name '{}'", beanName);
      }
      try {
        ((DisposableBean) this.bean).destroy();
      }
      catch (Throwable ex) {
        if (log.isWarnEnabled()) {
          String msg = "Invocation of destroy method failed on bean with name '" + this.beanName + "'";
          if (log.isDebugEnabled()) {
            // Log at warn level like below but add the exception stacktrace only with debug level
            log.warn(msg, ex);
          }
          else {
            log.warn(msg + ": " + ex);
          }
        }
      }
    }

    if (this.invokeAutoCloseable) {
      if (log.isTraceEnabled()) {
        log.trace("Invoking close() on bean with name '{}'", beanName);
      }
      try {
        ((AutoCloseable) this.bean).close();
      }
      catch (Throwable ex) {
        if (log.isWarnEnabled()) {
          String msg = "Invocation of close method failed on bean with name '" + this.beanName + "'";
          if (log.isDebugEnabled()) {
            // Log at warn level like below but add the exception stacktrace only with debug level
            log.warn(msg, ex);
          }
          else {
            log.warn(msg + ": " + ex);
          }
        }
      }
    }
    else if (this.destroyMethods != null) {
      for (Method destroyMethod : this.destroyMethods) {
        invokeCustomDestroyMethod(destroyMethod);
      }
    }
    else if (this.destroyMethodNames != null) {
      for (String destroyMethodName : this.destroyMethodNames) {
        Method destroyMethod = determineDestroyMethod(destroyMethodName);
        if (destroyMethod != null) {
          invokeCustomDestroyMethod(
                  ReflectionUtils.getInterfaceMethodIfPossible(destroyMethod, this.bean.getClass()));
        }
      }
    }
  }

  @Nullable
  private Method determineDestroyMethod(String destroyMethodName) {
    try {
      Class<?> beanClass = this.bean.getClass();
      MethodDescriptor descriptor = MethodDescriptor.create(this.beanName, beanClass, destroyMethodName);
      String methodName = descriptor.methodName();

      Method destroyMethod = findDestroyMethod(descriptor.declaringClass(), methodName);
      if (destroyMethod != null) {
        return destroyMethod;
      }
      for (Class<?> beanInterface : beanClass.getInterfaces()) {
        destroyMethod = findDestroyMethod(beanInterface, methodName);
        if (destroyMethod != null) {
          return destroyMethod;
        }
      }
      return null;
    }
    catch (IllegalArgumentException ex) {
      throw new BeanDefinitionValidationException(
              "Could not find unique destroy method on bean with name '" + beanName + ": " + ex.getMessage());
    }
  }

  @Nullable
  private Method findDestroyMethod(Class<?> clazz, String name) {
    return nonPublicAccessAllowed ?
           ReflectionUtils.findMethodWithMinimalParameters(clazz, name) :
           ReflectionUtils.findMethodWithMinimalParameters(clazz.getMethods(), name);
  }

  /**
   * Invoke the specified custom destroy method on the given bean.
   * <p>This implementation invokes a no-arg method if found, else checking
   * for a method with a single boolean argument (passing in "true",
   * assuming a "force" parameter), else logging an error.
   */
  private void invokeCustomDestroyMethod(Method destroyMethod) {
    if (log.isTraceEnabled()) {
      log.trace("Invoking custom destroy method '{}' on bean with name '{}': {}",
              destroyMethod.getName(), beanName, destroyMethod);
    }

    int paramCount = destroyMethod.getParameterCount();
    Object[] args = new Object[paramCount];
    if (paramCount == 1) {
      args[0] = Boolean.TRUE;
    }

    try {
      ReflectionUtils.makeAccessible(destroyMethod);
      Object returnValue = destroyMethod.invoke(this.bean, args);

      if (returnValue == null) {
        // Regular case: a void method
        logDestroyMethodCompletion(destroyMethod, false);
      }
      else if (returnValue instanceof Future<?> future) {
        // An async task: await its completion.
        future.get();
        logDestroyMethodCompletion(destroyMethod, true);
      }
      else if (!ReactiveStreams.isPresent ||
              !ReactiveDestroyMethodHandler.await(this, destroyMethod, returnValue)) {
        if (log.isDebugEnabled()) {
          log.debug("Unknown return value type from custom destroy method '{}' on bean with name '{}': {}",
                  destroyMethod.getName(), beanName, returnValue.getClass());
        }
      }
    }
    catch (InvocationTargetException | ExecutionException ex) {
      logDestroyMethodException(destroyMethod, ex.getCause());
    }
    catch (Throwable ex) {
      if (log.isWarnEnabled()) {
        log.warn("Failed to invoke custom destroy method '{}' on bean with name '{}'",
                destroyMethod.getName(), beanName, ex);
      }
    }
  }

  void logDestroyMethodException(Method destroyMethod, Throwable ex) {
    if (log.isWarnEnabled()) {
      String msg = "Custom destroy method '" + destroyMethod.getName() + "' on bean with name '" +
              this.beanName + "' propagated an exception";
      if (log.isDebugEnabled()) {
        // Log at warn level like below but add the exception stacktrace only with debug level
        log.warn(msg, ex);
      }
      else {
        log.warn(msg + ": " + ex);
      }
    }
  }

  void logDestroyMethodCompletion(Method destroyMethod, boolean async) {
    if (log.isDebugEnabled()) {
      log.debug("Custom destroy method '{}' on bean with name '{}' completed{}",
              destroyMethod.getName(), beanName, (async ? " asynchronously" : ""));
    }
  }

  /**
   * Serializes a copy of the state of this class,
   * filtering out non-serializable BeanPostProcessors.
   */
  @Serial
  protected Object writeReplace() {
    ArrayList<DestructionAwareBeanPostProcessor> serializablePostProcessors = null;
    if (this.beanPostProcessors != null) {
      serializablePostProcessors = new ArrayList<>();
      for (DestructionAwareBeanPostProcessor postProcessor : this.beanPostProcessors) {
        if (postProcessor instanceof Serializable) {
          serializablePostProcessors.add(postProcessor);
        }
      }
    }
    return new DisposableBeanAdapter(
            this.bean, this.beanName, this.nonPublicAccessAllowed, this.invokeDisposableBean,
            this.invokeAutoCloseable, this.destroyMethodNames, serializablePostProcessors);
  }

  /**
   * Check whether the given bean has any kind of destroy method to call.
   *
   * @param bean the bean instance
   * @param beanDefinition the corresponding bean definition
   */
  public static boolean hasDestroyMethod(Object bean, RootBeanDefinition beanDefinition) {
    return bean instanceof DisposableBean
            || inferDestroyMethodsIfNecessary(bean.getClass(), beanDefinition) != null;
  }

  /**
   * If the current value of the given beanDefinition's "destroyMethodName" property is
   * {@link AbstractBeanDefinition#INFER_METHOD}, then attempt to infer a destroy method.
   * Candidate methods are currently limited to public, no-arg methods named "close" or
   * "shutdown" (whether declared locally or inherited). The given BeanDefinition's
   * "destroyMethodName" is updated to be null if no such method is found, otherwise set
   * to the name of the inferred method. This constant serves as the default for the
   * {@code @Bean#destroyMethod} attribute and the value of the constant may also be
   * used in XML within the {@code <bean destroy-method="">} or {@code
   * <beans default-destroy-method="">} attributes.
   * <p>Also processes the {@link java.io.Closeable} and {@link java.lang.AutoCloseable}
   * interfaces, reflectively calling the "close" method on implementing beans as well.
   */
  @Nullable
  static String[] inferDestroyMethodsIfNecessary(Class<?> target, RootBeanDefinition beanDefinition) {
    String[] destroyMethodNames = beanDefinition.getDestroyMethodNames();
    if (destroyMethodNames != null && destroyMethodNames.length > 1) {
      return destroyMethodNames;
    }

    String destroyMethodName = beanDefinition.resolvedDestroyMethodName;
    if (destroyMethodName == null) {
      destroyMethodName = beanDefinition.getDestroyMethodName();
      boolean autoCloseable = AutoCloseable.class.isAssignableFrom(target);
      if (AbstractBeanDefinition.INFER_METHOD.equals(destroyMethodName)
              || (destroyMethodName == null && autoCloseable)) {
        // Only perform destroy method inference in case of the bean
        // not explicitly implementing the DisposableBean interface
        destroyMethodName = null;
        if (!(DisposableBean.class.isAssignableFrom(target))) {
          if (autoCloseable) {
            destroyMethodName = CLOSE_METHOD_NAME;
          }
          else {
            try {
              destroyMethodName = target.getMethod(CLOSE_METHOD_NAME).getName();
            }
            catch (NoSuchMethodException ex) {
              try {
                destroyMethodName = target.getMethod(SHUTDOWN_METHOD_NAME).getName();
              }
              catch (NoSuchMethodException ex2) {
                // no candidate destroy method found
              }
            }
          }
        }
      }
      beanDefinition.resolvedDestroyMethodName =
              destroyMethodName != null ? destroyMethodName : "";
    }
    return StringUtils.isEmpty(destroyMethodName) ? null : new String[] { destroyMethodName };
  }

  /**
   * Check whether the given bean has destruction-aware post-processors applying to it.
   *
   * @param bean the bean instance
   * @param postProcessors the post-processor candidates
   */
  public static boolean hasApplicableProcessors(Object bean,
          List<DestructionAwareBeanPostProcessor> postProcessors) {
    if (CollectionUtils.isNotEmpty(postProcessors)) {
      for (DestructionAwareBeanPostProcessor processor : postProcessors) {
        if (processor.requiresDestruction(bean)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Search for all DestructionAwareBeanPostProcessors in the List.
   *
   * @param processors the List to search
   * @return the filtered List of DestructionAwareBeanPostProcessors
   */
  @Nullable
  private static List<DestructionAwareBeanPostProcessor> filterPostProcessors(
          List<DestructionAwareBeanPostProcessor> processors, Object bean) {

    List<DestructionAwareBeanPostProcessor> filteredPostProcessors = null;
    if (CollectionUtils.isNotEmpty(processors)) {
      filteredPostProcessors = new ArrayList<>(processors.size());
      for (DestructionAwareBeanPostProcessor processor : processors) {
        if (processor.requiresDestruction(bean)) {
          filteredPostProcessors.add(processor);
        }
      }
    }
    return filteredPostProcessors;
  }

  /**
   * Inner class to avoid a hard dependency on the Reactive Streams API at runtime.
   */
  private static class ReactiveDestroyMethodHandler {

    public static boolean await(DisposableBeanAdapter beanAdapter, Method destroyMethod, Object returnValue) throws InterruptedException {
      ReactiveAdapter adapter = ReactiveAdapterRegistry.getSharedInstance().getAdapter(returnValue.getClass());
      if (adapter != null) {
        CountDownLatch latch = new CountDownLatch(1);
        adapter.toPublisher(returnValue)
                .subscribe(new DestroyMethodSubscriber(destroyMethod, latch, beanAdapter));
        latch.await();
        return true;
      }
      return false;
    }
  }

  /**
   * Reactive Streams Subscriber for destroy method completion.
   */
  private static class DestroyMethodSubscriber implements Subscriber<Object> {

    private final Method destroyMethod;

    private final CountDownLatch latch;

    private final DisposableBeanAdapter adapter;

    public DestroyMethodSubscriber(Method destroyMethod,
            CountDownLatch latch, DisposableBeanAdapter adapter) {
      this.destroyMethod = destroyMethod;
      this.latch = latch;
      this.adapter = adapter;
    }

    @Override
    public void onSubscribe(Subscription s) {
      s.request(Integer.MAX_VALUE);
    }

    @Override
    public void onNext(Object o) {

    }

    @Override
    public void onError(Throwable t) {
      this.latch.countDown();
      adapter.logDestroyMethodException(this.destroyMethod, t);
    }

    @Override
    public void onComplete() {
      this.latch.countDown();
      adapter.logDestroyMethodCompletion(this.destroyMethod, true);
    }
  }

}

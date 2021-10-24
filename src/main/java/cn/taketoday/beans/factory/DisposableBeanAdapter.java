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

package cn.taketoday.beans.factory;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import cn.taketoday.beans.DisposableBean;
import cn.taketoday.beans.support.BeanUtils;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.CollectionUtils;
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
 * @author TODAY 2021/10/24 22:56
 * @see AbstractBeanFactory
 * @see DisposableBean
 * @see DestructionBeanPostProcessor
 * @since 4.0
 */

final class DisposableBeanAdapter implements DisposableBean, Runnable, Serializable {
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
  private String destroyMethodName;

  @Nullable
  private transient Method destroyMethod;

  @Nullable
  private final List<DestructionBeanPostProcessor> beanPostProcessors;

  /**
   * Create a new DisposableBeanAdapter for the given bean.
   *
   * @param bean the bean instance (never {@code null})
   * @param beanName the name of the bean
   * @param beanDefinition the merged bean definition
   * @param postProcessors the List of BeanPostProcessors
   * (potentially DestructionBeanPostProcessor), if any
   */
  public DisposableBeanAdapter(
          Object bean, String beanName,
          BeanDefinition beanDefinition, List<DestructionBeanPostProcessor> postProcessors) {

    Assert.notNull(bean, "Disposable bean must not be null");
    this.bean = bean;
    this.beanName = beanName;
    this.nonPublicAccessAllowed = true;
    this.invokeDisposableBean = bean instanceof DisposableBean;

    String destroyMethodName = inferDestroyMethodIfNecessary(bean, beanDefinition);
    if (destroyMethodName != null &&
            !(this.invokeDisposableBean && DESTROY_METHOD_NAME.equals(destroyMethodName)) &&
            !beanDefinition.isExternallyManagedDestroyMethod(destroyMethodName)) {

      this.invokeAutoCloseable = (bean instanceof AutoCloseable && CLOSE_METHOD_NAME.equals(destroyMethodName));
      if (!this.invokeAutoCloseable) {
        this.destroyMethodName = destroyMethodName;
        Method destroyMethod = determineDestroyMethod(destroyMethodName);
        if (destroyMethod == null) {
          if (beanDefinition.isEnforceDestroyMethod()) {
            throw new BeanDefinitionValidationException(
                    "Could not find a destroy method named '"
                            + destroyMethodName + "' on bean with name '" + beanName + "'");
          }
        }
        else {
          if (destroyMethod.getParameterCount() > 0) {
            Class<?>[] paramTypes = destroyMethod.getParameterTypes();
            if (paramTypes.length > 1) {
              throw new BeanDefinitionValidationException(
                      "Method '" + destroyMethodName + "' of bean '" +
                              beanName + "' has more than one parameter - not supported as destroy method");
            }
            else if (paramTypes.length == 1 && boolean.class != paramTypes[0]) {
              throw new BeanDefinitionValidationException(
                      "Method '" + destroyMethodName + "' of bean '" +
                              beanName + "' has a non-boolean parameter - not supported as destroy method");
            }
          }
          destroyMethod = ReflectionUtils.getInterfaceMethodIfPossible(destroyMethod);
        }
        this.destroyMethod = destroyMethod;
      }
    }

    this.beanPostProcessors = filterPostProcessors(postProcessors, bean);
  }

  /**
   * Create a new DisposableBeanAdapter for the given bean.
   *
   * @param bean the bean instance (never {@code null})
   * @param postProcessors the List of BeanPostProcessors
   * (potentially DestructionBeanPostProcessor), if any
   */
  public DisposableBeanAdapter(Object bean, List<DestructionBeanPostProcessor> postProcessors) {
    Assert.notNull(bean, "Disposable bean must not be null");
    this.bean = bean;
    this.beanName = bean.getClass().getName();
    this.nonPublicAccessAllowed = true;
    this.invokeDisposableBean = (this.bean instanceof DisposableBean);
    this.beanPostProcessors = filterPostProcessors(postProcessors, bean);
  }

  /**
   * Create a new DisposableBeanAdapter for the given bean.
   */
  private DisposableBeanAdapter(
          Object bean, String beanName, boolean nonPublicAccessAllowed,
          boolean invokeDisposableBean, boolean invokeAutoCloseable,
          @Nullable String destroyMethodName,
          @Nullable List<DestructionBeanPostProcessor> postProcessors) {

    this.bean = bean;
    this.beanName = beanName;
    this.nonPublicAccessAllowed = nonPublicAccessAllowed;
    this.invokeDisposableBean = invokeDisposableBean;
    this.invokeAutoCloseable = invokeAutoCloseable;
    this.destroyMethodName = destroyMethodName;
    this.beanPostProcessors = postProcessors;
  }

  @Override
  public void run() {
    destroy();
  }

  @Override
  public void destroy() {
    if (!CollectionUtils.isEmpty(this.beanPostProcessors)) {
      for (DestructionBeanPostProcessor processor : this.beanPostProcessors) {
        processor.postProcessBeforeDestruction(this.bean, this.beanName);
      }
    }

    if (this.invokeDisposableBean) {
      if (log.isTraceEnabled()) {
        log.trace("Invoking destroy() on bean with name '{}'", this.beanName);
      }
      try {
        ((DisposableBean) this.bean).destroy();
      }
      catch (Throwable ex) {
        log.warn("Invocation of destroy method failed on bean with name '{}': {} ", this.beanName, ex, ex);
      }
    }

    if (this.invokeAutoCloseable) {
      if (log.isTraceEnabled()) {
        log.trace("Invoking close() on bean with name '{}'", this.beanName);
      }
      try {
        ((AutoCloseable) this.bean).close();
      }
      catch (Throwable ex) {
        log.warn("Invocation of close method failed on bean with name '{}' : {}", this.beanName, ex, ex);
      }
    }
    else if (this.destroyMethod != null) {
      invokeCustomDestroyMethod(this.destroyMethod);
    }
    else if (this.destroyMethodName != null) {
      Method methodToInvoke = determineDestroyMethod(this.destroyMethodName);
      if (methodToInvoke != null) {
        invokeCustomDestroyMethod(ReflectionUtils.getInterfaceMethodIfPossible(methodToInvoke));
      }
    }
  }

  @Nullable
  private Method determineDestroyMethod(String name) {
    try {
      return findDestroyMethod(name);
    }
    catch (IllegalArgumentException ex) {
      throw new BeanDefinitionValidationException(
              "Could not find unique destroy method on bean with name '" + this.beanName + ": " + ex.getMessage());
    }
  }

  @Nullable
  private Method findDestroyMethod(String name) {
    return (this.nonPublicAccessAllowed ?
            BeanUtils.findMethodWithMinimalParameters(this.bean.getClass(), name) :
            BeanUtils.findMethodWithMinimalParameters(this.bean.getClass().getMethods(), name));
  }

  /**
   * Invoke the specified custom destroy method on the given bean.
   * <p>This implementation invokes a no-arg method if found, else checking
   * for a method with a single boolean argument (passing in "true",
   * assuming a "force" parameter), else logging an error.
   */
  private void invokeCustomDestroyMethod(final Method destroyMethod) {
    int paramCount = destroyMethod.getParameterCount();
    final Object[] args = new Object[paramCount];
    if (paramCount == 1) {
      args[0] = Boolean.TRUE;
    }
    if (log.isTraceEnabled()) {
      log.trace("Invoking custom destroy method '" + this.destroyMethodName +
                        "' on bean with name '" + this.beanName + "'");
    }
    try {
      ReflectionUtils.makeAccessible(destroyMethod);
      destroyMethod.invoke(this.bean, args);
    }
    catch (InvocationTargetException ex) {
      String msg = "Custom destroy method '" + this.destroyMethodName + "' on bean with name '" +
              this.beanName + "' threw an exception";
      if (log.isDebugEnabled()) {
        log.warn(msg, ex.getTargetException());
      }
      else {
        log.warn(msg + ": " + ex.getTargetException());
      }
    }
    catch (Throwable ex) {
      log.warn("Failed to invoke custom destroy method '" + this.destroyMethodName +
                       "' on bean with name '" + this.beanName + "'", ex);
    }
  }

  /**
   * Serializes a copy of the state of this class,
   * filtering out non-serializable BeanPostProcessors.
   */
  protected Object writeReplace() {
    ArrayList<DestructionBeanPostProcessor> serializablePostProcessors = null;
    if (this.beanPostProcessors != null) {
      serializablePostProcessors = new ArrayList<>();
      for (DestructionBeanPostProcessor postProcessor : this.beanPostProcessors) {
        if (postProcessor instanceof Serializable) {
          serializablePostProcessors.add(postProcessor);
        }
      }
    }
    return new DisposableBeanAdapter(
            this.bean, this.beanName, this.nonPublicAccessAllowed, this.invokeDisposableBean,
            this.invokeAutoCloseable, this.destroyMethodName, serializablePostProcessors);
  }

  /**
   * Check whether the given bean has any kind of destroy method to call.
   *
   * @param bean the bean instance
   * @param beanDefinition the corresponding bean definition
   */
  public static boolean hasDestroyMethod(Object bean, BeanDefinition beanDefinition) {
    return (bean instanceof DisposableBean || inferDestroyMethodIfNecessary(bean, beanDefinition) != null);
  }

  /**
   * If the current value of the given beanDefinition's "destroyMethodName" property is
   * {@link AbstractBeanDefinition#INFER_METHOD}, then attempt to infer a destroy method.
   * Candidate methods are currently limited to public, no-arg methods named "close" or
   * "shutdown" (whether declared locally or inherited). The given BeanDefinition's
   * "destroyMethodName" is updated to be null if no such method is found, otherwise set
   * to the name of the inferred method.
   * <p>Also processes the {@link java.io.Closeable} and {@link java.lang.AutoCloseable}
   * interfaces, reflectively calling the "close" method on implementing beans as well.
   */
  @Nullable
  private static String inferDestroyMethodIfNecessary(Object bean, BeanDefinition beanDefinition) {
    String destroyMethodName = beanDefinition.resolvedDestroyMethodName;
    if (destroyMethodName == null) {
      destroyMethodName = beanDefinition.getDestroyMethods();
      boolean autoCloseable = (bean instanceof AutoCloseable);
      if (AbstractBeanDefinition.INFER_METHOD.equals(destroyMethodName) ||
              (destroyMethodName == null && autoCloseable)) {
        // Only perform destroy method inference in case of the bean
        // not explicitly implementing the DisposableBean interface
        destroyMethodName = null;
        if (!(bean instanceof DisposableBean)) {
          if (autoCloseable) {
            destroyMethodName = CLOSE_METHOD_NAME;
          }
          else {
            try {
              destroyMethodName = bean.getClass().getMethod(CLOSE_METHOD_NAME).getName();
            }
            catch (NoSuchMethodException ex) {
              try {
                destroyMethodName = bean.getClass().getMethod(SHUTDOWN_METHOD_NAME).getName();
              }
              catch (NoSuchMethodException ex2) {
                // no candidate destroy method found
              }
            }
          }
        }
      }
      beanDefinition.resolvedDestroyMethodName = (destroyMethodName != null ? destroyMethodName : "");
    }
    return StringUtils.isNotEmpty(destroyMethodName) ? destroyMethodName : null;
  }

  /**
   * Check whether the given bean has destruction-aware post-processors applying to it.
   *
   * @param bean the bean instance
   * @param postProcessors the post-processor candidates
   */
  public static boolean hasApplicableProcessors(
          Object bean, List<DestructionBeanPostProcessor> postProcessors) {
    if (!CollectionUtils.isEmpty(postProcessors)) {
      for (DestructionBeanPostProcessor processor : postProcessors) {
        if (processor.requiresDestruction(bean)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Search for all DestructionBeanPostProcessors in the List.
   *
   * @param processors the List to search
   * @return the filtered List of DestructionBeanPostProcessors
   */
  @Nullable
  private static List<DestructionBeanPostProcessor> filterPostProcessors(
          List<DestructionBeanPostProcessor> processors, Object bean) {

    if (CollectionUtils.isNotEmpty(processors)) {
      ArrayList<DestructionBeanPostProcessor> filteredPostProcessors = new ArrayList<>(processors.size());
      for (DestructionBeanPostProcessor processor : processors) {
        if (processor.requiresDestruction(bean)) {
          filteredPostProcessors.add(processor);
        }
      }
      return filteredPostProcessors;
    }
    return null;
  }

}

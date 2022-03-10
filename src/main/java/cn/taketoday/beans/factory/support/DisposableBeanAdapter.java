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

package cn.taketoday.beans.factory.support;

import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import cn.taketoday.beans.factory.BeanDefinitionValidationException;
import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.beans.factory.config.DestructionAwareBeanPostProcessor;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
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
 * @author TODAY 2021/10/24 22:56
 * @see AbstractBeanFactory
 * @see DisposableBean
 * @see DestructionAwareBeanPostProcessor
 * @since 4.0
 */
final class DisposableBeanAdapter implements DisposableBean, Runnable, Serializable {
  private static final Logger log = LoggerFactory.getLogger(DisposableBeanAdapter.class);

  private static final String CLOSE_METHOD_NAME = "close";
  private static final String DESTROY_METHOD_NAME = "destroy";
  private static final String SHUTDOWN_METHOD_NAME = "shutdown";

  private final Object bean;
  private final String beanName;

  private final boolean invokeDisposableBean;

  private boolean invokeAutoCloseable;

  @Nullable
  private String[] destroyMethodNames;

  @Nullable
  private transient Method[] destroyMethods;

  @Nullable
  private final List<DestructionAwareBeanPostProcessor> beanPostProcessors;

  private final boolean nonPublicAccessAllowed;

  /**
   * Create a new DisposableBeanAdapter for the given bean.
   *
   * @param bean the bean instance (never {@code null})
   * @param beanDefinition the merged bean definition
   * @param postProcessors the List of BeanPostProcessors
   * (potentially DestructionBeanPostProcessor), if any
   */
  public DisposableBeanAdapter(
          String beanName, Object bean, RootBeanDefinition beanDefinition,
          @Nullable List<DestructionAwareBeanPostProcessor> postProcessors) {
    Assert.notNull(bean, "Disposable bean must not be null");
    this.bean = bean;
    this.beanName = beanName;
    this.nonPublicAccessAllowed = beanDefinition.isNonPublicAccessAllowed();
    this.invokeDisposableBean = bean instanceof DisposableBean
            && !beanDefinition.hasAnyExternallyManagedDestroyMethod(DESTROY_METHOD_NAME);

    String[] destroyMethodNames = inferDestroyMethodsIfNecessary(bean, beanDefinition);
    if (ObjectUtils.isNotEmpty(destroyMethodNames)
            && !(invokeDisposableBean && DESTROY_METHOD_NAME.equals(destroyMethodNames[0]))
            && !beanDefinition.hasAnyExternallyManagedDestroyMethod(destroyMethodNames[0])) {

      this.invokeAutoCloseable = bean instanceof AutoCloseable && CLOSE_METHOD_NAME.equals(destroyMethodNames[0]);
      if (!invokeAutoCloseable) {
        this.destroyMethodNames = destroyMethodNames;
        Method[] destroyMethods = new Method[destroyMethodNames.length];
        for (int i = 0; i < destroyMethodNames.length; i++) {
          String destroyMethodName = destroyMethodNames[i];
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
          }
          destroyMethods[i] = destroyMethod;
        }
        this.destroyMethods = destroyMethods;
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
  public DisposableBeanAdapter(Object bean, List<DestructionAwareBeanPostProcessor> postProcessors) {
    Assert.notNull(bean, "Disposable bean must not be null");
    this.bean = bean;
    this.nonPublicAccessAllowed = true;
    this.beanName = bean.getClass().getName();
    this.invokeDisposableBean = this.bean instanceof DisposableBean;
    this.beanPostProcessors = filterPostProcessors(postProcessors, bean);
  }

  /**
   * Create a new DisposableBeanAdapter for the given bean.
   */
  private DisposableBeanAdapter(Object bean, String beanName, boolean nonPublicAccessAllowed,
          boolean invokeDisposableBean, boolean invokeAutoCloseable,
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
        ((DisposableBean) bean).destroy();
      }
      catch (Throwable ex) {
        String msg = "Invocation of destroy method failed on bean with name '" + this.beanName + "'";
        if (log.isDebugEnabled()) {
          log.warn(msg, ex);
        }
        else {
          log.warn(msg + ": " + ex);
        }
      }
    }

    if (invokeAutoCloseable) {
      if (log.isTraceEnabled()) {
        log.trace("Invoking close() on bean with name '{}'", beanName);
      }
      try {
        ((AutoCloseable) bean).close();
      }
      catch (Throwable ex) {
        String msg = "Invocation of close method failed on bean with name '" + this.beanName + "'";
        if (log.isDebugEnabled()) {
          log.warn(msg, ex);
        }
        else {
          log.warn(msg + ": " + ex);
        }
      }
    }
    else if (destroyMethods != null) {
      for (Method destroyMethod : destroyMethods) {
        invokeCustomDestroyMethod(destroyMethod);
      }
    }
    else if (destroyMethodNames != null) {
      for (String destroyMethodName : destroyMethodNames) {
        Method destroyMethod = determineDestroyMethod(destroyMethodName);
        if (destroyMethod != null) {
          invokeCustomDestroyMethod(
                  ReflectionUtils.getInterfaceMethodIfPossible(destroyMethod, this.bean.getClass()));
        }
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
              "Could not find unique destroy method on bean with name '" + beanName + ": " + ex.getMessage());
    }
  }

  @Nullable
  private Method findDestroyMethod(String name) {
    return nonPublicAccessAllowed ?
           ReflectionUtils.findMethodWithMinimalParameters(bean.getClass(), name) :
           ReflectionUtils.findMethodWithMinimalParameters(bean.getClass().getMethods(), name);
  }

  /**
   * Invoke the specified custom destroy method on the given bean.
   * <p>This implementation invokes a no-arg method if found, else checking
   * for a method with a single boolean argument (passing in "true",
   * assuming a "force" parameter), else logging an error.
   */
  private void invokeCustomDestroyMethod(Method destroyMethod) {
    int paramCount = destroyMethod.getParameterCount();
    Object[] args = new Object[paramCount];
    if (paramCount == 1) {
      args[0] = Boolean.TRUE;
    }
    if (log.isTraceEnabled()) {
      log.trace("Invoking custom destroy method '{}' on bean with name '{}'", destroyMethod.getName(), beanName);
    }
    try {
      ReflectionUtils.makeAccessible(destroyMethod);
      destroyMethod.invoke(this.bean, args);
    }
    catch (InvocationTargetException ex) {
      log.warn("Custom destroy method '{}' on bean with name '{}' threw an exception: {}",
              destroyMethod.getName(), beanName, ex.getTargetException(), ex);
    }
    catch (Throwable ex) {
      log.warn("Failed to invoke custom destroy method '{}' on bean with name '{}'",
              destroyMethod.getName(), beanName, ex);
    }
  }

  /**
   * Serializes a copy of the state of this class,
   * filtering out non-serializable BeanPostProcessors.
   */
  @Serial
  private Object writeReplace() {
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
    return (bean instanceof DisposableBean
            || inferDestroyMethodsIfNecessary(bean, beanDefinition) != null);
  }

  /**
   * If the current value of the given autoInferDestroyMethod is true, then attempt to infer a destroy method.
   * Candidate methods are currently limited to public, no-arg methods named "close" or
   * "shutdown" (whether declared locally or inherited). The given BeanDefinition's
   * "destroyMethodName" is updated to be null if no such method is found, otherwise set
   * to the name of the inferred method.
   * <p>Also processes the {@link java.io.Closeable} and {@link java.lang.AutoCloseable}
   * interfaces, reflectively calling the "close" method on implementing beans as well.
   */
  @Nullable
  private static String[] inferDestroyMethodsIfNecessary(Object bean, RootBeanDefinition beanDefinition) {

    String[] destroyMethodNames = beanDefinition.getDestroyMethodNames();
    if (destroyMethodNames != null && destroyMethodNames.length > 1) {
      return destroyMethodNames;
    }

    String destroyMethodName = beanDefinition.resolvedDestroyMethodName;
    if (destroyMethodName == null) {
      destroyMethodName = beanDefinition.getDestroyMethodName();
      boolean autoCloseable = (bean instanceof AutoCloseable);
      if (AbstractBeanDefinition.INFER_METHOD.equals(destroyMethodName)
              || (destroyMethodName == null && autoCloseable)) {
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
      beanDefinition.resolvedDestroyMethodName =
              destroyMethodName != null ? destroyMethodName : Constant.BLANK;
    }
    return StringUtils.isEmpty(destroyMethodName) ? null : new String[] { destroyMethodName };
  }

  /**
   * Check whether the given bean has destruction-aware post-processors applying to it.
   *
   * @param bean the bean instance
   * @param postProcessors the post-processor candidates
   */
  public static boolean hasApplicableProcessors(
          Object bean, List<DestructionAwareBeanPostProcessor> postProcessors) {
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
   * Search for all DestructionBeanPostProcessors in the List.
   *
   * @param processors the List to search
   * @return the filtered List of DestructionBeanPostProcessors
   */
  @Nullable
  private static List<DestructionAwareBeanPostProcessor> filterPostProcessors(
          List<DestructionAwareBeanPostProcessor> processors, Object bean) {

    if (CollectionUtils.isNotEmpty(processors)) {
      ArrayList<DestructionAwareBeanPostProcessor> filteredPostProcessors = new ArrayList<>(processors.size());
      for (DestructionAwareBeanPostProcessor processor : processors) {
        if (processor.requiresDestruction(bean)) {
          filteredPostProcessors.add(processor);
        }
      }
      return filteredPostProcessors;
    }
    return null;
  }

  @Nullable
  static ArrayList<DestructionAwareBeanPostProcessor> filter(
          Object obj, List<DestructionAwareBeanPostProcessor> postProcessors) {
    ArrayList<DestructionAwareBeanPostProcessor> filteredPostProcessors = null;
    if (CollectionUtils.isNotEmpty(postProcessors)) {
      filteredPostProcessors = new ArrayList<>(postProcessors.size());
      for (DestructionAwareBeanPostProcessor postProcessor : postProcessors) {
        if (postProcessor.requiresDestruction(obj)) {
          filteredPostProcessors.add(postProcessor);
        }
      }
    }
    return filteredPostProcessors;
  }

}

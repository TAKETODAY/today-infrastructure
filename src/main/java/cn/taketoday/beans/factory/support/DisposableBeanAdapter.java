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
import cn.taketoday.beans.factory.DestructionBeanPostProcessor;
import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
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

  private final boolean invokeDisposableBean;

  private boolean invokeAutoCloseable;

  @Nullable
  private String destroyMethodName;

  @Nullable
  private transient Method destroyMethod;

  @Nullable
  private final List<DestructionBeanPostProcessor> beanPostProcessors;

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
          Object bean, BeanDefinition beanDefinition,
          @Nullable List<DestructionBeanPostProcessor> postProcessors) {
    Assert.notNull(bean, "Disposable bean must not be null");
    this.bean = bean;
    this.beanName = beanDefinition.getBeanName();
    this.nonPublicAccessAllowed = beanDefinition.isNonPublicAccessAllowed();
    this.invokeDisposableBean = bean instanceof DisposableBean
            && !beanDefinition.hasAnyExternallyManagedDestroyMethod(DESTROY_METHOD_NAME);

    String destroyName = inferDestroyMethodIfNecessary(bean, beanDefinition);
    if (destroyName != null
            && !(invokeDisposableBean && destroyName.equals(DESTROY_METHOD_NAME))
            && !beanDefinition.hasAnyExternallyManagedDestroyMethod(destroyName)) {

      this.invokeAutoCloseable = bean instanceof AutoCloseable && destroyName.equals(CLOSE_METHOD_NAME);
      if (!invokeAutoCloseable) {
        this.destroyMethodName = destroyName;
        Method destroyMethod = determineDestroyMethod(destroyName);
        if (destroyMethod != null) {
          if (destroyMethod.getParameterCount() > 0) {
            Class<?>[] paramTypes = destroyMethod.getParameterTypes();
            if (paramTypes.length > 1) {
              throw new BeanDefinitionValidationException(
                      "Method '" + destroyName + "' of bean '" +
                              beanName + "' has more than one parameter - not supported as destroy method");
            }
            else if (paramTypes.length == 1 && boolean.class != paramTypes[0]) {
              throw new BeanDefinitionValidationException(
                      "Method '" + destroyName + "' of bean '" +
                              beanName + "' has a non-boolean parameter - not supported as destroy method");
            }
          }
          destroyMethod = ReflectionUtils.getInterfaceMethodIfPossible(destroyMethod, bean.getClass());
        }
        else if (beanDefinition.isEnforceDestroyMethod()) {
          throw new BeanDefinitionValidationException("Could not find a destroy method named '" +
                  destroyName + "' on bean with name '" + beanName + "'");
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
          @Nullable String destroyMethodName, @Nullable List<DestructionBeanPostProcessor> postProcessors) {

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
    if (CollectionUtils.isNotEmpty(beanPostProcessors)) {
      for (DestructionBeanPostProcessor processor : beanPostProcessors) {
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
        log.warn("Invocation of destroy method failed on bean with name '{}': {} ", beanName, ex, ex);
      }
    }

    if (this.invokeAutoCloseable) {
      if (log.isTraceEnabled()) {
        log.trace("Invoking close() on bean with name '{}'", beanName);
      }
      try {
        ((AutoCloseable) bean).close();
      }
      catch (Throwable ex) {
        log.warn("Invocation of close method failed on bean with name '{}' : {}", beanName, ex, ex);
      }
    }
    else if (destroyMethod != null) {
      invokeCustomDestroyMethod(destroyMethod);
    }
    else if (destroyMethodName != null) {
      Method methodToInvoke = determineDestroyMethod(destroyMethodName);
      if (methodToInvoke != null) {
        invokeCustomDestroyMethod(ReflectionUtils.getInterfaceMethodIfPossible(methodToInvoke, bean.getClass()));
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
      log.trace("Invoking custom destroy method '{}' on bean with name '{}'", this.destroyMethodName, this.beanName);
    }
    try {
      ReflectionUtils.makeAccessible(destroyMethod);
      destroyMethod.invoke(this.bean, args);
    }
    catch (InvocationTargetException ex) {
      log.warn("Custom destroy method '{}' on bean with name '{}' threw an exception: {}",
              this.destroyMethodName, this.beanName, ex.getTargetException(), ex);
    }
    catch (Throwable ex) {
      log.warn("Failed to invoke custom destroy method '{}' on bean with name '{}'",
              this.destroyMethodName, this.beanName, ex);
    }
  }

  /**
   * Serializes a copy of the state of this class,
   * filtering out non-serializable BeanPostProcessors.
   */
  @Serial
  private Object writeReplace() {
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
    return (bean instanceof DisposableBean
            || inferDestroyMethodIfNecessary(bean, beanDefinition) != null);
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
  private static String inferDestroyMethodIfNecessary(Object bean, BeanDefinition beanDefinition) {
    String destroyMethodName = beanDefinition.resolvedDestroyMethodName;
    if (destroyMethodName == null) {
      destroyMethodName = beanDefinition.getDestroyMethod();
      boolean autoCloseable = (bean instanceof AutoCloseable);
      if (BeanDefinition.INFER_METHOD.equals(destroyMethodName) ||
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
      beanDefinition.resolvedDestroyMethodName =
              destroyMethodName != null ? destroyMethodName : Constant.BLANK;
    }
    return StringUtils.isEmpty(destroyMethodName) ? null : destroyMethodName;
  }

  /**
   * Check whether the given bean has destruction-aware post-processors applying to it.
   *
   * @param bean the bean instance
   * @param postProcessors the post-processor candidates
   */
  public static boolean hasApplicableProcessors(
          Object bean, List<DestructionBeanPostProcessor> postProcessors) {
    if (CollectionUtils.isNotEmpty(postProcessors)) {
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

  /**
   * Destroy bean instance
   *
   * @param obj Bean instance
   */
  public static void destroyBean(Object obj) {
    destroyBean(obj, null);
  }

  /**
   * Destroy bean instance
   *
   * @param obj Bean instance
   */
  public static void destroyBean(Object obj, BeanDefinition def) {
    destroyBean(obj, def, null);
  }

  /**
   * Destroy bean instance
   *
   * @param obj Bean instance
   */
  public static void destroyBean(
          Object obj, BeanDefinition def, List<DestructionBeanPostProcessor> postProcessors) {
    Assert.notNull(obj, "bean instance must not be null");
    new DisposableBeanAdapter(obj, def, postProcessors)
            .destroy();
  }

  @Nullable
  static ArrayList<DestructionBeanPostProcessor> filter(
          Object obj, List<DestructionBeanPostProcessor> postProcessors) {
    ArrayList<DestructionBeanPostProcessor> filteredPostProcessors = null;
    if (CollectionUtils.isNotEmpty(postProcessors)) {
      filteredPostProcessors = new ArrayList<>(postProcessors.size());
      for (DestructionBeanPostProcessor postProcessor : postProcessors) {
        if (postProcessor.requiresDestruction(obj)) {
          filteredPostProcessors.add(postProcessor);
        }
      }
    }
    return filteredPostProcessors;
  }

}

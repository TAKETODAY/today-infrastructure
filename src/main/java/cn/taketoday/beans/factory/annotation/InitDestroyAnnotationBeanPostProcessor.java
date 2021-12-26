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
package cn.taketoday.beans.factory.annotation;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.beans.ArgumentsResolver;
import cn.taketoday.beans.factory.BeanCreationException;
import cn.taketoday.beans.factory.BeanDefinitionPostProcessor;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.BeanPostProcessor;
import cn.taketoday.beans.factory.BeansException;
import cn.taketoday.beans.factory.DestructionBeanPostProcessor;
import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.beans.factory.InitializationBeanPostProcessor;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.core.OrderSourceProvider;
import cn.taketoday.core.OrderedSupport;
import cn.taketoday.core.PriorityOrdered;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ReflectionUtils;

/**
 * {@link BeanPostProcessor} implementation that invokes annotated init and destroy
 * methods. Allows for an annotation alternative to {@link InitializingBean} and
 * {@link DisposableBean} callback interfaces.
 *
 * <p>The actual annotation types that this post-processor checks for can be
 * configured through the {@link #setInitAnnotationType "initAnnotationType"}
 * and {@link #setDestroyAnnotationType "destroyAnnotationType"} properties.
 * Any custom annotation can be used, since there are no required annotation
 * attributes.
 *
 * <p>Init and destroy annotations may be applied to methods of any visibility:
 * public, package-protected, protected, or private. Multiple such methods
 * may be annotated, but it is recommended to only annotate one single
 * init method and destroy method, respectively.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #setInitAnnotationType
 * @see #setDestroyAnnotationType
 * @since 4.0 2021/12/3 16:46
 */
@SuppressWarnings("serial")
public class InitDestroyAnnotationBeanPostProcessor extends OrderedSupport
        implements DestructionBeanPostProcessor, BeanDefinitionPostProcessor,
                   BeanFactoryAware, InitializationBeanPostProcessor, PriorityOrdered, Serializable {

  private static final Logger log = LoggerFactory.getLogger(InitDestroyAnnotationBeanPostProcessor.class);

  private final transient LifecycleMetadata emptyLifecycleMetadata =
          new LifecycleMetadata(Collections.emptyList(), Collections.emptyList()) {

            @Override
            public void invokeInitMethods(Object target, String beanName) { }

            @Override
            public void invokeDestroyMethods(Object target, String beanName) { }

            @Override
            public boolean hasDestroyMethods() {
              return false;
            }
          };

  @Nullable
  private Class<? extends Annotation> initAnnotationType;

  @Nullable
  private Class<? extends Annotation> destroyAnnotationType;

  @Nullable
  private final transient ConcurrentHashMap<Class<?>, LifecycleMetadata>
          lifecycleMetadataCache = new ConcurrentHashMap<>(256);

  @Nullable
  private ArgumentsResolver argumentsResolver;

  @Nullable
  public ArgumentsResolver getArgumentsResolver() {
    return argumentsResolver;
  }

  /**
   * @param argumentsResolver Can be {@code null} but cannot support parameter injection
   */
  public void setArgumentsResolver(@Nullable ArgumentsResolver argumentsResolver) {
    this.argumentsResolver = argumentsResolver;
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
    this.argumentsResolver = beanFactory.getArgumentsResolver();
  }

  /**
   * Specify the init annotation to check for, indicating initialization
   * methods to call after configuration of a bean.
   * <p>Any custom annotation can be used, since there are no required
   * annotation attributes. There is no default, although a typical choice
   * is the {@link jakarta.annotation.PostConstruct} annotation.
   */
  public void setInitAnnotationType(Class<? extends Annotation> initAnnotationType) {
    this.initAnnotationType = initAnnotationType;
  }

  /**
   * Specify the destroy annotation to check for, indicating destruction
   * methods to call when the context is shutting down.
   * <p>Any custom annotation can be used, since there are no required
   * annotation attributes. There is no default, although a typical choice
   * is the {@link jakarta.annotation.PreDestroy} annotation.
   */
  public void setDestroyAnnotationType(Class<? extends Annotation> destroyAnnotationType) {
    this.destroyAnnotationType = destroyAnnotationType;
  }

  @Override
  public void postProcessBeanDefinition(BeanDefinition beanDefinition, Object bean, String beanName) {
    findLifecycleMetadata(bean.getClass());
  }

  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
    LifecycleMetadata metadata = findLifecycleMetadata(bean.getClass());
    try {
      metadata.invokeInitMethods(bean, beanName);
    }
    catch (InvocationTargetException ex) {
      throw new BeanCreationException(beanName, "Invocation of init method failed", ex.getTargetException());
    }
    catch (Throwable ex) {
      throw new BeanCreationException(beanName, "Failed to invoke init method", ex);
    }
    return bean;
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    return bean;
  }

  @Override
  public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
    LifecycleMetadata metadata = findLifecycleMetadata(bean.getClass());
    try {
      metadata.invokeDestroyMethods(bean, beanName);
    }
    catch (InvocationTargetException ex) {
      String msg = "Destroy method on bean with name '" + beanName + "' threw an exception";
      if (log.isDebugEnabled()) {
        log.warn(msg, ex.getTargetException());
      }
      else {
        log.warn("{}: {}", msg, ex.getTargetException());
      }
    }
    catch (Throwable ex) {
      log.warn("Failed to invoke destroy method on bean with name '{}'", beanName, ex);
    }
  }

  @Override
  public boolean requiresDestruction(Object bean) {
    return findLifecycleMetadata(bean.getClass()).hasDestroyMethods();
  }

  private LifecycleMetadata findLifecycleMetadata(Class<?> clazz) {
    if (this.lifecycleMetadataCache == null) {
      // Happens after deserialization, during destruction...
      return buildLifecycleMetadata(clazz);
    }
    // Quick check on the concurrent map first, with minimal locking.
    LifecycleMetadata metadata = this.lifecycleMetadataCache.get(clazz);
    if (metadata == null) {
      synchronized(lifecycleMetadataCache) {
        metadata = lifecycleMetadataCache.get(clazz);
        if (metadata == null) {
          metadata = buildLifecycleMetadata(clazz);
          lifecycleMetadataCache.put(clazz, metadata);
        }
        return metadata;
      }
    }
    return metadata;
  }

  private LifecycleMetadata buildLifecycleMetadata(Class<?> clazz) {
    if (!AnnotationUtils.isCandidateClass(clazz, Arrays.asList(initAnnotationType, destroyAnnotationType))) {
      return this.emptyLifecycleMetadata;
    }

    ArrayList<LifecycleElement> initMethods = new ArrayList<>();
    ArrayList<LifecycleElement> destroyMethods = new ArrayList<>();
    Class<?> targetClass = clazz;

    do {
      ArrayList<LifecycleElement> currInitMethods = new ArrayList<>();
      ArrayList<LifecycleElement> currDestroyMethods = new ArrayList<>();

      ReflectionUtils.doWithLocalMethods(targetClass, method -> {
        if (initAnnotationType != null && method.isAnnotationPresent(initAnnotationType)) {
          LifecycleElement element = new LifecycleElement(method);
          currInitMethods.add(element);
          if (log.isTraceEnabled()) {
            log.trace("Found init method on class [{}]: {}", clazz.getName(), method);
          }
        }
        if (destroyAnnotationType != null && method.isAnnotationPresent(destroyAnnotationType)) {
          currDestroyMethods.add(new LifecycleElement(method));
          if (log.isTraceEnabled()) {
            log.trace("Found destroy method on class [{}]: {}", clazz.getName(), method);
          }
        }
      });

      initMethods.addAll(0, currInitMethods);
      destroyMethods.addAll(currDestroyMethods);
      targetClass = targetClass.getSuperclass();
    }
    while (targetClass != null && targetClass != Object.class);
    return initMethods.isEmpty() && destroyMethods.isEmpty()
           ? this.emptyLifecycleMetadata
           : new LifecycleMetadata(initMethods, destroyMethods);
  }

  //---------------------------------------------------------------------
  // Serialization support
  //---------------------------------------------------------------------

  @Serial
  private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
    // Rely on default serialization; just initialize state after deserialization.
    ois.defaultReadObject();
  }

  /**
   * Class representing information about annotated init and destroy methods.
   */
  private class LifecycleMetadata {

    private final List<LifecycleElement> initMethods;
    private final List<LifecycleElement> destroyMethods;

    public LifecycleMetadata(List<LifecycleElement> initMethods, List<LifecycleElement> destroyMethods) {
      this.initMethods = initMethods;
      this.destroyMethods = destroyMethods;
      AnnotationAwareOrderComparator.sort(initMethods);
      AnnotationAwareOrderComparator.sort(destroyMethods);
    }

    public void invokeInitMethods(Object target, String beanName) throws Throwable {
      if (!initMethods.isEmpty()) {
        for (LifecycleElement element : initMethods) {
          if (log.isTraceEnabled()) {
            log.trace("Invoking init method on bean '{}': {}", beanName, element.getMethod());
          }
          element.invoke(target, argumentsResolver);
        }
      }
    }

    public void invokeDestroyMethods(Object target, String beanName) throws Throwable {
      if (!destroyMethods.isEmpty()) {
        for (LifecycleElement element : destroyMethods) {
          if (log.isTraceEnabled()) {
            log.trace("Invoking destroy method on bean '{}': {}", beanName, element.getMethod());
          }
          element.invoke(target, argumentsResolver);
        }
      }
    }

    public boolean hasDestroyMethods() {
      return !destroyMethods.isEmpty();
    }
  }

  /**
   * Class representing injection information about an annotated method.
   */
  private static class LifecycleElement implements OrderSourceProvider {

    private final Method method;
    private final String identifier;

    public LifecycleElement(Method method) {
      this.method = method;
      this.identifier = Modifier.isPrivate(method.getModifiers())
                        ? ClassUtils.getQualifiedMethodName(method)
                        : method.getName();
    }

    public Method getMethod() {
      return this.method;
    }

    public String getIdentifier() {
      return this.identifier;
    }

    public void invoke(Object target, @Nullable ArgumentsResolver resolver) throws Throwable {
      ReflectionUtils.makeAccessible(method);
      if (resolver != null) {
        Object[] args = resolver.resolve(method);
        method.invoke(target, args);
      }
      else {
        method.invoke(target, (Object[]) null);
      }
    }

    @Override
    public boolean equals(@Nullable Object other) {
      if (this == other) {
        return true;
      }
      if (!(other instanceof LifecycleElement otherElement)) {
        return false;
      }
      return this.identifier.equals(otherElement.identifier);
    }

    @Override
    public int hashCode() {
      return this.identifier.hashCode();
    }

    @Nullable
    @Override
    public Object getOrderSource(Object obj) {
      return method;
    }
  }

}

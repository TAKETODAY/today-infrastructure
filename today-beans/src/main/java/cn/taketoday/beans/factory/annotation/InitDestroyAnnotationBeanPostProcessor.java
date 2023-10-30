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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.BeanCreationException;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.InitializationBeanPostProcessor;
import cn.taketoday.beans.factory.aot.BeanRegistrationAotContribution;
import cn.taketoday.beans.factory.aot.BeanRegistrationAotProcessor;
import cn.taketoday.beans.factory.config.DestructionAwareBeanPostProcessor;
import cn.taketoday.beans.factory.support.DependencyInjector;
import cn.taketoday.beans.factory.support.MergedBeanDefinitionPostProcessor;
import cn.taketoday.beans.factory.support.RegisteredBean;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.core.OrderSourceProvider;
import cn.taketoday.core.OrderedSupport;
import cn.taketoday.core.PriorityOrdered;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ReflectionUtils;

/**
 * {@link cn.taketoday.beans.factory.config.BeanPostProcessor} implementation
 * that invokes annotated init and destroy methods. Allows for an annotation
 * alternative to {@link cn.taketoday.beans.factory.InitializingBean}
 * and {@link cn.taketoday.beans.factory.DisposableBean} callback interfaces.
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
 * <p>{@link cn.taketoday.context.annotation.CommonAnnotationBeanPostProcessor}
 * supports the {@link jakarta.annotation.PostConstruct} and {@link jakarta.annotation.PreDestroy}
 * annotations out of the box, as init annotation and destroy annotation, respectively.
 * Furthermore, it also supports the {@link jakarta.annotation.Resource} annotation
 * for annotation-driven injection of named beans.
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #setInitAnnotationType
 * @see #setDestroyAnnotationType
 * @since 4.0 2021/12/3 16:46
 */
@SuppressWarnings("serial")
public class InitDestroyAnnotationBeanPostProcessor extends OrderedSupport
        implements DestructionAwareBeanPostProcessor, MergedBeanDefinitionPostProcessor, BeanFactoryAware,
        InitializationBeanPostProcessor, BeanRegistrationAotProcessor, PriorityOrdered, Serializable {

  private static final Logger log = LoggerFactory.getLogger(InitDestroyAnnotationBeanPostProcessor.class);

  private final transient LifecycleMetadata emptyLifecycleMetadata =
          new LifecycleMetadata(Object.class, Collections.emptyList(), Collections.emptyList()) {

            @Override
            public void checkInitDestroyMethods(RootBeanDefinition beanDefinition) { }

            @Override
            public void invokeInitMethods(Object target, String beanName) { }

            @Override
            public void invokeDestroyMethods(Object target, String beanName) { }

            @Override
            public boolean hasDestroyMethods() {
              return false;
            }
          };

  private final Set<Class<? extends Annotation>> initAnnotationTypes = new LinkedHashSet<>(2);

  private final Set<Class<? extends Annotation>> destroyAnnotationTypes = new LinkedHashSet<>(2);

  @Nullable
  private final transient ConcurrentHashMap<Class<?>, LifecycleMetadata>
          lifecycleMetadataCache = new ConcurrentHashMap<>(256);

  @Nullable
  private DependencyInjector dependencyInjector;

  @Override
  public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
    Assert.notNull(beanFactory, "BeanFactory is required");
    this.dependencyInjector = beanFactory.getInjector();
  }

  /**
   * Specify the init annotation to check for, indicating initialization
   * methods to call after configuration of a bean.
   * <p>Any custom annotation can be used, since there are no required
   * annotation attributes. There is no default, although a typical choice
   * is the {@link jakarta.annotation.PostConstruct} annotation.
   *
   * @see #addInitAnnotationType
   */
  public void setInitAnnotationType(Class<? extends Annotation> initAnnotationType) {
    this.initAnnotationTypes.clear();
    this.initAnnotationTypes.add(initAnnotationType);
  }

  /**
   * Add an init annotation to check for, indicating initialization
   * methods to call after configuration of a bean.
   *
   * @see #setInitAnnotationType
   */
  public void addInitAnnotationType(@Nullable Class<? extends Annotation> initAnnotationType) {
    if (initAnnotationType != null) {
      this.initAnnotationTypes.add(initAnnotationType);
    }
  }

  /**
   * Specify the destroy annotation to check for, indicating destruction
   * methods to call when the context is shutting down.
   * <p>Any custom annotation can be used, since there are no required
   * annotation attributes. There is no default, although a typical choice
   * is the {@link jakarta.annotation.PreDestroy} annotation.
   *
   * @see #addDestroyAnnotationType
   */
  public void setDestroyAnnotationType(Class<? extends Annotation> destroyAnnotationType) {
    this.destroyAnnotationTypes.clear();
    this.destroyAnnotationTypes.add(destroyAnnotationType);
  }

  /**
   * Add a destroy annotation to check for, indicating destruction
   * methods to call when the context is shutting down.
   *
   * @see #setDestroyAnnotationType
   */
  public void addDestroyAnnotationType(@Nullable Class<? extends Annotation> destroyAnnotationType) {
    if (destroyAnnotationType != null) {
      this.destroyAnnotationTypes.add(destroyAnnotationType);
    }
  }

  @Override
  public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
    findLifecycleMetadata(beanDefinition, beanType);
  }

  @Override
  @Nullable
  public BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
    RootBeanDefinition beanDefinition = registeredBean.getMergedBeanDefinition();
    beanDefinition.resolveDestroyMethodIfNecessary();
    LifecycleMetadata metadata = findLifecycleMetadata(beanDefinition, registeredBean.getBeanClass());
    if (CollectionUtils.isNotEmpty(metadata.initMethods)) {
      String[] initMethodNames = safeMerge(beanDefinition.getInitMethodNames(), metadata.initMethods);
      beanDefinition.setInitMethodNames(initMethodNames);
    }
    if (CollectionUtils.isNotEmpty(metadata.destroyMethods)) {
      String[] destroyMethodNames = safeMerge(beanDefinition.getDestroyMethodNames(), metadata.destroyMethods);
      beanDefinition.setDestroyMethodNames(destroyMethodNames);
    }
    return null;
  }

  private LifecycleMetadata findLifecycleMetadata(RootBeanDefinition beanDefinition, Class<?> beanClass) {
    LifecycleMetadata metadata = findLifecycleMetadata(beanClass);
    metadata.checkInitDestroyMethods(beanDefinition);
    return metadata;
  }

  private static String[] safeMerge(@Nullable String[] existingNames, Collection<LifecycleMethod> detectedMethods) {
    Stream<String> detectedNames = detectedMethods.stream().map(LifecycleMethod::getIdentifier);
    Stream<String> mergedNames =
            existingNames != null ? Stream.concat(detectedNames, Stream.of(existingNames)) : detectedNames;
    return mergedNames.distinct().toArray(String[]::new);
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

  private LifecycleMetadata buildLifecycleMetadata(Class<?> beanClass) {
    if (!AnnotationUtils.isCandidateClass(beanClass, this.initAnnotationTypes)
            && !AnnotationUtils.isCandidateClass(beanClass, this.destroyAnnotationTypes)) {
      return this.emptyLifecycleMetadata;
    }

    ArrayList<LifecycleMethod> initMethods = new ArrayList<>();
    ArrayList<LifecycleMethod> destroyMethods = new ArrayList<>();
    Class<?> targetClass = beanClass;

    do {
      ArrayList<LifecycleMethod> currInitMethods = new ArrayList<>();
      ArrayList<LifecycleMethod> currDestroyMethods = new ArrayList<>();

      ReflectionUtils.doWithLocalMethods(targetClass, method -> {
        for (Class<? extends Annotation> initAnnotationType : initAnnotationTypes) {
          if (initAnnotationType != null && method.isAnnotationPresent(initAnnotationType)) {
            currInitMethods.add(new LifecycleMethod(method, beanClass));
            if (log.isTraceEnabled()) {
              log.trace("Found init method on class [{}]: {}", beanClass.getName(), method);
            }
          }
        }
        for (Class<? extends Annotation> destroyAnnotationType : destroyAnnotationTypes) {
          if (destroyAnnotationType != null && method.isAnnotationPresent(destroyAnnotationType)) {
            currDestroyMethods.add(new LifecycleMethod(method, beanClass));
            if (log.isTraceEnabled()) {
              log.trace("Found destroy method on class [{}]: {}", beanClass.getName(), method);
            }
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
           : new LifecycleMetadata(beanClass, initMethods, destroyMethods);
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

    private final Class<?> targetClass;

    private final Collection<LifecycleMethod> initMethods;

    private final Collection<LifecycleMethod> destroyMethods;

    @Nullable
    private volatile Set<LifecycleMethod> checkedInitMethods;

    @Nullable
    private volatile Set<LifecycleMethod> checkedDestroyMethods;

    public LifecycleMetadata(Class<?> targetClass,
            Collection<LifecycleMethod> initMethods,
            Collection<LifecycleMethod> destroyMethods) {

      this.targetClass = targetClass;
      this.initMethods = initMethods;
      this.destroyMethods = destroyMethods;
    }

    public void checkInitDestroyMethods(RootBeanDefinition beanDefinition) {
      var checkedInitMethods = new LinkedHashSet<LifecycleMethod>(initMethods.size());
      for (LifecycleMethod element : initMethods) {
        String methodIdentifier = element.getIdentifier();
        if (!beanDefinition.isExternallyManagedInitMethod(methodIdentifier)) {
          beanDefinition.registerExternallyManagedInitMethod(methodIdentifier);
          checkedInitMethods.add(element);
          if (log.isTraceEnabled()) {
            log.trace("Registered init method on class [{}]: {}",
                    targetClass.getName(), methodIdentifier);
          }
        }
      }
      var checkedDestroyMethods = new LinkedHashSet<LifecycleMethod>(destroyMethods.size());
      for (LifecycleMethod element : destroyMethods) {
        String methodIdentifier = element.getIdentifier();
        if (!beanDefinition.isExternallyManagedDestroyMethod(methodIdentifier)) {
          beanDefinition.registerExternallyManagedDestroyMethod(methodIdentifier);
          checkedDestroyMethods.add(element);
          if (log.isTraceEnabled()) {
            log.trace("Registered destroy method on class [{}]: {}",
                    targetClass.getName(), methodIdentifier);
          }
        }
      }
      this.checkedInitMethods = checkedInitMethods;
      this.checkedDestroyMethods = checkedDestroyMethods;
    }

    public void invokeInitMethods(Object target, String beanName) throws Throwable {
      Collection<LifecycleMethod> checkedInitMethods = this.checkedInitMethods;
      Collection<LifecycleMethod> initMethodsToIterate =
              checkedInitMethods != null ? checkedInitMethods : this.initMethods;
      if (!initMethodsToIterate.isEmpty()) {
        for (LifecycleMethod element : initMethodsToIterate) {
          if (log.isTraceEnabled()) {
            log.trace("Invoking init method on bean '{}': {}", beanName, element.getMethod());
          }
          element.invoke(target, dependencyInjector);
        }
      }
    }

    public void invokeDestroyMethods(Object target, String beanName) throws Throwable {
      Collection<LifecycleMethod> checkedDestroyMethods = this.checkedDestroyMethods;
      Collection<LifecycleMethod> destroyMethodsToUse =
              (checkedDestroyMethods != null ? checkedDestroyMethods : this.destroyMethods);
      if (!destroyMethodsToUse.isEmpty()) {
        for (LifecycleMethod element : destroyMethodsToUse) {
          if (log.isTraceEnabled()) {
            log.trace("Invoking destroy method on bean '{}': {}", beanName, element.getMethod());
          }
          element.invoke(target, dependencyInjector);
        }
      }
    }

    public boolean hasDestroyMethods() {
      Collection<LifecycleMethod> checkedDestroyMethods = this.checkedDestroyMethods;
      Collection<LifecycleMethod> destroyMethodsToUse =
              checkedDestroyMethods != null ? checkedDestroyMethods : this.destroyMethods;
      return !destroyMethodsToUse.isEmpty();
    }
  }

  /**
   * Class representing an annotated init or destroy method.
   */
  private static class LifecycleMethod implements OrderSourceProvider {

    private final Method method;
    private final String identifier;

    public LifecycleMethod(Method method, Class<?> beanClass) {
      if (method.getParameterCount() != 0) {
        throw new IllegalStateException("Lifecycle annotation requires a no-arg method: " + method);
      }
      ReflectionUtils.makeAccessible(method);
      this.method = method;
      this.identifier = isPrivateOrNotVisible(method, beanClass) ?
                        ClassUtils.getQualifiedMethodName(method) : method.getName();
    }

    public Method getMethod() {
      return this.method;
    }

    public String getIdentifier() {
      return this.identifier;
    }

    public void invoke(Object target, @Nullable DependencyInjector resolver) throws Throwable {
      if (resolver != null) {
        Object[] args = resolver.resolveArguments(method);
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
      if (!(other instanceof LifecycleMethod otherElement)) {
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

    /**
     * Determine if the supplied lifecycle {@link Method} is private or not
     * visible to the supplied bean {@link Class}.
     */
    private static boolean isPrivateOrNotVisible(Method method, Class<?> beanClass) {
      int modifiers = method.getModifiers();
      if (Modifier.isPrivate(modifiers)) {
        return true;
      }
      // Method is declared in a class that resides in a different package
      // than the bean class and the method is neither public nor protected?
      return !method.getDeclaringClass().getPackageName().equals(beanClass.getPackageName())
              && !(Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers));
    }

  }

}

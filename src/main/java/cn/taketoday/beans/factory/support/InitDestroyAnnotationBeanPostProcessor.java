package cn.taketoday.beans.factory.support;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/3 16:46
 */

import cn.taketoday.beans.DisposableBean;
import cn.taketoday.beans.InitializingBean;
import cn.taketoday.beans.factory.BeanCreationException;
import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.beans.factory.BeanDefinitionPostProcessor;
import cn.taketoday.beans.factory.BeanPostProcessor;
import cn.taketoday.beans.factory.BeansException;
import cn.taketoday.beans.factory.DestructionBeanPostProcessor;
import cn.taketoday.beans.factory.InitializationBeanPostProcessor;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.PriorityOrdered;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ReflectionUtils;

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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
 * <p>{@link CommonAnnotationBeanPostProcessor}
 * supports the {@link jakarta.annotation.PostConstruct} and {@link jakarta.annotation.PreDestroy}
 * annotations out of the box, as init annotation and destroy annotation, respectively.
 * Furthermore, it also supports the {@link jakarta.annotation.Resource} annotation
 * for annotation-driven injection of named beans.
 *
 * @author Juergen Hoeller
 * @see #setInitAnnotationType
 * @see #setDestroyAnnotationType
 * @since 4.0
 */
@SuppressWarnings("serial")
public class InitDestroyAnnotationBeanPostProcessor
        implements DestructionBeanPostProcessor, BeanDefinitionPostProcessor, InitializationBeanPostProcessor, PriorityOrdered, Serializable {
  private static final Logger log = LoggerFactory.getLogger(InitDestroyAnnotationBeanPostProcessor.class);

  private final transient LifecycleMetadata emptyLifecycleMetadata =
          new LifecycleMetadata(Object.class, Collections.emptyList(), Collections.emptyList()) {
            @Override
            public void checkConfigMembers(BeanDefinition beanDefinition) { }

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

  private int order = Ordered.LOWEST_PRECEDENCE;

  @Nullable
  private final transient ConcurrentHashMap<Class<?>, LifecycleMetadata> lifecycleMetadataCache = new ConcurrentHashMap<>(256);

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

  public void setOrder(int order) {
    this.order = order;
  }

  @Override
  public int getOrder() {
    return this.order;
  }

  @Override
  public void postProcessBeanDefinition(BeanDefinition beanDefinition, Class<?> beanType, String beanName) {
    LifecycleMetadata metadata = findLifecycleMetadata(beanType);
    metadata.checkConfigMembers(beanDefinition);
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
        log.warn(msg + ": " + ex.getTargetException());
      }
    }
    catch (Throwable ex) {
      log.warn("Failed to invoke destroy method on bean with name '" + beanName + "'", ex);
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
      synchronized(this.lifecycleMetadataCache) {
        metadata = this.lifecycleMetadataCache.get(clazz);
        if (metadata == null) {
          metadata = buildLifecycleMetadata(clazz);
          this.lifecycleMetadataCache.put(clazz, metadata);
        }
        return metadata;
      }
    }
    return metadata;
  }

  private LifecycleMetadata buildLifecycleMetadata(final Class<?> clazz) {
    if (!AnnotationUtils.isCandidateClass(clazz, Arrays.asList(this.initAnnotationType, this.destroyAnnotationType))) {
      return this.emptyLifecycleMetadata;
    }

    List<LifecycleElement> initMethods = new ArrayList<>();
    List<LifecycleElement> destroyMethods = new ArrayList<>();
    Class<?> targetClass = clazz;

    do {
      final List<LifecycleElement> currInitMethods = new ArrayList<>();
      final List<LifecycleElement> currDestroyMethods = new ArrayList<>();

      ReflectionUtils.doWithLocalMethods(targetClass, method -> {
        if (this.initAnnotationType != null && method.isAnnotationPresent(this.initAnnotationType)) {
          LifecycleElement element = new LifecycleElement(method);
          currInitMethods.add(element);
          if (log.isTraceEnabled()) {
            log.trace("Found init method on class [" + clazz.getName() + "]: " + method);
          }
        }
        if (this.destroyAnnotationType != null && method.isAnnotationPresent(this.destroyAnnotationType)) {
          currDestroyMethods.add(new LifecycleElement(method));
          if (log.isTraceEnabled()) {
            log.trace("Found destroy method on class [" + clazz.getName() + "]: " + method);
          }
        }
      });

      initMethods.addAll(0, currInitMethods);
      destroyMethods.addAll(currDestroyMethods);
      targetClass = targetClass.getSuperclass();
    }
    while (targetClass != null && targetClass != Object.class);

    return (initMethods.isEmpty() && destroyMethods.isEmpty() ? this.emptyLifecycleMetadata :
            new LifecycleMetadata(clazz, initMethods, destroyMethods));
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
  private static class LifecycleMetadata {

    private final Class<?> targetClass;

    private final Collection<LifecycleElement> initMethods;

    private final Collection<LifecycleElement> destroyMethods;

    @Nullable
    private volatile Set<LifecycleElement> checkedInitMethods;

    @Nullable
    private volatile Set<LifecycleElement> checkedDestroyMethods;

    public LifecycleMetadata(Class<?> targetClass, Collection<LifecycleElement> initMethods,
                             Collection<LifecycleElement> destroyMethods) {

      this.targetClass = targetClass;
      this.initMethods = initMethods;
      this.destroyMethods = destroyMethods;
    }

    public void checkConfigMembers(BeanDefinition beanDefinition) {
      Set<LifecycleElement> checkedInitMethods = new LinkedHashSet<>(this.initMethods.size());
      for (LifecycleElement element : this.initMethods) {
        checkedInitMethods.add(element);
        if (log.isTraceEnabled()) {
          log.trace("Registered init method on class [{}]: {}", this.targetClass.getName(), element);
        }
      }
      Set<LifecycleElement> checkedDestroyMethods = new LinkedHashSet<>(this.destroyMethods.size());
      for (LifecycleElement element : this.destroyMethods) {
        checkedDestroyMethods.add(element);
        if (log.isTraceEnabled()) {
          log.trace("Registered destroy method on class [{}]: {}", this.targetClass.getName(), element);
        }
      }
      this.checkedInitMethods = checkedInitMethods;
      this.checkedDestroyMethods = checkedDestroyMethods;
    }

    public void invokeInitMethods(Object target, String beanName) throws Throwable {
      Collection<LifecycleElement> checkedInitMethods = this.checkedInitMethods;
      Collection<LifecycleElement> initMethodsToIterate =
              (checkedInitMethods != null ? checkedInitMethods : this.initMethods);
      if (!initMethodsToIterate.isEmpty()) {
        for (LifecycleElement element : initMethodsToIterate) {
          if (log.isTraceEnabled()) {
            log.trace("Invoking init method on bean '{}': {}", beanName, element.getMethod());
          }
          element.invoke(target);
        }
      }
    }

    public void invokeDestroyMethods(Object target, String beanName) throws Throwable {
      Collection<LifecycleElement> checkedDestroyMethods = this.checkedDestroyMethods;
      Collection<LifecycleElement> destroyMethodsToUse =
              (checkedDestroyMethods != null ? checkedDestroyMethods : this.destroyMethods);
      if (!destroyMethodsToUse.isEmpty()) {
        for (LifecycleElement element : destroyMethodsToUse) {
          if (log.isTraceEnabled()) {
            log.trace("Invoking destroy method on bean '{}': {}", beanName, element.getMethod());
          }
          element.invoke(target);
        }
      }
    }

    public boolean hasDestroyMethods() {
      Collection<LifecycleElement> checkedDestroyMethods = this.checkedDestroyMethods;
      Collection<LifecycleElement> destroyMethodsToUse =
              (checkedDestroyMethods != null ? checkedDestroyMethods : this.destroyMethods);
      return !destroyMethodsToUse.isEmpty();
    }
  }

  /**
   * Class representing injection information about an annotated method.
   */
  private static class LifecycleElement {

    private final Method method;
    private final String identifier;

    public LifecycleElement(Method method) {
      if (method.getParameterCount() != 0) {
        throw new IllegalStateException("Lifecycle method annotation requires a no-arg method: " + method);
      }
      this.method = method;
      this.identifier = (Modifier.isPrivate(method.getModifiers()) ?
              ClassUtils.getQualifiedMethodName(method) : method.getName());
    }

    public Method getMethod() {
      return this.method;
    }

    public String getIdentifier() {
      return this.identifier;
    }

    public void invoke(Object target) throws Throwable {
      ReflectionUtils.makeAccessible(this.method);
      this.method.invoke(target, (Object[]) null);
    }

    @Override
    public boolean equals(@Nullable Object other) {
      if (this == other) {
        return true;
      }
      if (!(other instanceof LifecycleElement otherElement)) {
        return false;
      }
      return (this.identifier.equals(otherElement.identifier));
    }

    @Override
    public int hashCode() {
      return this.identifier.hashCode();
    }
  }

}

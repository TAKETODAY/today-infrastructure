/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.beans.BeanUtils;
import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.PropertyValues;
import cn.taketoday.beans.TypeConverter;
import cn.taketoday.beans.factory.BeanCreationException;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.beans.factory.DependenciesBeanPostProcessor;
import cn.taketoday.beans.factory.InjectionPoint;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.beans.factory.UnsatisfiedDependencyException;
import cn.taketoday.beans.factory.annotation.InjectionMetadata.InjectedElement;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.config.DependencyDescriptor;
import cn.taketoday.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import cn.taketoday.beans.factory.support.LookupOverride;
import cn.taketoday.beans.factory.support.MergedBeanDefinitionPostProcessor;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.core.BridgeMethodResolver;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.PriorityOrdered;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * {@link cn.taketoday.beans.factory.config.BeanPostProcessor BeanPostProcessor}
 * implementation that autowires annotated fields, setter methods, and arbitrary
 * config methods. Such members to be injected are detected through annotations:
 * by default, Framework's {@link Autowired @Autowired} and {@link Value @Value}
 * annotations.
 *
 * <p>Also supports the common {@link jakarta.inject.Inject @Inject} annotation,
 * if available, as a direct alternative to Framework's own {@code @Autowired}.
 * Additionally, it retains support for the {@code javax.inject.Inject} variant
 * dating back to the original JSR-330 specification (as known from Java EE 6-8).
 *
 * <h3>Autowired Constructors</h3>
 * <p>Only one constructor of any given bean class may declare this annotation with
 * the 'required' attribute set to {@code true}, indicating <i>the</i> constructor
 * to autowire when used as a Framework bean. Furthermore, if the 'required' attribute
 * is set to {@code true}, only a single constructor may be annotated with
 * {@code @Autowired}. If multiple <i>non-required</i> constructors declare the
 * annotation, they will be considered as candidates for autowiring. The constructor
 * with the greatest number of dependencies that can be satisfied by matching beans
 * in the Framework container will be chosen. If none of the candidates can be satisfied,
 * then a primary/default constructor (if present) will be used. If a class only
 * declares a single constructor to begin with, it will always be used, even if not
 * annotated. An annotated constructor does not have to be public.
 *
 * <h3>Autowired Fields</h3>
 * <p>Fields are injected right after construction of a bean, before any
 * config methods are invoked. Such a config field does not have to be public.
 *
 * <h3>Autowired Methods</h3>
 * <p>Config methods may have an arbitrary name and any number of arguments; each of
 * those arguments will be autowired with a matching bean in the Framework container.
 * Bean property setter methods are effectively just a special case of such a
 * general config method. Config methods do not have to be public.
 *
 * <h3>Annotation Config vs. XML Config</h3>
 * <p>A default {@code AutowiredAnnotationBeanPostProcessor} will be registered
 * by the "context:annotation-config" and "context:component-scan" XML tags.
 * Remove or turn off the default annotation configuration there if you intend
 * to specify a custom {@code AutowiredAnnotationBeanPostProcessor} bean definition.
 *
 * <p><b>NOTE:</b> Annotation injection will be performed <i>before</i> XML injection;
 * thus the latter configuration will override the former for properties wired through
 * both approaches.
 *
 * <h3>{@literal @}Lookup Methods</h3>
 * <p>In addition to regular injection points as discussed above, this post-processor
 * also handles Framework's {@link Lookup @Lookup} annotation which identifies lookup
 * methods to be replaced by the container at runtime. This is essentially a type-safe
 * version of {@code getBean(Class, args)} and {@code getBean(String, args)}.
 * See {@link Lookup @Lookup's javadoc} for details.
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Stephane Nicoll
 * @author Sebastien Deleuze
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #setAutowiredAnnotationType
 * @see Autowired
 * @see Value
 * @since 4.0 2022/3/20 21:12
 */
public class AutowiredAnnotationBeanPostProcessor implements SmartInstantiationAwareBeanPostProcessor,
        MergedBeanDefinitionPostProcessor, PriorityOrdered, BeanFactoryAware, DependenciesBeanPostProcessor {

  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final Set<Class<? extends Annotation>> autowiredAnnotationTypes = new LinkedHashSet<>(4);

  private String requiredParameterName = "required";

  private boolean requiredParameterValue = true;

  private int order = Ordered.LOWEST_PRECEDENCE - 2;

  @Nullable
  private ConfigurableBeanFactory beanFactory;

  private final Set<String> lookupMethodsChecked = Collections.newSetFromMap(new ConcurrentHashMap<>(256));

  private final Map<Class<?>, Constructor<?>[]> candidateConstructorsCache = new ConcurrentHashMap<>(256);

  private final Map<String, InjectionMetadata> injectionMetadataCache = new ConcurrentHashMap<>(256);

  /**
   * Create a new {@code AutowiredAnnotationBeanPostProcessor} for Framework's
   * standard {@link Autowired @Autowired} and {@link Value @Value} annotations.
   * <p>Also supports the common {@link jakarta.inject.Inject @Inject} annotation,
   * if available, as well as the original {@code javax.inject.Inject} variant.
   */
  public AutowiredAnnotationBeanPostProcessor() {
    this.autowiredAnnotationTypes.add(Autowired.class);
    this.autowiredAnnotationTypes.add(Value.class);

    try {
      this.autowiredAnnotationTypes.add(ClassUtils.forName("jakarta.inject.Inject", AutowiredAnnotationBeanPostProcessor.class.getClassLoader()));
      log.trace("'jakarta.inject.Inject' annotation found and supported for autowiring");
    }
    catch (ClassNotFoundException ex) {
      // jakarta.inject API not available - simply skip.
    }

    try {
      this.autowiredAnnotationTypes.add(ClassUtils.forName("javax.inject.Inject", AutowiredAnnotationBeanPostProcessor.class.getClassLoader()));
      log.trace("'javax.inject.Inject' annotation found and supported for autowiring");
    }
    catch (ClassNotFoundException ex) {
      // javax.inject API not available - simply skip.
    }
  }

  /**
   * Set the 'autowired' annotation type, to be used on constructors, fields,
   * setter methods, and arbitrary config methods.
   * <p>The default autowired annotation types are the Framework-provided
   * {@link Autowired @Autowired} and {@link Value @Value} annotations as well
   * as the common {@code @Inject} annotation, if available.
   * <p>This setter property exists so that developers can provide their own
   * (non-Framework-specific) annotation type to indicate that a member is supposed
   * to be autowired.
   */
  public void setAutowiredAnnotationType(Class<? extends Annotation> autowiredAnnotationType) {
    Assert.notNull(autowiredAnnotationType, "'autowiredAnnotationType' must not be null");
    this.autowiredAnnotationTypes.clear();
    this.autowiredAnnotationTypes.add(autowiredAnnotationType);
  }

  /**
   * Set the 'autowired' annotation types, to be used on constructors, fields,
   * setter methods, and arbitrary config methods.
   * <p>The default autowired annotation types are the Framework-provided
   * {@link Autowired @Autowired} and {@link Value @Value} annotations as well
   * as the common {@code @Inject} annotation, if available.
   * <p>This setter property exists so that developers can provide their own
   * (non-Framework-specific) annotation types to indicate that a member is supposed
   * to be autowired.
   */
  public void setAutowiredAnnotationTypes(Set<Class<? extends Annotation>> autowiredAnnotationTypes) {
    Assert.notEmpty(autowiredAnnotationTypes, "'autowiredAnnotationTypes' must not be empty");
    this.autowiredAnnotationTypes.clear();
    this.autowiredAnnotationTypes.addAll(autowiredAnnotationTypes);
  }

  /**
   * Set the name of an attribute of the annotation that specifies whether it is required.
   *
   * @see #setRequiredParameterValue(boolean)
   */
  public void setRequiredParameterName(String requiredParameterName) {
    this.requiredParameterName = requiredParameterName;
  }

  /**
   * Set the boolean value that marks a dependency as required.
   * <p>For example if using 'required=true' (the default), this value should be
   * {@code true}; but if using 'optional=false', this value should be {@code false}.
   *
   * @see #setRequiredParameterName(String)
   */
  public void setRequiredParameterValue(boolean requiredParameterValue) {
    this.requiredParameterValue = requiredParameterValue;
  }

  public void setOrder(int order) {
    this.order = order;
  }

  @Override
  public int getOrder() {
    return this.order;
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    if (!(beanFactory instanceof ConfigurableBeanFactory)) {
      throw new IllegalArgumentException(
              "AutowiredAnnotationBeanPostProcessor requires a ConfigurableBeanFactory: " + beanFactory);
    }
    this.beanFactory = (ConfigurableBeanFactory) beanFactory;
  }

  @Override
  public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Object bean, String beanName) {
    // prepare InjectionMetadata
    if (beanDefinition.isEnableDependencyInjection()) {
      InjectionMetadata metadata = findAutowiringMetadata(beanName, bean.getClass(), null);
      metadata.checkConfigMembers(beanDefinition);
    }
  }

  @Override
  public void resetBeanDefinition(String beanName) {
    this.lookupMethodsChecked.remove(beanName);
    this.injectionMetadataCache.remove(beanName);
  }

  @Override
  @Nullable
  public Constructor<?>[] determineCandidateConstructors(Class<?> beanClass, final String beanName)
          throws BeanCreationException {

    // Let's check for lookup methods here...
    if (!this.lookupMethodsChecked.contains(beanName)) {
      if (AnnotationUtils.isCandidateClass(beanClass, Lookup.class)) {
        try {
          Class<?> targetClass = beanClass;
          do {
            ReflectionUtils.doWithLocalMethods(targetClass, method -> {
              Lookup lookup = method.getAnnotation(Lookup.class);
              if (lookup != null) {
                Assert.state(this.beanFactory != null, "No BeanFactory available");
                LookupOverride override = new LookupOverride(method, lookup.value());
                try {
                  RootBeanDefinition mbd = (RootBeanDefinition)
                          this.beanFactory.getMergedBeanDefinition(beanName);
                  mbd.getMethodOverrides().addOverride(override);
                }
                catch (NoSuchBeanDefinitionException ex) {
                  throw new BeanCreationException(beanName,
                          "Cannot apply @Lookup to beans without corresponding bean definition");
                }
              }
            });
            targetClass = targetClass.getSuperclass();
          }
          while (targetClass != null && targetClass != Object.class);

        }
        catch (IllegalStateException ex) {
          throw new BeanCreationException(beanName, "Lookup method resolution failed", ex);
        }
      }
      this.lookupMethodsChecked.add(beanName);
    }

    // Quick check on the concurrent map first, with minimal locking.
    Constructor<?>[] candidateConstructors = this.candidateConstructorsCache.get(beanClass);
    if (candidateConstructors == null) {
      // Fully synchronized resolution now...
      synchronized(this.candidateConstructorsCache) {
        candidateConstructors = this.candidateConstructorsCache.get(beanClass);
        if (candidateConstructors == null) {
          Constructor<?>[] rawCandidates;
          try {
            rawCandidates = beanClass.getDeclaredConstructors();
          }
          catch (Throwable ex) {
            throw new BeanCreationException(beanName,
                    "Resolution of declared constructors on bean Class [" + beanClass.getName() +
                            "] from ClassLoader [" + beanClass.getClassLoader() + "] failed", ex);
          }
          List<Constructor<?>> candidates = new ArrayList<>(rawCandidates.length);
          Constructor<?> requiredConstructor = null;
          Constructor<?> defaultConstructor = null;
          for (Constructor<?> candidate : rawCandidates) {
            MergedAnnotation<?> ann = findAutowiredAnnotation(candidate);
            if (ann == null) {
              Class<?> userClass = ClassUtils.getUserClass(beanClass);
              if (userClass != beanClass) {
                try {
                  Constructor<?> superCtor =
                          userClass.getDeclaredConstructor(candidate.getParameterTypes());
                  ann = findAutowiredAnnotation(superCtor);
                }
                catch (NoSuchMethodException ex) {
                  // Simply proceed, no equivalent superclass constructor found...
                }
              }
            }
            if (ann != null) {
              if (requiredConstructor != null) {
                throw new BeanCreationException(beanName,
                        "Invalid autowire-marked constructor: " + candidate +
                                ". Found constructor with 'required' Autowired annotation already: " +
                                requiredConstructor);
              }
              boolean required = determineRequiredStatus(ann);
              if (required) {
                if (!candidates.isEmpty()) {
                  throw new BeanCreationException(beanName,
                          "Invalid autowire-marked constructors: " + candidates +
                                  ". Found constructor with 'required' Autowired annotation: " +
                                  candidate);
                }
                requiredConstructor = candidate;
              }
              candidates.add(candidate);
            }
            else if (candidate.getParameterCount() == 0) {
              defaultConstructor = candidate;
            }
          }
          if (!candidates.isEmpty()) {
            // Add default constructor to list of optional constructors, as fallback.
            if (requiredConstructor == null) {
              if (defaultConstructor != null) {
                candidates.add(defaultConstructor);
              }
              else if (candidates.size() == 1 && log.isInfoEnabled()) {
                log.info("Inconsistent constructor declaration on bean with name '{}': single autowire-marked constructor flagged as optional - " +
                        "this constructor is effectively required since there is no " +
                        "default constructor to fall back to: {}", beanName, candidates.get(0));
              }
            }
            candidateConstructors = candidates.toArray(new Constructor<?>[0]);
          }
          else if (rawCandidates.length == 1 && rawCandidates[0].getParameterCount() > 0) {
            candidateConstructors = new Constructor<?>[] { rawCandidates[0] };
          }
          else {
            candidateConstructors = new Constructor<?>[0];
          }
          this.candidateConstructorsCache.put(beanClass, candidateConstructors);
        }
      }
    }
    return (candidateConstructors.length > 0 ? candidateConstructors : null);
  }

  @Nullable
  @Override
  public PropertyValues processDependencies(@Nullable PropertyValues pvs, Object bean, String beanName) {
    InjectionMetadata metadata = findAutowiringMetadata(beanName, bean.getClass(), pvs);
    try {
      metadata.inject(bean, beanName, pvs);
    }
    catch (BeanCreationException ex) {
      throw ex;
    }
    catch (Throwable ex) {
      throw new BeanCreationException(beanName, "Injection of autowired dependencies failed", ex);
    }
    return pvs;
  }

  /**
   * 'Native' processing method for direct calls with an arbitrary target instance,
   * resolving all of its fields and methods which are annotated with one of the
   * configured 'autowired' annotation types.
   *
   * @param bean the target instance to process
   * @throws BeanCreationException if autowiring failed
   * @see #setAutowiredAnnotationTypes(Set)
   */
  public void processInjection(Object bean) throws BeanCreationException {
    Class<?> clazz = bean.getClass();
    InjectionMetadata metadata = findAutowiringMetadata(clazz.getName(), clazz, null);
    try {
      metadata.inject(bean, null, null);
    }
    catch (BeanCreationException ex) {
      throw ex;
    }
    catch (Throwable ex) {
      throw new BeanCreationException(
              "Injection of autowired dependencies failed for class [" + clazz + "]", ex);
    }
  }

  private InjectionMetadata findAutowiringMetadata(String beanName, Class<?> clazz, @Nullable PropertyValues pvs) {
    // Fall back to class name as cache key, for backwards compatibility with custom callers.
    String cacheKey = StringUtils.isNotEmpty(beanName) ? beanName : clazz.getName();
    // Quick check on the concurrent map first, with minimal locking.
    InjectionMetadata metadata = this.injectionMetadataCache.get(cacheKey);
    if (InjectionMetadata.needsRefresh(metadata, clazz)) {
      synchronized(this.injectionMetadataCache) {
        metadata = this.injectionMetadataCache.get(cacheKey);
        if (InjectionMetadata.needsRefresh(metadata, clazz)) {
          if (metadata != null) {
            metadata.clear(pvs);
          }
          metadata = buildAutowiringMetadata(clazz);
          this.injectionMetadataCache.put(cacheKey, metadata);
        }
      }
    }
    return metadata;
  }

  private InjectionMetadata buildAutowiringMetadata(Class<?> clazz) {
    if (!AnnotationUtils.isCandidateClass(clazz, this.autowiredAnnotationTypes)) {
      return InjectionMetadata.EMPTY;
    }

    ArrayList<InjectedElement> elements = new ArrayList<>();
    Class<?> targetClass = clazz;

    do {
      final ArrayList<InjectedElement> currElements = new ArrayList<>();

      ReflectionUtils.doWithLocalFields(targetClass, field -> {
        MergedAnnotation<?> ann = findAutowiredAnnotation(field);
        if (ann != null) {
          if (Modifier.isStatic(field.getModifiers())) {
            log.warn("Autowired annotation is not supported on static fields: {}", field);
            return;
          }
          boolean required = determineRequiredStatus(ann);
          currElements.add(new AutowiredFieldElement(field, required));
        }
      });

      ReflectionUtils.doWithLocalMethods(targetClass, method -> {
        Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(method);
        if (!BridgeMethodResolver.isVisibilityBridgeMethodPair(method, bridgedMethod)) {
          return;
        }
        MergedAnnotation<?> ann = findAutowiredAnnotation(bridgedMethod);
        if (ann != null && method.equals(ReflectionUtils.getMostSpecificMethod(method, clazz))) {
          if (Modifier.isStatic(method.getModifiers())) {
            log.warn("Autowired annotation is not supported on static methods: {}", method);
            return;
          }
          if (method.getParameterCount() == 0) {
            log.warn("Autowired annotation should only be used on methods with parameters: {}", method);
          }
          boolean required = determineRequiredStatus(ann);
          PropertyDescriptor pd = BeanUtils.findPropertyForMethod(bridgedMethod, clazz);
          currElements.add(new AutowiredMethodElement(method, required, pd));
        }
      });

      elements.addAll(0, currElements);
      targetClass = targetClass.getSuperclass();
    }
    while (targetClass != null && targetClass != Object.class);

    return InjectionMetadata.forElements(elements, clazz);
  }

  @Nullable
  private MergedAnnotation<?> findAutowiredAnnotation(AccessibleObject ao) {
    MergedAnnotations annotations = MergedAnnotations.from(ao);
    for (Class<? extends Annotation> type : this.autowiredAnnotationTypes) {
      MergedAnnotation<?> annotation = annotations.get(type);
      if (annotation.isPresent()) {
        return annotation;
      }
    }
    return null;
  }

  /**
   * Determine if the annotated field or method requires its dependency.
   * <p>A 'required' dependency means that autowiring should fail when no beans
   * are found. Otherwise, the autowiring process will simply bypass the field
   * or method when no beans are found.
   *
   * @param ann the Autowired annotation
   * @return whether the annotation indicates that a dependency is required
   */
  protected boolean determineRequiredStatus(MergedAnnotation<?> ann) {
    return ann.getValue(requiredParameterName, boolean.class)
            .map(value -> requiredParameterValue == value)
            .orElse(true);
  }

  /**
   * Obtain all beans of the given type as autowire candidates.
   *
   * @param type the type of the bean
   * @return the target beans, or an empty Collection if no bean of this type is found
   * @throws BeansException if bean retrieval failed
   */
  protected <T> Map<String, T> findAutowireCandidates(Class<T> type) throws BeansException {
    if (this.beanFactory == null) {
      throw new IllegalStateException("No BeanFactory configured - " +
              "override the getBeanOfType method or specify the 'beanFactory' property");
    }
    return BeanFactoryUtils.beansOfTypeIncludingAncestors(this.beanFactory, type);
  }

  /**
   * Register the specified bean as dependent on the autowired beans.
   */
  private void registerDependentBeans(@Nullable String beanName, Set<String> autowiredBeanNames) {
    if (beanName != null) {
      for (String autowiredBeanName : autowiredBeanNames) {
        if (this.beanFactory != null && this.beanFactory.containsBean(autowiredBeanName)) {
          this.beanFactory.registerDependentBean(autowiredBeanName, beanName);
        }
        if (log.isTraceEnabled()) {
          log.trace("Autowiring by type from bean name '{}' to bean named '{}'", beanName, autowiredBeanName);
        }
      }
    }
  }

  /**
   * Resolve the specified cached method argument or field value.
   */
  @Nullable
  private Object resolvedCachedArgument(@Nullable String beanName, @Nullable Object cachedArgument) {
    if (cachedArgument instanceof DependencyDescriptor descriptor) {
      Assert.state(this.beanFactory != null, "No BeanFactory available");
      return this.beanFactory.resolveDependency(descriptor, beanName, null, null);
    }
    else {
      return cachedArgument;
    }
  }

  /**
   * Class representing injection information about an annotated field.
   */
  private class AutowiredFieldElement extends InjectedElement {

    private final boolean required;

    private volatile boolean cached;

    @Nullable
    private volatile Object cachedFieldValue;

    public AutowiredFieldElement(Field field, boolean required) {
      super(field, null);
      this.required = required;
    }

    @Override
    protected void inject(Object bean, @Nullable String beanName, @Nullable PropertyValues pvs) throws Throwable {
      Field field = (Field) this.member;
      Object value;
      if (this.cached) {
        try {
          value = resolvedCachedArgument(beanName, this.cachedFieldValue);
        }
        catch (NoSuchBeanDefinitionException ex) {
          // Unexpected removal of target bean for cached argument -> re-resolve
          value = resolveFieldValue(field, bean, beanName);
        }
      }
      else {
        value = resolveFieldValue(field, bean, beanName);
      }
      if (value != null) {
        ReflectionUtils.makeAccessible(field);
        field.set(bean, value);
      }
    }

    @Nullable
    private Object resolveFieldValue(Field field, Object bean, @Nullable String beanName) {
      DependencyDescriptor desc = new DependencyDescriptor(field, this.required);
      desc.setContainingClass(bean.getClass());
      Set<String> autowiredBeanNames = new LinkedHashSet<>(1);
      Assert.state(beanFactory != null, "No BeanFactory available");
      TypeConverter typeConverter = beanFactory.getTypeConverter();
      Object value;
      try {
        value = beanFactory.resolveDependency(desc, beanName, autowiredBeanNames, typeConverter);
      }
      catch (BeansException ex) {
        throw new UnsatisfiedDependencyException(null, beanName, new InjectionPoint(field), ex);
      }
      synchronized(this) {
        if (!this.cached) {
          Object cachedFieldValue = null;
          if (value != null || this.required) {
            cachedFieldValue = desc;
            registerDependentBeans(beanName, autowiredBeanNames);
            if (value != null && autowiredBeanNames.size() == 1) {
              String autowiredBeanName = autowiredBeanNames.iterator().next();
              if (beanFactory.containsBean(autowiredBeanName) &&
                      beanFactory.isTypeMatch(autowiredBeanName, field.getType())) {
                cachedFieldValue = new ShortcutDependencyDescriptor(
                        desc, autowiredBeanName, field.getType());
              }
            }
          }
          this.cachedFieldValue = cachedFieldValue;
          this.cached = true;
        }
      }
      return value;
    }
  }

  /**
   * Class representing injection information about an annotated method.
   */
  private class AutowiredMethodElement extends InjectedElement {

    private final boolean required;

    private volatile boolean cached;

    @Nullable
    private volatile Object[] cachedMethodArguments;

    public AutowiredMethodElement(Method method, boolean required, @Nullable PropertyDescriptor pd) {
      super(method, pd);
      this.required = required;
    }

    @Override
    protected void inject(Object bean, @Nullable String beanName, @Nullable PropertyValues pvs) throws Throwable {
      if (checkPropertySkipping(pvs)) {
        return;
      }
      Method method = (Method) this.member;
      Object[] arguments;
      if (this.cached) {
        try {
          arguments = resolveCachedArguments(beanName);
        }
        catch (NoSuchBeanDefinitionException ex) {
          // Unexpected removal of target bean for cached argument -> re-resolve
          arguments = resolveMethodArguments(method, bean, beanName);
        }
      }
      else {
        arguments = resolveMethodArguments(method, bean, beanName);
      }
      if (arguments != null) {
        try {
          ReflectionUtils.makeAccessible(method);
          method.invoke(bean, arguments);
        }
        catch (InvocationTargetException ex) {
          throw ex.getTargetException();
        }
      }
    }

    @Nullable
    private Object[] resolveCachedArguments(@Nullable String beanName) {
      Object[] cachedMethodArguments = this.cachedMethodArguments;
      if (cachedMethodArguments == null) {
        return null;
      }
      Object[] arguments = new Object[cachedMethodArguments.length];
      for (int i = 0; i < arguments.length; i++) {
        arguments[i] = resolvedCachedArgument(beanName, cachedMethodArguments[i]);
      }
      return arguments;
    }

    @Nullable
    private Object[] resolveMethodArguments(Method method, Object bean, @Nullable String beanName) {
      int argumentCount = method.getParameterCount();
      Object[] arguments = new Object[argumentCount];
      DependencyDescriptor[] descriptors = new DependencyDescriptor[argumentCount];
      Set<String> autowiredBeans = new LinkedHashSet<>(argumentCount);
      Assert.state(beanFactory != null, "No BeanFactory available");
      TypeConverter typeConverter = beanFactory.getTypeConverter();
      for (int i = 0; i < arguments.length; i++) {
        MethodParameter methodParam = new MethodParameter(method, i);
        DependencyDescriptor currDesc = new DependencyDescriptor(methodParam, this.required);
        currDesc.setContainingClass(bean.getClass());
        descriptors[i] = currDesc;
        try {
          Object arg = beanFactory.resolveDependency(currDesc, beanName, autowiredBeans, typeConverter);
          if (arg == null && !this.required) {
            arguments = null;
            break;
          }
          arguments[i] = arg;
        }
        catch (BeansException ex) {
          throw new UnsatisfiedDependencyException(null, beanName, new InjectionPoint(methodParam), ex);
        }
      }
      synchronized(this) {
        if (!this.cached) {
          if (arguments != null) {
            DependencyDescriptor[] cachedMethodArguments = Arrays.copyOf(descriptors, arguments.length);
            registerDependentBeans(beanName, autowiredBeans);
            if (autowiredBeans.size() == argumentCount) {
              Iterator<String> it = autowiredBeans.iterator();
              Class<?>[] paramTypes = method.getParameterTypes();
              for (int i = 0; i < paramTypes.length; i++) {
                String autowiredBeanName = it.next();
                if (beanFactory.containsBean(autowiredBeanName)
                        && beanFactory.isTypeMatch(autowiredBeanName, paramTypes[i])) {
                  cachedMethodArguments[i] = new ShortcutDependencyDescriptor(
                          descriptors[i], autowiredBeanName, paramTypes[i]);
                }
              }
            }
            this.cachedMethodArguments = cachedMethodArguments;
          }
          else {
            this.cachedMethodArguments = null;
          }
          this.cached = true;
        }
      }
      return arguments;
    }
  }

  /**
   * DependencyDescriptor variant with a pre-resolved target bean name.
   */
  @SuppressWarnings("serial")
  private static class ShortcutDependencyDescriptor extends DependencyDescriptor {

    private final String shortcut;

    private final Class<?> requiredType;

    public ShortcutDependencyDescriptor(DependencyDescriptor original, String shortcut, Class<?> requiredType) {
      super(original);
      this.shortcut = shortcut;
      this.requiredType = requiredType;
    }

    @Override
    public Object resolveShortcut(BeanFactory beanFactory) {
      return beanFactory.getBean(this.shortcut, this.requiredType);
    }
  }

}

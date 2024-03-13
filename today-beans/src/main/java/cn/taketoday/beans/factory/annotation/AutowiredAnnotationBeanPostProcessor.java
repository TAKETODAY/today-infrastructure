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

package cn.taketoday.beans.factory.annotation;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.aot.generate.AccessControl;
import cn.taketoday.aot.generate.GeneratedClass;
import cn.taketoday.aot.generate.GeneratedMethod;
import cn.taketoday.aot.generate.GenerationContext;
import cn.taketoday.aot.hint.ExecutableMode;
import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.hint.support.ClassHintUtils;
import cn.taketoday.beans.BeanUtils;
import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.PropertyValues;
import cn.taketoday.beans.TypeConverter;
import cn.taketoday.beans.factory.BeanCreationException;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.DependenciesBeanPostProcessor;
import cn.taketoday.beans.factory.InjectionPoint;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.beans.factory.UnsatisfiedDependencyException;
import cn.taketoday.beans.factory.annotation.InjectionMetadata.InjectedElement;
import cn.taketoday.beans.factory.aot.AutowiredArgumentsCodeGenerator;
import cn.taketoday.beans.factory.aot.AutowiredFieldValueResolver;
import cn.taketoday.beans.factory.aot.AutowiredMethodArgumentsResolver;
import cn.taketoday.beans.factory.aot.BeanRegistrationAotContribution;
import cn.taketoday.beans.factory.aot.BeanRegistrationAotProcessor;
import cn.taketoday.beans.factory.aot.BeanRegistrationCode;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.config.DependencyDescriptor;
import cn.taketoday.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import cn.taketoday.beans.factory.support.AbstractAutowireCapableBeanFactory;
import cn.taketoday.beans.factory.support.AutowireCandidateResolver;
import cn.taketoday.beans.factory.support.LookupOverride;
import cn.taketoday.beans.factory.support.MergedBeanDefinitionPostProcessor;
import cn.taketoday.beans.factory.support.RegisteredBean;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.core.BridgeMethodResolver;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.PriorityOrdered;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.core.type.MethodMetadata;
import cn.taketoday.core.type.classreading.MetadataReaderFactory;
import cn.taketoday.core.type.classreading.SimpleMetadataReaderFactory;
import cn.taketoday.javapoet.ClassName;
import cn.taketoday.javapoet.CodeBlock;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * {@link cn.taketoday.beans.factory.config.BeanPostProcessor BeanPostProcessor}
 * implementation that autowires annotated fields, setter methods, and arbitrary
 * config methods. Such members to be injected are detected through annotations:
 * by default, Infra {@link Autowired @Autowired} and {@link Value @Value}
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
 * also handles Infra {@link Lookup @Lookup} annotation which identifies lookup
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
        MergedBeanDefinitionPostProcessor, BeanRegistrationAotProcessor, PriorityOrdered, BeanFactoryAware, DependenciesBeanPostProcessor {

  private static final Constructor<?>[] EMPTY_CONSTRUCTOR_ARRAY = new Constructor<?>[0];

  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final LinkedHashSet<Class<? extends Annotation>> autowiredAnnotationTypes = new LinkedHashSet<>(4);

  private String requiredParameterName = "required";

  private boolean requiredParameterValue = true;

  private int order = Ordered.LOWEST_PRECEDENCE - 2;

  @Nullable
  private ConfigurableBeanFactory beanFactory;

  @Nullable
  private MetadataReaderFactory metadataReaderFactory;

  private final Set<String> lookupMethodsChecked = ConcurrentHashMap.newKeySet(256);

  private final ConcurrentHashMap<Class<?>, Constructor<?>[]> candidateConstructorsCache = new ConcurrentHashMap<>(256);

  private final ConcurrentHashMap<String, InjectionMetadata> injectionMetadataCache = new ConcurrentHashMap<>(256);

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
      this.autowiredAnnotationTypes.add(ClassUtils.forName(
              "jakarta.inject.Inject", AutowiredAnnotationBeanPostProcessor.class.getClassLoader()));
      log.trace("'jakarta.inject.Inject' annotation found and supported for autowiring");
    }
    catch (ClassNotFoundException ex) {
      // jakarta.inject API not available - simply skip.
    }

    try {
      this.autowiredAnnotationTypes.add(ClassUtils.forName(
              "javax.inject.Inject", AutowiredAnnotationBeanPostProcessor.class.getClassLoader()));
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
    Assert.notNull(autowiredAnnotationType, "'autowiredAnnotationType' is required");
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
    if (!(beanFactory instanceof ConfigurableBeanFactory cbf)) {
      throw new IllegalArgumentException(
              "AutowiredAnnotationBeanPostProcessor requires a ConfigurableBeanFactory: " + beanFactory);
    }
    this.beanFactory = cbf;
    this.metadataReaderFactory = new SimpleMetadataReaderFactory(cbf.getBeanClassLoader());
  }

  @Override
  public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
    if (beanDefinition.isEnableDependencyInjection()) {
      // Register externally managed config members on bean definition.
      findInjectionMetadata(beanName, beanType, beanDefinition);

      // Use opportunity to clear caches which are not needed after singleton instantiation.
      // The injectionMetadataCache itself is left intact since it cannot be reliably
      // reconstructed in terms of externally managed config members otherwise.
      if (beanDefinition.isSingleton()) {
        this.candidateConstructorsCache.remove(beanType);
        // With actual lookup overrides, keep it intact along with bean definition.
        if (!beanDefinition.hasMethodOverrides()) {
          this.lookupMethodsChecked.remove(beanName);
        }
      }
    }
  }

  @Override
  public void resetBeanDefinition(String beanName) {
    this.lookupMethodsChecked.remove(beanName);
    this.injectionMetadataCache.remove(beanName);
  }

  @Override
  @Nullable
  public BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
    RootBeanDefinition beanDefinition = registeredBean.getMergedBeanDefinition();
    if (beanDefinition.isEnableDependencyInjection()) {
      String beanName = registeredBean.getBeanName();
      Class<?> beanClass = registeredBean.getBeanClass();
      InjectionMetadata metadata = findInjectionMetadata(beanName, beanClass, beanDefinition);
      Collection<AutowiredElement> autowiredElements = getAutowiredElements(metadata,
              beanDefinition.getPropertyValues());
      if (CollectionUtils.isNotEmpty(autowiredElements)) {
        return new AotContribution(beanClass, autowiredElements, getAutowireCandidateResolver());
      }
    }
    return null;
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private Collection<AutowiredElement> getAutowiredElements(InjectionMetadata metadata, PropertyValues propertyValues) {
    return (Collection) metadata.getInjectedElements(propertyValues);
  }

  @Nullable
  private AutowireCandidateResolver getAutowireCandidateResolver() {
    if (this.beanFactory instanceof StandardBeanFactory lbf) {
      return lbf.getAutowireCandidateResolver();
    }
    return null;
  }

  private InjectionMetadata findInjectionMetadata(String beanName, Class<?> beanType, RootBeanDefinition beanDefinition) {
    InjectionMetadata metadata = findAutowiringMetadata(beanName, beanType, null);
    metadata.checkConfigMembers(beanDefinition);
    return metadata;
  }

  @Override
  public Class<?> determineBeanType(Class<?> beanClass, String beanName) throws BeanCreationException {
    checkLookupMethods(beanClass, beanName);

    // Pick up subclass with fresh lookup method override from above
    if (this.beanFactory instanceof AbstractAutowireCapableBeanFactory aacBeanFactory) {
      RootBeanDefinition mbd = (RootBeanDefinition) this.beanFactory.getMergedBeanDefinition(beanName);
      if (mbd.getFactoryMethodName() == null && mbd.hasBeanClass()) {
        return aacBeanFactory.getInstantiationStrategy().getActualBeanClass(mbd, beanName, aacBeanFactory);
      }
    }
    return beanClass;
  }

  @Override
  @Nullable
  public Constructor<?>[] determineCandidateConstructors(Class<?> beanClass, final String beanName)
          throws BeanCreationException {
    checkLookupMethods(beanClass, beanName);

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
                    "Resolution of declared constructors on bean Class [%s] from ClassLoader [%s] failed"
                            .formatted(beanClass.getName(), beanClass.getClassLoader()), ex);
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
                        "Invalid autowire-marked constructor: %s. Found constructor with 'required' Autowired annotation already: %s"
                                .formatted(candidate, requiredConstructor));
              }
              boolean required = determineRequiredStatus(ann);
              if (required) {
                if (!candidates.isEmpty()) {
                  throw new BeanCreationException(beanName,
                          "Invalid autowire-marked constructors: %s. Found constructor with 'required' Autowired annotation: %s"
                                  .formatted(candidates, candidate));
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
            candidateConstructors = candidates.toArray(EMPTY_CONSTRUCTOR_ARRAY);
          }
          else if (rawCandidates.length == 1 && rawCandidates[0].getParameterCount() > 0) {
            candidateConstructors = new Constructor<?>[] { rawCandidates[0] };
          }
          else {
            candidateConstructors = EMPTY_CONSTRUCTOR_ARRAY;
          }
          this.candidateConstructorsCache.put(beanClass, candidateConstructors);
        }
      }
    }
    return (candidateConstructors.length > 0 ? candidateConstructors : null);
  }

  private void checkLookupMethods(Class<?> beanClass, final String beanName) throws BeanCreationException {
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
              "Injection of autowired dependencies failed for class [%s]".formatted(clazz), ex);
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

    final ArrayList<InjectedElement> elements = new ArrayList<>();
    Class<?> targetClass = clazz;

    do {
      final ArrayList<InjectedElement> fieldElements = new ArrayList<>();

      ReflectionUtils.doWithLocalFields(targetClass, field -> {
        MergedAnnotation<?> ann = findAutowiredAnnotation(field);
        if (ann != null) {
          if (Modifier.isStatic(field.getModifiers())) {
            log.info("Autowired annotation is not supported on static fields: {}", field);
            return;
          }
          boolean required = determineRequiredStatus(ann);
          fieldElements.add(new AutowiredFieldElement(field, required));
        }
      });

      final ArrayList<InjectedElement> methodElements = new ArrayList<>();
      ReflectionUtils.doWithLocalMethods(targetClass, method -> {
        Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(method);
        if (!BridgeMethodResolver.isVisibilityBridgeMethodPair(method, bridgedMethod)) {
          return;
        }
        MergedAnnotation<?> ann = findAutowiredAnnotation(bridgedMethod);
        if (ann != null && method.equals(ReflectionUtils.getMostSpecificMethod(method, clazz))) {
          if (Modifier.isStatic(method.getModifiers())) {
            log.info("Autowired annotation is not supported on static methods: {}", method);
            return;
          }
          if (method.getParameterCount() == 0) {
            if (method.getDeclaringClass().isRecord()) {
              // Annotations on the compact constructor arguments made available on accessors, ignoring.
              return;
            }
            log.info("Autowired annotation should only be used on methods with parameters: {}", method);
          }
          boolean required = determineRequiredStatus(ann);
          PropertyDescriptor pd = BeanUtils.findPropertyForMethod(bridgedMethod, clazz);
          methodElements.add(new AutowiredMethodElement(method, required, pd));
        }
      });

      elements.addAll(0, sortMethodElements(methodElements, targetClass));
      elements.addAll(0, fieldElements);
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
   * Sort the method elements via ASM for deterministic declaration order if possible.
   */
  private List<InjectedElement> sortMethodElements(List<InjectedElement> methodElements, Class<?> targetClass) {
    if (methodElements.size() > 1) {
      if (metadataReaderFactory == null) {
        metadataReaderFactory = new SimpleMetadataReaderFactory(beanFactory != null
                ? beanFactory.getBeanClassLoader() : ClassUtils.getDefaultClassLoader());
      }
      // Try reading the class file via ASM for deterministic declaration order...
      // Unfortunately, the JVM's standard reflection returns methods in arbitrary
      // order, even between different runs of the same application on the same JVM.
      try {
        AnnotationMetadata asm = metadataReaderFactory.getMetadataReader(targetClass.getName()).getAnnotationMetadata();
        Set<MethodMetadata> asmMethods = asm.getAnnotatedMethods(Autowired.class.getName());
        if (asmMethods.size() >= methodElements.size()) {
          ArrayList<InjectedElement> candidateMethods = new ArrayList<>(methodElements);
          ArrayList<InjectedElement> selectedMethods = new ArrayList<>(asmMethods.size());
          for (MethodMetadata asmMethod : asmMethods) {
            for (Iterator<InjectedElement> it = candidateMethods.iterator(); it.hasNext(); ) {
              InjectedElement element = it.next();
              if (element.getMember().getName().equals(asmMethod.getMethodName())) {
                selectedMethods.add(element);
                it.remove();
                break;
              }
            }
          }
          if (selectedMethods.size() == methodElements.size()) {
            // All reflection-detected methods found in ASM method set -> proceed
            return selectedMethods;
          }
        }
      }
      catch (IOException ex) {
        log.debug("Failed to read class file via ASM for determining @Autowired method order", ex);
        // No worries, let's continue with the reflection metadata we started with...
      }
    }
    return methodElements;
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
  private Object resolveCachedArgument(@Nullable String beanName, @Nullable Object cachedArgument) {
    if (cachedArgument instanceof DependencyDescriptor descriptor) {
      Assert.state(this.beanFactory != null, "No BeanFactory available");
      return this.beanFactory.resolveDependency(descriptor, beanName, null, null);
    }
    else {
      return cachedArgument;
    }
  }

  /**
   * Base class representing injection information.
   */
  private abstract static class AutowiredElement extends InjectedElement {

    protected final boolean required;

    protected AutowiredElement(Member member, @Nullable PropertyDescriptor pd, boolean required) {
      super(member, pd);
      this.required = required;
    }
  }

  /**
   * Class representing injection information about an annotated field.
   */
  private class AutowiredFieldElement extends AutowiredElement {

    private volatile boolean cached;

    @Nullable
    private volatile Object cachedFieldValue;

    public AutowiredFieldElement(Field field, boolean required) {
      super(field, null, required);
    }

    @Override
    protected void inject(Object bean, @Nullable String beanName, @Nullable PropertyValues pvs) throws Throwable {
      Field field = (Field) this.member;
      Object value;
      if (this.cached) {
        try {
          value = resolveCachedArgument(beanName, this.cachedFieldValue);
        }
        catch (BeansException ex) {
          // Unexpected target bean mismatch for cached argument -> re-resolve
          this.cached = false;
          log.debug("Failed to resolve cached argument", ex);
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
      var autowiredBeanNames = new LinkedHashSet<String>(2);
      ConfigurableBeanFactory beanFactory = AutowiredAnnotationBeanPostProcessor.this.beanFactory;
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
          if (value != null || this.required) {
            Object cachedFieldValue = desc;
            registerDependentBeans(beanName, autowiredBeanNames);
            if (value != null && autowiredBeanNames.size() == 1) {
              String autowiredBeanName = autowiredBeanNames.iterator().next();
              if (beanFactory.containsBean(autowiredBeanName)
                      && beanFactory.isTypeMatch(autowiredBeanName, field.getType())) {
                cachedFieldValue = new ShortcutDependencyDescriptor(desc, autowiredBeanName);
              }
            }
            this.cachedFieldValue = cachedFieldValue;
            this.cached = true;
          }
          else {
            this.cachedFieldValue = null;
            // cached flag remains false
          }
        }
      }
      return value;
    }
  }

  /**
   * Class representing injection information about an annotated method.
   */
  private class AutowiredMethodElement extends AutowiredElement {

    private volatile boolean cached;

    @Nullable
    private volatile Object[] cachedMethodArguments;

    public AutowiredMethodElement(Method method, boolean required, @Nullable PropertyDescriptor pd) {
      super(method, pd, required);
    }

    @Override
    protected void inject(Object bean, @Nullable String beanName, @Nullable PropertyValues pvs) throws Throwable {
      if (!shouldInject(pvs)) {
        return;
      }
      Method method = (Method) this.member;
      Object[] arguments;
      if (this.cached) {
        try {
          arguments = resolveCachedArguments(beanName, this.cachedMethodArguments);
        }
        catch (BeansException ex) {
          // Unexpected target bean mismatch for cached argument -> re-resolve
          this.cached = false;
          log.debug("Failed to resolve cached argument", ex);
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
    private Object[] resolveCachedArguments(@Nullable String beanName, @Nullable Object[] cachedMethodArguments) {
      if (cachedMethodArguments == null) {
        return null;
      }
      Object[] arguments = new Object[cachedMethodArguments.length];
      for (int i = 0; i < arguments.length; i++) {
        arguments[i] = resolveCachedArgument(beanName, cachedMethodArguments[i]);
      }
      return arguments;
    }

    @Nullable
    private Object[] resolveMethodArguments(Method method, Object bean, @Nullable String beanName) {
      int argumentCount = method.getParameterCount();
      Object[] arguments = new Object[argumentCount];
      DependencyDescriptor[] descriptors = new DependencyDescriptor[argumentCount];
      var autowiredBeans = new LinkedHashSet<String>(argumentCount * 2);

      ConfigurableBeanFactory beanFactory = AutowiredAnnotationBeanPostProcessor.this.beanFactory;
      Assert.state(beanFactory != null, "No BeanFactory available");

      TypeConverter typeConverter = beanFactory.getTypeConverter();
      for (int i = 0; i < arguments.length; i++) {
        MethodParameter methodParam = new MethodParameter(method, i);
        DependencyDescriptor currDesc = new DependencyDescriptor(methodParam, this.required);
        currDesc.setContainingClass(bean.getClass());
        descriptors[i] = currDesc;
        try {
          Object arg = beanFactory.resolveDependency(currDesc, beanName, autowiredBeans, typeConverter);
          if (arg == null && !this.required && !methodParam.isOptional()) {
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
            DependencyDescriptor[] cachedMethodArguments = Arrays.copyOf(descriptors, argumentCount);
            registerDependentBeans(beanName, autowiredBeans);
            if (autowiredBeans.size() == argumentCount) {
              Iterator<String> it = autowiredBeans.iterator();
              Class<?>[] paramTypes = method.getParameterTypes();
              for (int i = 0; i < paramTypes.length; i++) {
                String autowiredBeanName = it.next();
                if (arguments[i] != null && beanFactory.containsBean(autowiredBeanName)
                        && beanFactory.isTypeMatch(autowiredBeanName, paramTypes[i])) {
                  cachedMethodArguments[i] = new ShortcutDependencyDescriptor(
                          descriptors[i], autowiredBeanName);
                }
              }
            }
            this.cachedMethodArguments = cachedMethodArguments;
            this.cached = true;
          }
          else {
            this.cachedMethodArguments = null;
            // cached flag remains false
          }
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

    public ShortcutDependencyDescriptor(DependencyDescriptor original, String shortcut) {
      super(original);
      this.shortcut = shortcut;
    }

    @Override
    public Object resolveShortcut(BeanFactory beanFactory) {
      return beanFactory.getBean(this.shortcut, getDependencyType());
    }
  }

  /**
   * {@link BeanRegistrationAotContribution} to autowire fields and methods.
   */
  private static class AotContribution implements BeanRegistrationAotContribution {

    private static final String REGISTERED_BEAN_PARAMETER = "registeredBean";

    private static final String INSTANCE_PARAMETER = "instance";

    private final Class<?> target;

    private final Collection<AutowiredElement> autowiredElements;

    @Nullable
    private final AutowireCandidateResolver candidateResolver;

    AotContribution(Class<?> target, Collection<AutowiredElement> autowiredElements,
            @Nullable AutowireCandidateResolver candidateResolver) {

      this.target = target;
      this.autowiredElements = autowiredElements;
      this.candidateResolver = candidateResolver;
    }

    @Override
    public void applyTo(GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode) {
      GeneratedClass generatedClass = generationContext.getGeneratedClasses()
              .addForFeatureComponent("Autowiring", this.target, type -> {
                type.addJavadoc("Autowiring for {@link $T}.", this.target);
                type.addModifiers(javax.lang.model.element.Modifier.PUBLIC);
              });
      GeneratedMethod generateMethod = generatedClass.getMethods().add("apply", method -> {
        method.addJavadoc("Apply the autowiring.");
        method.addModifiers(javax.lang.model.element.Modifier.PUBLIC,
                javax.lang.model.element.Modifier.STATIC);
        method.addParameter(RegisteredBean.class, REGISTERED_BEAN_PARAMETER);
        method.addParameter(this.target, INSTANCE_PARAMETER);
        method.returns(this.target);
        method.addCode(generateMethodCode(generatedClass.getName(),
                generationContext.getRuntimeHints()));
      });
      beanRegistrationCode.addInstancePostProcessor(generateMethod.toMethodReference());

      if (this.candidateResolver != null) {
        registerHints(generationContext.getRuntimeHints());
      }
    }

    private CodeBlock generateMethodCode(ClassName targetClassName, RuntimeHints hints) {
      CodeBlock.Builder code = CodeBlock.builder();
      for (AutowiredElement autowiredElement : this.autowiredElements) {
        code.addStatement(generateMethodStatementForElement(
                targetClassName, autowiredElement, hints));
      }
      code.addStatement("return $L", INSTANCE_PARAMETER);
      return code.build();
    }

    private CodeBlock generateMethodStatementForElement(ClassName targetClassName,
            AutowiredElement autowiredElement, RuntimeHints hints) {

      Member member = autowiredElement.getMember();
      boolean required = autowiredElement.required;
      if (member instanceof Field field) {
        return generateMethodStatementForField(targetClassName, field, required, hints);
      }
      if (member instanceof Method method) {
        return generateMethodStatementForMethod(targetClassName, method, required, hints);
      }
      throw new IllegalStateException(
              "Unsupported member type " + member.getClass().getName());
    }

    private CodeBlock generateMethodStatementForField(ClassName targetClassName,
            Field field, boolean required, RuntimeHints hints) {

      hints.reflection().registerField(field);
      CodeBlock resolver = CodeBlock.of("$T.$L($S)", AutowiredFieldValueResolver.class, (!required) ? "forField" : "forRequiredField", field.getName());
      AccessControl accessControl = AccessControl.forMember(field);
      if (!accessControl.isAccessibleFrom(targetClassName)) {
        return CodeBlock.of("$L.resolveAndSet($L, $L)", resolver,
                REGISTERED_BEAN_PARAMETER, INSTANCE_PARAMETER);
      }
      return CodeBlock.of("$L.$L = $L.resolve($L)", INSTANCE_PARAMETER,
              field.getName(), resolver, REGISTERED_BEAN_PARAMETER);
    }

    private CodeBlock generateMethodStatementForMethod(ClassName targetClassName,
            Method method, boolean required, RuntimeHints hints) {

      CodeBlock.Builder code = CodeBlock.builder();
      code.add("$T.$L", AutowiredMethodArgumentsResolver.class,
              (!required) ? "forMethod" : "forRequiredMethod");
      code.add("($S", method.getName());
      if (method.getParameterCount() > 0) {
        code.add(", $L", generateParameterTypesCode(method.getParameterTypes()));
      }
      code.add(")");
      AccessControl accessControl = AccessControl.forMember(method);
      if (!accessControl.isAccessibleFrom(targetClassName)) {
        hints.reflection().registerMethod(method, ExecutableMode.INVOKE);
        code.add(".resolveAndInvoke($L, $L)", REGISTERED_BEAN_PARAMETER, INSTANCE_PARAMETER);
      }
      else {
        hints.reflection().registerMethod(method, ExecutableMode.INTROSPECT);
        CodeBlock arguments = new AutowiredArgumentsCodeGenerator(this.target, method)
                .generateCode(method.getParameterTypes());
        CodeBlock injectionCode = CodeBlock.of("args -> $L.$L($L)", INSTANCE_PARAMETER, method.getName(), arguments);
        code.add(".resolve($L, $L)", REGISTERED_BEAN_PARAMETER, injectionCode);
      }
      return code.build();
    }

    private CodeBlock generateParameterTypesCode(Class<?>[] parameterTypes) {
      return CodeBlock.join(Arrays.stream(parameterTypes)
              .map(parameterType -> CodeBlock.of("$T.class", parameterType))
              .toList(), ", ");
    }

    private void registerHints(RuntimeHints runtimeHints) {
      this.autowiredElements.forEach(autowiredElement -> {
        boolean required = autowiredElement.required;
        Member member = autowiredElement.getMember();
        if (member instanceof Field field) {
          DependencyDescriptor dependencyDescriptor = new DependencyDescriptor(field, required);
          registerProxyIfNecessary(runtimeHints, dependencyDescriptor);
        }
        if (member instanceof Method method) {
          Class<?>[] parameterTypes = method.getParameterTypes();
          for (int i = 0; i < parameterTypes.length; i++) {
            MethodParameter methodParam = new MethodParameter(method, i);
            DependencyDescriptor dependencyDescriptor = new DependencyDescriptor(methodParam, required);
            registerProxyIfNecessary(runtimeHints, dependencyDescriptor);
          }
        }
      });
    }

    private void registerProxyIfNecessary(RuntimeHints runtimeHints, DependencyDescriptor dependencyDescriptor) {
      if (this.candidateResolver != null) {
        Class<?> proxyClass =
                this.candidateResolver.getLazyResolutionProxyClass(dependencyDescriptor, null);
        if (proxyClass != null) {
          ClassHintUtils.registerProxyIfNecessary(proxyClass, runtimeHints);
        }
      }
    }

  }

}

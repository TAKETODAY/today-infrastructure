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

import java.beans.PropertyDescriptor;
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.beans.BeanUtils;
import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.PropertyValues;
import cn.taketoday.beans.factory.BeanCreationException;
import cn.taketoday.beans.factory.BeanDefinitionPostProcessor;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.BeanPostProcessor;
import cn.taketoday.beans.factory.DependenciesBeanPostProcessor;
import cn.taketoday.beans.factory.InjectionPoint;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.beans.factory.SmartInstantiationAwareBeanPostProcessor;
import cn.taketoday.beans.factory.UnsatisfiedDependencyException;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.factory.annotation.InjectionMetadata;
import cn.taketoday.beans.factory.annotation.Value;
import cn.taketoday.core.BridgeMethodResolver;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.PriorityOrdered;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * {@link BeanPostProcessor BeanPostProcessor}
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
 * <p>A default {@code StandardDependenciesBeanPostProcessor} will be registered
 * by the "context:annotation-config" and "context:component-scan" XML tags.
 * Remove or turn off the default annotation configuration there if you intend
 * to specify a custom {@code StandardDependenciesBeanPostProcessor} bean definition.
 *
 * <p><b>NOTE:</b> Annotation injection will be performed <i>before</i> XML injection;
 * thus the latter configuration will override the former for properties wired through
 * both approaches.
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Stephane Nicoll
 * @author Sebastien Deleuze
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Autowired
 * @since 4.0 2021/12/26 14:51
 */
public class StandardDependenciesBeanPostProcessor implements DependenciesBeanPostProcessor,
        SmartInstantiationAwareBeanPostProcessor, BeanDefinitionPostProcessor, PriorityOrdered, BeanFactoryAware {

  protected final Logger log = LoggerFactory.getLogger(getClass());

  private int order = Ordered.LOWEST_PRECEDENCE - 2;

  @Nullable
  private ConfigurableBeanFactory beanFactory;

  private final Set<String> lookupMethodsChecked = Collections.newSetFromMap(new ConcurrentHashMap<>(256));

  private final ConcurrentHashMap<String, InjectionMetadata> injectionMetadataCache = new ConcurrentHashMap<>(256);
  private final ConcurrentHashMap<Class<?>, Constructor<?>[]> candidateConstructorsCache = new ConcurrentHashMap<>(256);

  private DependencyInjector dependencyInjector;

  private RequiredStatusRetriever requiredStatusRetriever = new RequiredStatusRetriever();

  public StandardDependenciesBeanPostProcessor() { }

  public StandardDependenciesBeanPostProcessor(ConfigurableBeanFactory beanFactory) {
    this();
    setBeanFactory(beanFactory);
  }

  /**
   * set a new required-status-retriever
   * <p>
   * if input is {@code null} use a default {@link RequiredStatusRetriever}
   * </p>
   *
   * @param requiredStatusRetriever required status retriever
   */
  public void setRequiredStatusRetriever(@Nullable RequiredStatusRetriever requiredStatusRetriever) {
    this.requiredStatusRetriever =
            requiredStatusRetriever != null ? requiredStatusRetriever : new RequiredStatusRetriever();
  }

  public RequiredStatusRetriever getRequiredStatusRetriever() {
    return requiredStatusRetriever;
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
    this.beanFactory = beanFactory.unwrap(ConfigurableBeanFactory.class);
    setDependencyInjector(beanFactory.getInjector());
  }

  /**
   * set a DependencyInjector to handle dependency
   *
   * @param dependencyInjector Can be {@code null} but cannot support parameter injection
   */
  public void setDependencyInjector(@Nullable DependencyInjector dependencyInjector) {
    this.dependencyInjector = dependencyInjector;
  }

  public DependencyInjector getDependencyInjector() {
    return dependencyInjector;
  }

  public DependencyResolvingStrategies getResolvingStrategies() {
    return dependencyInjector.getResolvingStrategies();
  }

  public DependencyInjector obtainDependencyInjector() {
    if (dependencyInjector == null) {
      setDependencyInjector(new DependencyInjector(beanFactory));
    }
    return dependencyInjector;
  }

  @Override
  public void postProcessBeanDefinition(BeanDefinition beanDefinition, Object bean, String beanName) {
    InjectionMetadata metadata = findAutowiringMetadata(beanName, bean.getClass(), null);
    metadata.checkConfigMembers(beanDefinition);
  }

  /**
   * post process after property-value {@link BeanDefinition#getPropertyValues()}
   */
  @Override
  public void processDependencies(@Nullable PropertyValues propertyValues, Object bean, String beanName) {
    InjectionMetadata metadata = findAutowiringMetadata(beanName, bean.getClass(), propertyValues);
    try {
      metadata.inject(bean, beanName, propertyValues);
    }
    catch (BeanCreationException ex) {
      throw ex;
    }
    catch (Throwable ex) {
      throw new BeanCreationException(
              beanName, "Injection of autowired dependencies failed", ex);
    }
  }

  @Override
  public void resetBeanDefinition(String beanName) {
    lookupMethodsChecked.remove(beanName);
    injectionMetadataCache.remove(beanName);
  }

  @Override
  @Nullable
  public Constructor<?>[] determineCandidateConstructors(Class<?> beanClass, final String beanName)
          throws BeanCreationException {

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
          ArrayList<Constructor<?>> candidates = new ArrayList<>(rawCandidates.length);
          Constructor<?> requiredConstructor = null;
          Constructor<?> defaultConstructor = null;
          for (Constructor<?> candidate : rawCandidates) {
            boolean canInject = dependencyInjector.canInject(candidate);
            if (!canInject) {
              Class<?> userClass = ClassUtils.getUserClass(beanClass);
              if (userClass != beanClass) {
                try {
                  Constructor<?> superCtor =
                          userClass.getDeclaredConstructor(candidate.getParameterTypes());
                  canInject = dependencyInjector.canInject(superCtor);
                  if (canInject) {
                    candidate = superCtor;
                  }
                }
                catch (NoSuchMethodException ex) {
                  // Simply proceed, no equivalent superclass constructor found...
                }
              }
            }

            if (canInject) {
              if (requiredConstructor != null) {
                throw new BeanCreationException(beanName,
                        "Invalid autowire-marked constructor: " + candidate +
                                ". Found constructor with 'required' Autowired annotation already: " + requiredConstructor);
              }
              boolean required = determineRequiredStatus(candidate);
              if (required) {
                if (!candidates.isEmpty()) {
                  throw new BeanCreationException(beanName,
                          "Invalid autowire-marked constructors: " + candidates +
                                  ". Found constructor with 'required' Autowired annotation: " + candidate);
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
                log.info("Inconsistent constructor declaration on bean with name '{}': " +
                        "single autowire-marked constructor flagged as optional - " +
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
          candidateConstructorsCache.put(beanClass, candidateConstructors);
        }
      }
    }
    return candidateConstructors.length > 0 ? candidateConstructors : null;
  }

  /**
   * 'Native' processing method for direct calls with an arbitrary target instance,
   * resolving all of its fields and methods which are annotated with one of the
   * configured 'autowired' annotation types.
   *
   * @param bean the target instance to process
   * @throws BeanCreationException if autowiring failed
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

  private InjectionMetadata findAutowiringMetadata(
          String beanName, Class<?> clazz, @Nullable PropertyValues pvs) {
    // Fall back to class name as cache key, for backwards compatibility with custom callers.
    String cacheKey = StringUtils.isNotEmpty(beanName) ? beanName : clazz.getName();
    // Quick check on the concurrent map first, with minimal locking.
    InjectionMetadata metadata = injectionMetadataCache.get(cacheKey);
    if (InjectionMetadata.needsRefresh(metadata, clazz)) {
      synchronized(injectionMetadataCache) {
        metadata = injectionMetadataCache.get(cacheKey);
        if (InjectionMetadata.needsRefresh(metadata, clazz)) {
          if (metadata != null) {
            metadata.clear(pvs);
          }
          metadata = buildAutowiringMetadata(clazz);
          injectionMetadataCache.put(cacheKey, metadata);
        }
      }
    }
    return metadata;
  }

  private InjectionMetadata buildAutowiringMetadata(Class<?> clazz) {
    if (shouldSkipBuildAutowiringMetadata(clazz)) {
      return InjectionMetadata.EMPTY;
    }
    ArrayList<InjectionMetadata.InjectedElement> elements = new ArrayList<>();
    Class<?> targetClass = clazz;

    do {
      DependencyInjector dependencyInjector = getDependencyInjector();
      ArrayList<InjectionMetadata.InjectedElement> currElements = new ArrayList<>();

      ReflectionUtils.doWithLocalFields(targetClass, field -> {
        if (dependencyInjector.canInject(field)) {
          if (Modifier.isStatic(field.getModifiers())) {
            if (log.isInfoEnabled()) {
              log.info("Autowired annotation is not supported on static fields: {}", field);
            }
            return;
          }
          boolean required = determineRequiredStatus(field);
          currElements.add(new AutowiredFieldElement(field, required));
        }
      });

      ReflectionUtils.doWithLocalMethods(targetClass, method -> {
        Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(method);
        if (!BridgeMethodResolver.isVisibilityBridgeMethodPair(method, bridgedMethod)) {
          return;
        }
        if (dependencyInjector.canInject(method)
                && method.equals(ReflectionUtils.getMostSpecificMethod(method, clazz))) {
          if (Modifier.isStatic(method.getModifiers())) {
            log.info("Autowired annotation is not supported on static methods: {}", method);
            return;
          }
          boolean required = determineRequiredStatus(method);
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

  private boolean shouldSkipBuildAutowiringMetadata(Class<?> clazz) {
    return clazz.getName().startsWith("java.");
  }

  /**
   * Determine if the annotated field or method requires its dependency.
   * <p>A 'required' dependency means that autowiring should fail when no beans
   * are found. Otherwise, the autowiring process will simply bypass the field
   * or method when no beans are found.
   *
   * @param source the AccessibleObject
   * @return whether the annotation indicates that a dependency is required
   */
  protected boolean determineRequiredStatus(AccessibleObject source) {
    return requiredStatusRetriever.retrieve(source);
  }

  /**
   * Register the specified bean as dependent on the autowired beans.
   */
  private void registerDependentBeans(@Nullable String beanName, Set<String> autowiredBeanNames) {
    if (beanName != null) {
      for (String autowiredBeanName : autowiredBeanNames) {
        if (beanFactory != null && beanFactory.containsBean(autowiredBeanName)) {
          beanFactory.registerDependentBean(autowiredBeanName, beanName);
        }
        if (log.isTraceEnabled()) {
          log.trace("Autowiring by type from bean name '{}' to bean named '{}'",
                  beanName, autowiredBeanName);
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
      Assert.state(beanFactory != null, "No BeanFactory available");
      return beanFactory.resolveDependency(descriptor, beanName, null, null);
    }
    else {
      return cachedArgument;
    }
  }

  /**
   * Class representing injection information about an annotated field.
   */
  private class AutowiredFieldElement extends InjectionMetadata.InjectedElement {

    private final boolean required;

    private volatile boolean cached;

    @Nullable
    private volatile Object cachedFieldValue;

    public AutowiredFieldElement(Field field, boolean required) {
      super(field, null);
      this.required = required;
    }

    @Override
    protected void inject(
            Object bean, @Nullable String beanName, @Nullable PropertyValues pvs) throws Throwable {
      Field field = (Field) member;
      Object value;
      if (cached) {
        try {
          value = resolvedCachedArgument(beanName, cachedFieldValue);
        }
        catch (NoSuchBeanDefinitionException ex) {
          // Unexpected removal of target bean for cached argument -> re-resolve
          value = resolveFieldValue(field, bean, beanName);
        }
      }
      else {
        value = resolveFieldValue(field, bean, beanName);
      }
      if (value != InjectionPoint.DO_NOT_SET) {
        ReflectionUtils.makeAccessible(field);
        field.set(bean, value);
      }
    }

    @Nullable
    private Object resolveFieldValue(Field field, Object bean, @Nullable String beanName) {
      DependencyDescriptor desc = new DependencyDescriptor(field, required);
      desc.setContainingClass(bean.getClass());
      Assert.state(beanFactory != null, "No BeanFactory available");
      DependencyResolvingContext context = new DependencyResolvingContext(null, beanFactory, beanName);
      Object value = obtainDependencyInjector().resolve(desc, context);

      synchronized(this) {
        if (!cached) {
          Object cachedFieldValue = null;
          // not required don't modify its original value
          if (value != null || required) {
            cachedFieldValue = desc;
            Set<String> autowiredBeanNames = context.getDependentBeans();
            if (CollectionUtils.isNotEmpty(autowiredBeanNames)) {
              registerDependentBeans(beanName, autowiredBeanNames);
              if (autowiredBeanNames.size() == 1) {
                String autowiredBeanName = autowiredBeanNames.iterator().next();
                if (beanFactory.containsBean(autowiredBeanName)
                        && beanFactory.isTypeMatch(autowiredBeanName, field.getType())) {
                  cachedFieldValue = new ShortcutDependencyDescriptor(
                          desc, autowiredBeanName, field.getType());
                }
                else {
                  cachedFieldValue = value;
                }
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
  private class AutowiredMethodElement extends InjectionMetadata.InjectedElement {

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
      Method method = (Method) member;
      Object[] arguments;
      if (cached) {
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

    private Object[] resolveMethodArguments(Method method, Object bean, @Nullable String beanName) {
      int argumentCount = method.getParameterCount();
      Object[] arguments = new Object[argumentCount];
      DependencyDescriptor[] descriptors = new DependencyDescriptor[argumentCount];
      Assert.state(beanFactory != null, "No BeanFactory available");

      DependencyResolvingContext context = new DependencyResolvingContext(method, beanFactory, beanName);
      for (int i = 0; i < arguments.length; i++) {
        MethodParameter methodParam = new MethodParameter(method, i);
        DependencyDescriptor currDesc = new DependencyDescriptor(methodParam, this.required);
        currDesc.setContainingClass(bean.getClass());
        descriptors[i] = currDesc;

        try {
          Object arg = obtainDependencyInjector().resolveValue(currDesc, context);
          arguments[i] = arg;
        }
        catch (BeansException ex) {
          throw new UnsatisfiedDependencyException(null, beanName, new InjectionPoint(methodParam), ex);
        }
      }
      synchronized(this) {
        if (!this.cached) {
          Object[] cachedMethodArguments = Arrays.copyOf(descriptors, arguments.length);
          Set<String> autowiredBeans = context.getDependentBeans();
          if (CollectionUtils.isNotEmpty(autowiredBeans)) {
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
                else {
                  cachedMethodArguments[i] = arguments[i];
                }
              }
            }
          }
          this.cachedMethodArguments = cachedMethodArguments;
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

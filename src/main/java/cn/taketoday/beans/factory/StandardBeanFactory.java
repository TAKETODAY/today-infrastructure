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

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;

import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.core.annotation.MergedAnnotations.SearchStrategy;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.core.type.MethodMetadata;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.Prototype;
import cn.taketoday.logging.LogMessage;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * Standard {@link BeanFactory} implementation
 *
 * @author TODAY 2019-03-23 15:00
 */
public class StandardBeanFactory
        extends AbstractAutowireCapableBeanFactory implements ConfigurableBeanFactory, BeanDefinitionRegistry {

  private static final Logger log = LoggerFactory.getLogger(StandardBeanFactory.class);

  /**
   * @since 2.1.7 Preventing repeated initialization of beans(Prevent duplicate
   * initialization) , Prevent Cycle Dependency
   */
  private final HashSet<String> currentInitializingBeanName = new HashSet<>();

  /** Whether to allow re-registration of a different definition with the same name. */
  private boolean allowBeanDefinitionOverriding = true;

  /** Map of bean definition objects, keyed by bean name */
  private final ConcurrentHashMap<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(64);

  /** List of bean definition names, in registration order. */
  private final LinkedHashSet<String> beanDefinitionNames = new LinkedHashSet<>(256);

  /** List of names of manually registered singletons, in registration order. */
  private final LinkedHashSet<String> manualSingletonNames = new LinkedHashSet<>(16);

  //---------------------------------------------------------------------
  // Implementation of DefaultSingletonBeanRegistry
  //---------------------------------------------------------------------

  @Override
  public void registerSingleton(String name, Object singleton) {
    super.registerSingleton(name, singleton);
    manualSingletonNames.add(name);
  }

  @Override
  public void removeSingleton(String name) {
    super.removeSingleton(name);
    manualSingletonNames.remove(name);
  }

  /**
   * Preventing Cycle Dependency expected {@link Prototype} beans
   */
  @Override
  public void populateBean(Object bean, BeanDefinition definition) {
    if (definition.isPrototype()) {
      super.populateBean(bean, definition);
    }
    else {
      String name = definition.getName();
      if (currentInitializingBeanName.add(name)) {
        super.populateBean(bean, definition);
        currentInitializingBeanName.remove(name);
      }
    }
  }

  //---------------------------------------------------------------------
  // Implementation of BeanDefinitionRegistry interface @since 4.0
  //---------------------------------------------------------------------

  @Override
  public Map<String, BeanDefinition> getBeanDefinitions() {
    return beanDefinitionMap;
  }

  @Override
  public void registerBeanDefinition(BeanDefinition def) {
    registerBeanDefinition(def.getName(), def);
  }

  @Override
  public void registerBeanDefinition(String beanName, BeanDefinition def) {
    if (def.getName() == null) {
      def.setName(beanName);
    }
    def = transformBeanDefinition(beanName, def);
    if (def == null) {
      return;
    }
    try {
      def.validate();
    }
    catch (BeanDefinitionValidationException ex) {
      throw new BeanDefinitionStoreException("Validation of bean definition '" + def + "' failed", ex);
    }

    BeanDefinition existBeanDef = getBeanDefinition(beanName);
    if (existBeanDef != null && !def.hasAttribute(MissingBean.MissingBeanMetadata)) {
      if (!isAllowBeanDefinitionOverriding()) {
        throw new BeanDefinitionOverrideException(beanName, def, existBeanDef);
      }
      else if (existBeanDef.getRole() < def.getRole()) {
        // e.g. was ROLE_APPLICATION, now overriding with ROLE_SUPPORT or ROLE_INFRASTRUCTURE
        if (log.isInfoEnabled()) {
          log.info("Overriding user-defined bean definition " +
                           "for bean '{}' with a framework-generated bean " +
                           "definition: replacing [{}] with [{}]", beanName, existBeanDef, def);
        }
      }
    }

    beanDefinitionMap.put(beanName, def);
    beanDefinitionNames.add(beanName);
    postProcessRegisterBeanDefinition(def);
  }

  /**
   * @since 3.0
   */
  protected BeanDefinition transformBeanDefinition(String name, BeanDefinition def) {
    BeanDefinition missedDef = null;
    if (containsBeanDefinition(name)) {
      missedDef = getBeanDefinition(name);
    }

    if (missedDef != null && missedDef.hasAttribute(MissingBean.MissingBeanMetadata)) {
      // Have a corresponding missed bean
      // copy all state
      def.copyFrom(missedDef);
      def.setName(name); // fix bean name update error
    }
    // nothing
    return def;
  }

  /**
   * Process after register {@link BeanDefinition}
   *
   * @param targetDef Target {@link BeanDefinition}
   */
  protected void postProcessRegisterBeanDefinition(BeanDefinition targetDef) {

  }

  @Override
  public void removeBeanDefinition(String beanName) {
    beanDefinitionMap.remove(beanName);
    beanDefinitionNames.remove(beanName);
  }

  @Override
  public BeanDefinition getBeanDefinition(String beanName) {
    return beanDefinitionMap.get(beanName);
  }

  @Override
  public BeanDefinition getBeanDefinition(Class<?> requiredType) {
    Set<String> candidateNames = getBeanNamesForType(requiredType, true, false);
    int size = candidateNames.size();
    if (size == 1) {
      return getBeanDefinition(candidateNames.iterator().next());
    }
    else if (size > 1) {
      String primaryCandidate = getPrimaryCandidate(candidateNames, requiredType);
      if (primaryCandidate != null) {
        return getBeanDefinition(primaryCandidate);
      }
      // fall
      throw new NoUniqueBeanException(requiredType, candidateNames);
    }
    return null;
  }

  @Override
  public boolean containsBeanDefinition(Class<?> type) {
    return !getBeanNamesForType(type, true, false).isEmpty();
  }

  @Override
  public boolean containsBeanDefinition(Class<?> type, boolean equals) {
    if (equals) {
      for (String name : getBeanNamesForType(type, true, false)) {
        Class<?> type1 = getType(name);
        if (type1 == type) {
          return true;
        }
      }
      return false;
    }
    return getBeanDefinition(type) != null;
  }

  @Override
  public boolean containsBeanDefinition(String beanName, Class<?> type) {
    return containsBeanDefinition(beanName) && containsBeanDefinition(type);
  }

  @Override
  public boolean containsBeanDefinition(String beanName) {
    return beanDefinitionMap.containsKey(beanName);
  }

  @Override
  public String[] getBeanDefinitionNames() {
    return StringUtils.toStringArray(beanDefinitionNames);
  }

  @Override
  public Iterator<String> getBeanNamesIterator() {
    return beanDefinitionMap.keySet().iterator();
  }

  @Override
  public int getBeanDefinitionCount() {
    return beanDefinitionMap.size();
  }

  @Override
  public boolean isBeanNameInUse(String beanName) {
    return beanDefinitionMap.containsKey(beanName);
  }

  @Override
  public Iterator<BeanDefinition> iterator() {
    return beanDefinitionMap.values().iterator();
  }

  /**
   * Set whether it should be allowed to override bean definitions by registering
   * a different definition with the same name, automatically replacing the former.
   * If not, an exception will be thrown. This also applies to overriding aliases.
   * <p>Default is "true".
   *
   * @see #registerBeanDefinition
   * @since 4.0
   */
  public void setAllowBeanDefinitionOverriding(boolean allowBeanDefinitionOverriding) {
    this.allowBeanDefinitionOverriding = allowBeanDefinitionOverriding;
  }

  /**
   * Return whether it should be allowed to override bean definitions by registering
   * a different definition with the same name, automatically replacing the former.
   *
   * @since 4.0
   */
  @Override
  public boolean isAllowBeanDefinitionOverriding() {
    return this.allowBeanDefinitionOverriding;
  }

  //---------------------------------------------------------------------
  // Implementation of BeanFactory interface
  //---------------------------------------------------------------------

  @Override
  public <T> T getBean(Class<T> requiredType) throws BeansException {
    Assert.notNull(requiredType, "Required type must not be null");
    return resolveBean(ResolvableType.fromRawClass(requiredType), false);
  }

  @Nullable
  private <T> T resolveBean(ResolvableType requiredType, boolean nonUniqueAsNull) {
    return resolveBean(requiredType, true, true, nonUniqueAsNull);
  }

  @Nullable
  @SuppressWarnings("unchecked")
  private <T> T resolveBean(
          ResolvableType requiredType,
          boolean includeNonSingletons, boolean allowEagerInit, boolean nonUniqueAsNull) {
    NamedBeanHolder<T> namedBean = resolveNamedBean(
            requiredType, includeNonSingletons, allowEagerInit, nonUniqueAsNull);
    if (namedBean != null) {
      return namedBean.getBeanInstance();
    }
    Object dependency = resolveFromObjectFactories(requiredType.resolve());
    if (dependency != null) {
      return (T) dependency;
    }

    BeanFactory parent = getParentBeanFactory();
    if (parent instanceof StandardBeanFactory) {
      return ((StandardBeanFactory) parent).resolveBean(requiredType, nonUniqueAsNull);
    }
    else if (parent != null) {
      ObjectSupplier<T> parentProvider = parent.getObjectSupplier(requiredType);
      return parentProvider.get();
    }
    return null;
  }

  @Nullable
  @SuppressWarnings("unchecked")
  private <T> NamedBeanHolder<T> resolveNamedBean(
          ResolvableType requiredType, boolean includeNonSingletons,
          boolean allowEagerInit, boolean nonUniqueAsNull) throws BeansException {
    Assert.notNull(requiredType, "Required type must not be null");
    Set<String> candidateNames = getBeanNamesForType(requiredType, includeNonSingletons, allowEagerInit);

    int size = candidateNames.size();
    if (size == 1) {
      return resolveNamedBean(candidateNames.iterator().next(), requiredType);
    }
    else if (size > 1) {
      String primaryCandidate = determinePrimaryCandidate(candidateNames, requiredType.toClass());
      if (primaryCandidate == null) {
        Map<String, Object> candidates = CollectionUtils.newLinkedHashMap(size);
        for (String beanName : candidateNames) {
          if (containsSingleton(beanName)) {
            Object beanInstance = getBean(beanName);
            candidates.put(beanName, beanInstance);
          }
          else {
            candidates.put(beanName, getType(beanName));
          }
        }
        primaryCandidate = determineHighestPriorityCandidate(candidates, requiredType.toClass());
        if (primaryCandidate != null) {
          Object beanInstance = candidates.get(primaryCandidate);
          if (beanInstance == null) {
            return null;
          }
          if (beanInstance instanceof Class) {
            return resolveNamedBean(primaryCandidate, requiredType);
          }
          return new NamedBeanHolder<>(primaryCandidate, (T) beanInstance);
        }
      }
      if (primaryCandidate != null) {
        return resolveNamedBean(primaryCandidate, requiredType);
      }

      // fall
      if (!nonUniqueAsNull) {
        throw new NoUniqueBeanException(requiredType, candidateNames);
      }
    }

    return null;
  }

  @Override
  public String getPrimaryCandidate(Set<String> candidateNames, Class<?> requiredType) {
    String primaryCandidate = determinePrimaryCandidate(candidateNames, requiredType);
    if (primaryCandidate == null) {
      Map<String, Object> candidates = CollectionUtils.newLinkedHashMap(candidateNames.size());
      for (String beanName : candidateNames) {
        if (containsSingleton(beanName)) {
          Object beanInstance = getBean(beanName);
          candidates.put(beanName, beanInstance);
        }
        else {
          candidates.put(beanName, getType(beanName));
        }
      }
      primaryCandidate = determineHighestPriorityCandidate(candidates, requiredType);
    }
    return primaryCandidate;
  }

  @Nullable
  private <T> NamedBeanHolder<T> resolveNamedBean(
          String beanName, ResolvableType requiredType) throws BeansException {
    Object bean = doGetBean(beanName, null, null);
    if (bean == null) {
      return null;
    }
    return new NamedBeanHolder<>(beanName, adaptBeanInstance(beanName, bean, requiredType.toClass()));
  }

  /**
   * Determine the primary candidate in the given set of beans.
   *
   * @param candidates a set of candidate names
   * @param requiredType the target dependency type to match against
   * @return the name of the primary candidate, or {@code null} if none found
   * @see #isPrimary(String)
   */
  @Nullable
  protected String determinePrimaryCandidate(Set<String> candidates, Class<?> requiredType) {
    String primaryBeanName = null;
    for (String candidateBeanName : candidates) {
      if (isPrimary(candidateBeanName)) {
        if (primaryBeanName != null) {
          boolean candidateLocal = containsBeanDefinition(candidateBeanName);
          boolean primaryLocal = containsBeanDefinition(primaryBeanName);
          if (candidateLocal && primaryLocal) {
            throw new NoUniqueBeanException(
                    requiredType, candidates.size(),
                    "more than one 'primary' bean found among candidates: " + candidates);
          }
          else if (candidateLocal) {
            primaryBeanName = candidateBeanName;
          }
        }
        else {
          primaryBeanName = candidateBeanName;
        }
      }
    }
    return primaryBeanName;
  }

  /**
   * Determine the candidate with the highest priority in the given set of beans.
   * <p>Based on {@code @jakarta.annotation.Priority}. As defined by the related
   * {@link Ordered} interface, the lowest value has the highest priority.
   *
   * @param candidates a set of candidate names
   * @param requiredType the target dependency type to match against
   * @return the name of the candidate with the highest priority,
   * or {@code null} if none found
   * @see #getPriority(Object)
   */
  @Nullable
  protected String determineHighestPriorityCandidate(Map<String, Object> candidates, Class<?> requiredType) {
    String highestPriorityBeanName = null;
    Integer highestPriority = null;
    for (Map.Entry<String, Object> entry : candidates.entrySet()) {
      String candidateBeanName = entry.getKey();
      Object beanInstance = entry.getValue();
      if (beanInstance != null) {
        Integer candidatePriority = getPriority(beanInstance);
        if (candidatePriority != null) {
          if (highestPriorityBeanName != null) {
            if (candidatePriority.equals(highestPriority)) {
              throw new NoUniqueBeanException(
                      requiredType, candidates.size(),
                      "Multiple beans found with the same priority ('" + highestPriority +
                              "') among candidates: " + candidates.keySet());
            }
            else if (candidatePriority < highestPriority) {
              highestPriorityBeanName = candidateBeanName;
              highestPriority = candidatePriority;
            }
          }
          else {
            highestPriorityBeanName = candidateBeanName;
            highestPriority = candidatePriority;
          }
        }
      }
    }
    return highestPriorityBeanName;
  }

  /**
   * Return whether the bean definition for the given bean name has been
   * marked as a primary bean.
   *
   * @param beanName the name of the bean
   * @return whether the given bean qualifies as primary
   */
  protected boolean isPrimary(String beanName) {
    if (containsBeanDefinition(beanName)) {
      return beanDefinitionMap.get(beanName).isPrimary();
    }
    BeanFactory parent = getParentBeanFactory();
    return (parent instanceof StandardBeanFactory &&
            ((StandardBeanFactory) parent).isPrimary(beanName));
  }

  /**
   * Return the priority assigned for the given bean instance by
   * the {@code jakarta.annotation.Priority} annotation.
   *
   * @param beanInstance the bean instance to check (can be {@code null})
   * @return the priority assigned to that bean or {@code null} if none is set
   */
  @Nullable
  protected Integer getPriority(Object beanInstance) {
    return AnnotationAwareOrderComparator.INSTANCE.getPriority(beanInstance);
  }

  @Override
  public <T> NamedBeanHolder<T> resolveNamedBean(Class<T> requiredType) throws BeansException {
    Assert.notNull(requiredType, "Required type must not be null");
    NamedBeanHolder<T> namedBean = resolveNamedBean(ResolvableType.fromClass(requiredType), true, true, false);
    if (namedBean != null) {
      return namedBean;
    }
    BeanFactory parent = getParentBeanFactory();
    if (parent instanceof AutowireCapableBeanFactory) {
      return ((AutowireCapableBeanFactory) parent).resolveNamedBean(requiredType);
    }
    throw new NoSuchBeanDefinitionException(requiredType);
  }

  //---------------------------------------------------------------------
  // Listing Get operations for type-lookup
  //---------------------------------------------------------------------

  @Override
  public <T> Supplier<T> getObjectSupplier(String beanName) {
    return getObjectSupplier(obtainBeanDefinition(beanName));
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> ObjectSupplier<T> getObjectSupplier(BeanDefinition def) {
    Assert.notNull(def, "BeanDefinition must not be null");

    if (def.isSingleton()) {
      final class SingletonObjectSupplier implements ObjectSupplier<T> {
        volatile T targetSingleton;

        @Override
        public T getIfAvailable() throws BeansException {
          T ret = targetSingleton;
          if (ret == null) {
            synchronized(this) {
              ret = targetSingleton;
              if (ret == null) {
                ret = targetSingleton = (T) getBean(def);
              }
            }
          }
          return ret;
        }

        @Override
        public Iterator<T> iterator() {
          return CollectionUtils.singletonIterator(get());
        }

        @Override
        public T get() { return getIfAvailable(); }

        @Override
        public Stream<T> orderedStream() { return stream(); }

        @Override
        public Stream<T> stream() { return Stream.of(targetSingleton); }
      }
      return new SingletonObjectSupplier();
    }

    return new DefaultObjectSupplier<>(def.getBeanClass(), this) {

      @Override
      public T getIfAvailable() throws BeansException {
        return (T) getBean(def);
      }
    };
  }

  @Override
  public <T> ObjectSupplier<T> getObjectSupplier(Class<T> requiredType) {
    Assert.notNull(requiredType, "requiredType must not be null");
    return new DefaultObjectSupplier<>(requiredType, this);
  }

  @Override
  public <T> ObjectSupplier<T> getObjectSupplier(ResolvableType requiredType) {
    return getObjectSupplier(requiredType, true, true);
  }

  @Override
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public <T> ObjectSupplier<T> getObjectSupplier(
          ResolvableType requiredType, boolean includeNonSingletons, boolean allowEagerInit) {
    if (requiredType.isArray()) {
      // Bean[] beans
      ResolvableType type = requiredType.getComponentType();
      if (type == ResolvableType.NONE) {
        throw new IllegalArgumentException("cannot determine bean type");
      }
      // not supports iteration, stream
      return new AbstractResolvableTypeObjectSupplier(type, includeNonSingletons, allowEagerInit) {

        @Override
        Object getIfAvailable(ResolvableType requiredType, boolean nonRegistered, boolean allowEagerInit) {
          Map<String, Object> beansOfType = getBeansOfType(requiredType, nonRegistered, allowEagerInit);
          if (beansOfType.isEmpty()) {
            return Array.newInstance(requiredType.resolve(), 0);
          }
          Object array = Array.newInstance(requiredType.resolve(), beansOfType.size());
          return beansOfType.values().toArray((Object[]) array);
        }
      };
    }

    if (requiredType.isMap()) {
      ResolvableType type = requiredType.asMap().getGeneric(1);
      if (type == ResolvableType.NONE) {
        throw new IllegalArgumentException("cannot determine bean type");
      }
      // not supports iteration, stream
      return new AbstractResolvableTypeObjectSupplier(type, includeNonSingletons, allowEagerInit) {

        @Override
        Object getIfAvailable(ResolvableType requiredType, boolean nonRegistered, boolean allowEagerInit) {
          return getBeansOfType(requiredType, nonRegistered, allowEagerInit);
        }
      };
    }

    if (requiredType.isCollection()) {
      ResolvableType type = requiredType.asCollection().getGeneric(0);
      if (type == ResolvableType.NONE) {
        throw new IllegalArgumentException("cannot determine bean type");
      }
      // not supports iteration, stream
      return new AbstractResolvableTypeObjectSupplier(type, includeNonSingletons, allowEagerInit) {

        @Override
        Object getIfAvailable(ResolvableType requiredType, boolean nonRegistered, boolean allowEagerInit) {
          Map<String, Object> beansOfType = getBeansOfType(requiredType, nonRegistered, allowEagerInit);
          Collection<Object> ret = CollectionUtils.createCollection(requiredType.resolve());
          if (beansOfType.isEmpty()) {
            return ret;
          }

          ret.addAll(beansOfType.values());
          return ret;
        }
      };
    }

    // find like Bean<String>
    return new ResolvableTypeObjectSupplier<>(requiredType, includeNonSingletons, allowEagerInit);
  }

  final class ResolvableTypeObjectSupplier<T> extends AbstractResolvableTypeObjectSupplier<T> {

    ResolvableTypeObjectSupplier(ResolvableType requiredType, boolean includeNoneRegistered, boolean includeNonSingletons) {
      super(requiredType, includeNoneRegistered, includeNonSingletons);
    }

    @Override
    protected T getIfAvailable(
            ResolvableType requiredType, boolean includeNonSingletons, boolean allowEagerInit) {
      return resolveBean(requiredType, includeNonSingletons, allowEagerInit, true);
    }

    private Map<String, T> getBeansOfType0() {
      return getBeansOfType(requiredType, includeNonSingletons, allowEagerInit);
    }

    @Override
    public Stream<T> stream() {
      Map<String, T> beansOfType = getBeansOfType0();
      return beansOfType.values().stream();
    }

    @Override
    public Stream<T> orderedStream() {
      Map<String, T> beansOfType = getBeansOfType0();
      ArrayList<T> beans = new ArrayList<>(beansOfType.values());
      AnnotationAwareOrderComparator.sort(beans);
      return beans.stream();
    }

    @Override
    public Iterator<T> iterator() {
      Map<String, T> beansOfType = getBeansOfType0();
      return beansOfType.values().iterator();
    }

  }

  @Override
  public <T> Map<String, T> getBeansOfType(
          Class<T> requiredType, boolean includeNonSingletons, boolean allowEagerInit) {
    return getBeansOfType(ResolvableType.fromRawClass(requiredType), includeNonSingletons, allowEagerInit);
  }

  /**
   * Return bean matching the given type (including subclasses), judging from bean definitions
   *
   * @param def the BeanDefinition to check
   * @param requiredType the class or interface to match, or {@code null} for all bean names
   * @param includeNonSingletons whether to include prototype or scoped beans too
   * or just singletons (also applies to FactoryBeans)
   * @return the bean matching the given object type (including subclasses)
   */
  protected boolean isEligibleBean(BeanDefinition def, Class<?> requiredType, boolean includeNonSingletons) {
    if (!(includeNonSingletons || def.isSingleton())) {
      return false;
    }

    if (requiredType != null) {
      Class<?> type = getType(def.getName());
      if (type != null) {
        return requiredType.isAssignableFrom(type);
      }
      return false;
    }
    return true;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> Map<String, T> getBeansOfType(
          @Nullable ResolvableType requiredType, boolean includeNonSingletons, boolean allowEagerInit) {
    Set<String> beanNames = getBeanNamesForType(requiredType, includeNonSingletons, allowEagerInit);
    Map<String, T> beans = CollectionUtils.newLinkedHashMap(beanNames.size());
    for (String beanName : beanNames) {
      Object beanInstance = getBean(beanName);
      if (beanInstance != null) {
        beans.put(beanName, (T) beanInstance);
      }
    }
    return beans;
  }

  // getBeanNamesOfType

  @Override
  public Set<String> getBeanNamesForType(Class<?> requiredType, boolean includeNonSingletons) {
    return getBeanNamesForType(requiredType, true, includeNonSingletons);
  }

  @Override
  public Set<String> getBeanNamesForType(
          Class<?> requiredType, boolean includeNonSingletons, boolean allowEagerInit) {
    return getBeanNamesForType(ResolvableType.fromRawClass(requiredType), includeNonSingletons, allowEagerInit);
  }

  @Override
  public Set<String> getBeanNamesForType(
          ResolvableType requiredType, boolean includeNonSingletons, boolean allowEagerInit) {
    LinkedHashSet<String> beanNames = new LinkedHashSet<>();

    for (String beanName : beanDefinitionNames) {
      BeanDefinition definition = beanDefinitionMap.get(beanName);
      boolean matchFound = false;
      boolean allowFactoryBeanInit = allowEagerInit || containsSingleton(beanName);
      if (includeNonSingletons || definition.isSingleton()) {
        matchFound = isTypeMatch(beanName, requiredType, allowFactoryBeanInit);
      }
      if (!matchFound && isFactoryBean(definition)) {
        // In case of FactoryBean, try to match FactoryBean instance itself next.
        beanName = FACTORY_BEAN_PREFIX + beanName;
        matchFound = isTypeMatch(beanName, requiredType, allowFactoryBeanInit);
      }
      if (matchFound) {
        beanNames.add(beanName);
      }
    }

    // Check manually registered singletons too.
    for (String beanName : this.manualSingletonNames) {
      if (beanNames.contains(beanName)) {
        continue;
      }
      try {
        // In case of FactoryBean, match object created by FactoryBean.
        if (isFactoryBean(beanName)) {
          if ((includeNonSingletons || isSingleton(beanName)) && isTypeMatch(beanName, requiredType)) {
            beanNames.add(beanName);
            // Match found for this bean: do not match FactoryBean itself anymore.
            continue;
          }
          // In case of FactoryBean, try to match FactoryBean itself next.
          beanName = FACTORY_BEAN_PREFIX + beanName;
        }
        // Match raw bean instance (might be raw FactoryBean).
        if (isTypeMatch(beanName, requiredType)) {
          beanNames.add(beanName);
        }
      }
      catch (NoSuchBeanDefinitionException ex) {
        // Shouldn't happen - probably a result of circular reference resolution...
        log.trace(LogMessage.format(
                "Failed to check manually registered singleton with name '%s'", beanName), ex);
      }
    }
    return beanNames;
  }

  @Override
  public Map<String, Object> getBeansOfAnnotation(
          Class<? extends Annotation> annotationType, boolean includeNonSingletons) {
    Assert.notNull(annotationType, "annotationType must not be null");

    Set<String> beanNames = getBeanNamesForAnnotation(annotationType);
    Map<String, Object> result = CollectionUtils.newLinkedHashMap(beanNames.size());
    for (String beanName : beanNames) {
      Object beanInstance = getBean(beanName);
      result.put(beanName, beanInstance);
    }
    return result;
  }

  @Override
  public Set<String> getBeanNamesForAnnotation(Class<? extends Annotation> annotationType) {
    Assert.notNull(annotationType, "annotationType must not be null");
    LinkedHashSet<String> names = new LinkedHashSet<>();

    for (String beanName : beanDefinitionNames) {
      BeanDefinition bd = beanDefinitionMap.get(beanName);
      if (bd != null && getMergedAnnotationOnBean(beanName, annotationType).isPresent()) {
        names.add(beanName);
      }
    }

    for (String beanName : getSingletonNames()) {
      if (!names.contains(beanName) && getMergedAnnotationOnBean(beanName, annotationType).isPresent()) {
        names.add(beanName);
      }
    }

    return names;
  }

  @Override
  public <A extends Annotation> A getAnnotationOnBean(String beanName, Class<A> annotationType) {
    return getMergedAnnotationOnBean(beanName, annotationType)
            .synthesize(MergedAnnotation::isPresent).orElse(null);
  }

  @Override
  public <A extends Annotation> MergedAnnotation<A> getMergedAnnotationOnBean(
          String beanName, Class<A> annotationType) throws NoSuchBeanDefinitionException {
    return findMergedAnnotationOnBean(beanName, annotationType);
  }

  private <A extends Annotation> MergedAnnotation<A> findMergedAnnotationOnBean(
          String beanName, Class<A> annotationType) {

    Class<?> beanType = getType(beanName);
    if (beanType != null) {
      MergedAnnotation<A> annotation =
              MergedAnnotations.from(beanType, SearchStrategy.TYPE_HIERARCHY).get(annotationType);
      if (annotation.isPresent()) {
        return annotation;
      }
    }

    if (containsBeanDefinition(beanName)) {
      BeanDefinition definition = beanDefinitionMap.get(beanName);
      if (definition instanceof AnnotatedBeanDefinition) {
        MethodMetadata methodMetadata = ((AnnotatedBeanDefinition) definition).getFactoryMethodMetadata();

        if (methodMetadata != null) {
          MergedAnnotation<A> annotation = methodMetadata.getAnnotations().get(annotationType);
          if (annotation.isPresent()) {
            return annotation;
          }
        }

        AnnotationMetadata annotationMetadata = ((AnnotatedBeanDefinition) definition).getMetadata();
        MergedAnnotation<A> annotation = annotationMetadata.getAnnotations().get(annotationType);
        if (annotation.isPresent()) {
          return annotation;
        }
      }

      // Check raw bean class, e.g. in case of a proxy.
      if (definition.hasBeanClass()) {
        Class<?> beanClass = definition.getBeanClass();
        if (beanClass != beanType) {
          MergedAnnotation<A> annotation =
                  MergedAnnotations.from(beanClass, SearchStrategy.TYPE_HIERARCHY).get(annotationType);
          if (annotation.isPresent()) {
            return annotation;
          }
        }
      }
    }
    return MergedAnnotation.missing();
  }

  //---------------------------------------------------------------------
  // Implementation of ConfigurableBeanFactory interface @since 4.0
  //---------------------------------------------------------------------

  @Override
  public void copyConfigurationFrom(ConfigurableBeanFactory otherFactory) {
    super.copyConfigurationFrom(otherFactory);
    if (otherFactory instanceof StandardBeanFactory) {
      this.allowBeanDefinitionOverriding = ((StandardBeanFactory) otherFactory).allowBeanDefinitionOverriding;
    }
  }

  @Override
  public void removeBean(String name) {
    removeBeanDefinition(name);
    super.removeBean(name);
  }

}

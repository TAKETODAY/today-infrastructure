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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.FactoryBean;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.core.annotation.MergedAnnotations.SearchStrategy;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.Prototype;
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
  private final ArrayList<String> beanDefinitionNames = new ArrayList<>(256);

  /**
   * Preventing Cycle Dependency expected {@link Prototype} beans
   */
  @Override
  public Object initializeBean(final Object bean, final BeanDefinition def) {
    if (def.isPrototype()) {
      return super.initializeBean(bean, def);
    }

    final String name = def.getName();
    if (currentInitializingBeanName.contains(name)) {
      return bean;
    }
    currentInitializingBeanName.add(name);
    final Object initializingBean = super.initializeBean(bean, def);
    currentInitializingBeanName.remove(name);
    return initializingBean;
  }

  /**
   * Register {@link FactoryBeanDefinition} to the {@link BeanFactory}
   *
   * @param oldBeanName Target old bean name
   * @param factoryDef {@link FactoryBean} Bean definition
   */
  protected void registerFactoryBean(final String oldBeanName, final BeanDefinition factoryDef) {

    final FactoryBeanDefinition<?> def = //
            factoryDef instanceof FactoryBeanDefinition
            ? (FactoryBeanDefinition<?>) factoryDef
            : new FactoryBeanDefinition<>(factoryDef, this);

    registerBeanDefinition(oldBeanName, def);
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

//    registerFactoryBean(beanName, def);

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
  }

  @Override
  public BeanDefinition getBeanDefinition(String beanName) {
    return beanDefinitionMap.get(beanName);
  }

  @Override
  public BeanDefinition getBeanDefinition(Class<?> beanClass) {
    BeanDefinition def = getBeanDefinition(createBeanName(beanClass));
    if (def != null) {
      if (isAssignableTo(def, beanClass)) {
        return def;
      }
    }

    for (BeanDefinition definition : beanDefinitionMap.values()) {
      if (isAssignableTo(definition, beanClass)) {
        return def;
      }
    }
    return null;
  }

  private boolean isAssignableTo(BeanDefinition definition, Class<?> beanClass) {
    if (definition.hasBeanClass()) {
      return definition.isAssignableTo(beanClass);
    }
    else {
      Class<?> candidateClass = resolveBeanClass(definition);
      return candidateClass != null && beanClass.isAssignableFrom(candidateClass);
    }
  }

  @Override
  public boolean containsBeanDefinition(Class<?> type) {
    return containsBeanDefinition(type, false);
  }

  @Override
  public boolean containsBeanDefinition(Class<?> type, boolean equals) {
    // TODO optimise lookup performance
    Predicate<BeanDefinition> predicate = getPredicate(type, equals);
    BeanDefinition def = getBeanDefinition(createBeanName(type));
    if (def != null && predicate.test(def)) {
      return true;
    }

    for (BeanDefinition beanDef : getBeanDefinitions().values()) {
      if (predicate.test(beanDef)) {
        return true;
      }
    }
    return false;
  }

  private Predicate<BeanDefinition> getPredicate(Class<?> type, boolean equals) {
    return equals
           ? beanDef -> type == beanDef.getBeanClass()
           : beanDef -> type.isAssignableFrom(beanDef.getBeanClass());
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
    return resolveBean(ResolvableType.fromClass(requiredType), false);
  }

  @Nullable
  private <T> T resolveBean(ResolvableType requiredType, boolean nonUniqueAsNull) {
    NamedBeanHolder<T> namedBean = resolveNamedBean(requiredType, nonUniqueAsNull);
    if (namedBean != null) {
      return namedBean.getBeanInstance();
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
          ResolvableType requiredType, boolean nonUniqueAsNull) throws BeansException {
    Assert.notNull(requiredType, "Required type must not be null");
    Set<String> candidateNames = getBeanNamesOfType(requiredType, true, true);

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
    Object bean = getBean(beanName, null);
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
    NamedBeanHolder<T> namedBean = resolveNamedBean(ResolvableType.fromClass(requiredType), false);
    if (namedBean != null) {
      return namedBean;
    }
    BeanFactory parent = getParentBeanFactory();
    if (parent instanceof AutowireCapableBeanFactory) {
      return ((AutowireCapableBeanFactory) parent).resolveNamedBean(requiredType);
    }
    throw new NoSuchBeanDefinitionException(requiredType);
  }

  @Override
  public Set<String> getBeanNamesOfType(Class<?> requiredType, boolean includeNonSingletons) {
    return getBeanNamesOfType(requiredType, true, includeNonSingletons);
  }

  @Override
  public Set<String> getBeanNamesOfType(
          Class<?> requiredType, boolean includeNoneRegistered, boolean includeNonSingletons) {
    LinkedHashSet<String> beanNames = new LinkedHashSet<>();

    for (Map.Entry<String, BeanDefinition> entry : getBeanDefinitions().entrySet()) {
      BeanDefinition def = entry.getValue();
      if (isEligibleBean(def, requiredType, includeNonSingletons)) {
        beanNames.add(entry.getKey());
      }
    }
    if (includeNoneRegistered) {
      synchronized(getSingletons()) {
        for (Map.Entry<String, Object> entry : getSingletons().entrySet()) {
          Object bean = entry.getValue();
          if (requiredType == null || requiredType.isInstance(bean)) {
            beanNames.add(entry.getKey());
          }
        }
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
      if (bd != null && !bd.isAbstract() && getMergedAnnotationOnBean(beanName, annotationType).isPresent()) {
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
      if (definition instanceof FactoryMethodBeanDefinition) {
        // Check annotations declared on factory method, if any.
        Method factoryMethod = ((FactoryMethodBeanDefinition) definition).getFactoryMethod();
        if (factoryMethod != null) {
          MergedAnnotation<A> annotation =
                  MergedAnnotations.from(factoryMethod, SearchStrategy.TYPE_HIERARCHY).get(annotationType);
          if (annotation.isPresent()) {
            return annotation;
          }
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

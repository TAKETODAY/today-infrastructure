/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.beans.factory;

import java.lang.annotation.Annotation;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import infra.beans.BeansException;
import infra.core.ResolvableType;
import infra.lang.Assert;
import infra.lang.Nullable;

/**
 * Convenience methods operating on bean factories, in particular
 * on the {@link BeanFactory} interface.
 *
 * <p>Returns bean counts, bean names or bean instances,
 * taking into account the nesting hierarchy of a bean factory
 * (which the methods defined on the BeanFactory interface don't,
 * in contrast to the methods defined on the BeanFactory interface).
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author TODAY 2021/9/30 23:49
 * @since 4.0
 */
public abstract class BeanFactoryUtils {

  /**
   * Separator for generated bean names. If a class name or parent name is not
   * unique, "#1", "#2" etc will be appended, until the name becomes unique.
   */
  public static final String GENERATED_BEAN_NAME_SEPARATOR = "#";

  /**
   * Cache from name with factory bean prefix to stripped name without dereference.
   *
   * @see BeanFactory#FACTORY_BEAN_PREFIX
   */
  private static final ConcurrentHashMap<String, String> transformedBeanNameCache = new ConcurrentHashMap<>();

  /**
   * Return whether the given name is a factory dereference
   * (beginning with the factory dereference prefix).
   *
   * @param name the name of the bean
   * @return whether the given name is a factory dereference
   * @see BeanFactory#FACTORY_BEAN_PREFIX
   */
  public static boolean isFactoryDereference(@Nullable String name) {
    return name != null && name.startsWith(BeanFactory.FACTORY_BEAN_PREFIX);
  }

  /**
   * Return the actual bean name, stripping out the factory dereference
   * prefix (if any, also stripping repeated factory prefixes if found).
   *
   * @param name the name of the bean
   * @return the transformed name
   * @see BeanFactory#FACTORY_BEAN_PREFIX
   */
  public static String transformedBeanName(String name) {
    Assert.notNull(name, "'name' is required");
    if (!name.startsWith(BeanFactory.FACTORY_BEAN_PREFIX)) {
      return name;
    }
    return transformedBeanNameCache.computeIfAbsent(name, beanName -> {
      do {
        beanName = beanName.substring(BeanFactory.FACTORY_BEAN_PREFIX.length());
      }
      while (beanName.startsWith(BeanFactory.FACTORY_BEAN_PREFIX));
      return beanName;
    });
  }

  /**
   * Return whether the given name is a bean name which has been generated
   * by the default naming strategy (containing a "#..." part).
   *
   * @param name the name of the bean
   * @return whether the given name is a generated bean name
   * @see #GENERATED_BEAN_NAME_SEPARATOR
   */
  public static boolean isGeneratedBeanName(@Nullable String name) {
    return (name != null && name.contains(GENERATED_BEAN_NAME_SEPARATOR));
  }

  /**
   * Extract the "raw" bean name from the given (potentially generated) bean name,
   * excluding any "#..." suffixes which might have been added for uniqueness.
   *
   * @param name the potentially generated bean name
   * @return the raw bean name
   * @see #GENERATED_BEAN_NAME_SEPARATOR
   */
  public static String originalBeanName(String name) {
    Assert.notNull(name, "'name' is required");
    int separatorIndex = name.indexOf(GENERATED_BEAN_NAME_SEPARATOR);
    return (separatorIndex != -1 ? name.substring(0, separatorIndex) : name);
  }

  // Retrieval of bean names

  /**
   * Count all beans in any hierarchy in which this factory participates.
   * Includes counts of ancestor bean factories.
   * <p>Beans that are "overridden" (specified in a descendant factory
   * with the same name) are only counted once.
   *
   * @param factory the bean factory
   * @return count of beans including those defined in ancestor factories
   * @see #beanNamesIncludingAncestors
   */
  public static int countBeansIncludingAncestors(BeanFactory factory) {
    return beanNamesIncludingAncestors(factory).size();
  }

  /**
   * Return all bean names in the factory, including ancestor factories.
   *
   * @param factory the bean factory
   * @return the set of matching bean names, or an empty array if none
   * @see #beanNamesForTypeIncludingAncestors
   */
  public static Set<String> beanNamesIncludingAncestors(BeanFactory factory) {
    return beanNamesForTypeIncludingAncestors(factory, Object.class);
  }

  /**
   * Get all bean names for the given type, including those defined in ancestor
   * factories. Will return unique names in case of overridden bean definitions.
   * <p>Does consider objects created by FactoryBeans, which means that FactoryBeans
   * will get initialized. If the object created by the FactoryBean doesn't match,
   * the raw FactoryBean itself will be matched against the type.
   * <p>This version of {@code beanNamesForTypeIncludingAncestors} automatically
   * includes prototypes and FactoryBeans.
   *
   * @param factory the bean factory
   * @param type the type that beans must match (as a {@code ResolvableType})
   * @return the set of matching bean names, or an empty array if none
   * @see BeanFactory#getBeansOfType(ResolvableType, boolean, boolean)
   */
  public static Set<String> beanNamesForTypeIncludingAncestors(BeanFactory factory, ResolvableType type) {
    Assert.notNull(factory, "BeanFactory is required");
    Set<String> result = factory.getBeanNamesForType(type);
    if (factory instanceof HierarchicalBeanFactory hbf) {
      if (hbf.getParentBeanFactory() != null) {
        Set<String> parentResult = beanNamesForTypeIncludingAncestors(
                hbf.getParentBeanFactory(), type);
        mergeNamesWithParent(result, parentResult, hbf);
      }
    }
    return result;
  }

  /**
   * Get all bean names for the given type, including those defined in ancestor
   * factories. Will return unique names in case of overridden bean definitions.
   * <p>Does consider objects created by FactoryBeans if the "allowEagerInit"
   * flag is set, which means that FactoryBeans will get initialized. If the
   * object created by the FactoryBean doesn't match, the raw FactoryBean itself
   * will be matched against the type. If "allowEagerInit" is not set,
   * only raw FactoryBeans will be checked (which doesn't require initialization
   * of each FactoryBean).
   *
   * @param factory the bean factory
   * @param type the type that beans must match (as a {@code ResolvableType})
   * @param includeNonSingletons whether to include prototype or scoped beans too
   * or just singletons (also applies to FactoryBeans)
   * @param allowEagerInit whether to initialize <i>lazy-init singletons</i> and
   * <i>objects created by FactoryBeans</i> (or by factory methods with a
   * "factory-bean" reference) for the type check. Note that FactoryBeans need to be
   * eagerly initialized to determine their type: So be aware that passing in "true"
   * for this flag will initialize FactoryBeans and "factory-bean" references.
   * @return the set of matching bean names, or an empty array if none
   * @see BeanFactory#getBeanNamesForType(ResolvableType, boolean, boolean)
   */
  public static Set<String> beanNamesForTypeIncludingAncestors(
          BeanFactory factory, ResolvableType type, boolean includeNonSingletons, boolean allowEagerInit) {

    Assert.notNull(factory, "BeanFactory is required");
    Set<String> result = factory.getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
    if (factory instanceof HierarchicalBeanFactory hbf) {
      if (hbf.getParentBeanFactory() != null) {
        Set<String> parentResult = beanNamesForTypeIncludingAncestors(
                hbf.getParentBeanFactory(), type, includeNonSingletons, allowEagerInit);
        mergeNamesWithParent(result, parentResult, hbf);
      }
    }
    return result;
  }

  /**
   * Get all bean names for the given type, including those defined in ancestor
   * factories. Will return unique names in case of overridden bean definitions.
   * <p>Does consider objects created by FactoryBeans, which means that FactoryBeans
   * will get initialized. If the object created by the FactoryBean doesn't match,
   * the raw FactoryBean itself will be matched against the type.
   * <p>This version of {@code beanNamesForTypeIncludingAncestors} automatically
   * includes prototypes and FactoryBeans.
   *
   * @param factory the bean factory
   * @param type the type that beans must match (as a {@code Class})
   * @return the set of matching bean names, or an empty array if none
   * @see BeanFactory#getBeanNamesForType(Class)
   */
  public static Set<String> beanNamesForTypeIncludingAncestors(BeanFactory factory, Class<?> type) {
    Assert.notNull(factory, "BeanFactory is required");
    Set<String> result = factory.getBeanNamesForType(type);
    if (factory instanceof HierarchicalBeanFactory hbf) {
      if (hbf.getParentBeanFactory() != null) {
        Set<String> parentResult = beanNamesForTypeIncludingAncestors(
                hbf.getParentBeanFactory(), type);
        mergeNamesWithParent(result, parentResult, hbf);
      }
    }
    return result;
  }

  /**
   * Get all bean names for the given type, including those defined in ancestor
   * factories. Will return unique names in case of overridden bean definitions.
   * <p>Does consider objects created by FactoryBeans if the "allowEagerInit"
   * flag is set, which means that FactoryBeans will get initialized. If the
   * object created by the FactoryBean doesn't match, the raw FactoryBean itself
   * will be matched against the type. If "allowEagerInit" is not set,
   * only raw FactoryBeans will be checked (which doesn't require initialization
   * of each FactoryBean).
   *
   * @param factory the bean factory
   * @param includeNonSingletons whether to include prototype or scoped beans too
   * or just singletons (also applies to FactoryBeans)
   * @param allowEagerInit whether to initialize <i>lazy-init singletons</i> and
   * <i>objects created by FactoryBeans</i> (or by factory methods with a
   * "factory-bean" reference) for the type check. Note that FactoryBeans need to be
   * eagerly initialized to determine their type: So be aware that passing in "true"
   * for this flag will initialize FactoryBeans and "factory-bean" references.
   * @param type the type that beans must match
   * @return the set of matching bean names, or an empty array if none
   * @see BeanFactory#getBeanNamesForType(Class, boolean, boolean)
   */
  public static Set<String> beanNamesForTypeIncludingAncestors(
          BeanFactory factory, Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {

    Assert.notNull(factory, "BeanFactory is required");
    Set<String> result = factory.getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
    if (factory instanceof HierarchicalBeanFactory hbf) {
      if (hbf.getParentBeanFactory() != null) {
        Set<String> parentResult = beanNamesForTypeIncludingAncestors(
                hbf.getParentBeanFactory(), type, includeNonSingletons, allowEagerInit);
        mergeNamesWithParent(result, parentResult, hbf);
      }
    }
    return result;
  }

  /**
   * Get all bean names whose {@code Class} has the supplied {@link Annotation}
   * type, including those defined in ancestor factories, without creating any bean
   * instances yet. Will return unique names in case of overridden bean definitions.
   *
   * @param factory the bean factory
   * @param annotationType the type of annotation to look for
   * @return the set of matching bean names, or an empty array if none
   * @see BeanFactory#getBeanNamesForAnnotation(Class)
   */
  public static Set<String> beanNamesForAnnotationIncludingAncestors(
          BeanFactory factory, Class<? extends Annotation> annotationType) {

    Assert.notNull(factory, "BeanFactory is required");
    Set<String> result = factory.getBeanNamesForAnnotation(annotationType);
    if (factory instanceof HierarchicalBeanFactory hbf) {
      if (hbf.getParentBeanFactory() != null) {
        Set<String> parentResult = beanNamesForAnnotationIncludingAncestors(
                hbf.getParentBeanFactory(), annotationType);
        mergeNamesWithParent(result, parentResult, hbf);
      }
    }
    return result;
  }

  // Retrieval of bean instances

  /**
   * Return all beans of the given type or subtypes, also picking up beans defined in
   * ancestor bean factories if the current bean factory is a HierarchicalBeanFactory.
   * The returned Map will only contain beans of this type.
   * <p>Does consider objects created by FactoryBeans, which means that FactoryBeans
   * will get initialized. If the object created by the FactoryBean doesn't match,
   * the raw FactoryBean itself will be matched against the type.
   * <p><b>Note: Beans of the same name will take precedence at the 'lowest' factory level,
   * i.e. such beans will be returned from the lowest factory that they are being found in,
   * hiding corresponding beans in ancestor factories.</b> This feature allows for
   * 'replacing' beans by explicitly choosing the same bean name in a child factory;
   * the bean in the ancestor factory won't be visible then, not even for by-type lookups.
   *
   * @param factory the bean factory
   * @param type type of bean to match
   * @return the Map of matching bean instances, or an empty Map if none
   * @throws BeansException if a bean could not be created
   * @see BeanFactory#getBeansOfType(Class)
   */
  public static <T> Map<String, T> beansOfTypeIncludingAncestors(BeanFactory factory, Class<T> type) throws BeansException {
    Assert.notNull(factory, "BeanFactory is required");
    LinkedHashMap<String, T> result = new LinkedHashMap<>(4);
    result.putAll(factory.getBeansOfType(type));
    if (factory instanceof HierarchicalBeanFactory hbf) {
      BeanFactory parentBeanFactory = hbf.getParentBeanFactory();
      if (parentBeanFactory != null) {
        Map<String, T> parentResult = beansOfTypeIncludingAncestors(
                parentBeanFactory, type);

        for (Map.Entry<String, T> entry : parentResult.entrySet()) {
          String beanName = entry.getKey();
          if (!result.containsKey(beanName) && !hbf.containsLocalBean(beanName)) {
            result.put(beanName, entry.getValue());
          }
        }
      }
    }
    return result;
  }

  /**
   * Return all beans of the given type or subtypes, also picking up beans defined in
   * ancestor bean factories if the current bean factory is a HierarchicalBeanFactory.
   * The returned Map will only contain beans of this type.
   * <p>Does consider objects created by FactoryBeans if the "allowEagerInit" flag is set,
   * which means that FactoryBeans will get initialized. If the object created by the
   * FactoryBean doesn't match, the raw FactoryBean itself will be matched against the
   * type. If "allowEagerInit" is not set, only raw FactoryBeans will be checked
   * (which doesn't require initialization of each FactoryBean).
   * <p><b>Note: Beans of the same name will take precedence at the 'lowest' factory level,
   * i.e. such beans will be returned from the lowest factory that they are being found in,
   * hiding corresponding beans in ancestor factories.</b> This feature allows for
   * 'replacing' beans by explicitly choosing the same bean name in a child factory;
   * the bean in the ancestor factory won't be visible then, not even for by-type lookups.
   *
   * @param factory the bean factory
   * @param type type of bean to match
   * @param includeNonSingletons whether to include prototype or scoped beans too
   * or just singletons (also applies to FactoryBeans)
   * @param allowEagerInit whether to initialize <i>lazy-init singletons</i> and
   * <i>objects created by FactoryBeans</i> (or by factory methods with a
   * "factory-bean" reference) for the type check. Note that FactoryBeans need to be
   * eagerly initialized to determine their type: So be aware that passing in "true"
   * for this flag will initialize FactoryBeans and "factory-bean" references.
   * @return the Map of matching bean instances, or an empty Map if none
   * @throws BeansException if a bean could not be created
   * @see BeanFactory#getBeansOfType(Class, boolean, boolean)
   */
  public static <T> Map<String, T> beansOfTypeIncludingAncestors(BeanFactory factory,
          @Nullable Class<T> type, boolean includeNonSingletons, boolean allowEagerInit) throws BeansException {
    Assert.notNull(factory, "BeanFactory is required");

    LinkedHashMap<String, T> result = new LinkedHashMap<>(4);
    result.putAll(factory.getBeansOfType(type, includeNonSingletons, allowEagerInit));
    if (factory instanceof HierarchicalBeanFactory hbf) {
      if (hbf.getParentBeanFactory() != null) {
        Map<String, T> parentResult = beansOfTypeIncludingAncestors(
                hbf.getParentBeanFactory(), type, includeNonSingletons, allowEagerInit);

        for (Map.Entry<String, T> entry : parentResult.entrySet()) {
          String beanName = entry.getKey();
          if (!result.containsKey(beanName) && !hbf.containsLocalBean(beanName)) {
            result.put(beanName, entry.getValue());
          }
        }
      }
    }
    return result;
  }

  /**
   * Return a single bean of the given type or subtypes, also picking up beans
   * defined in ancestor bean factories if the current bean factory is a
   * HierarchicalBeanFactory. Useful convenience method when we expect a
   * single bean and don't care about the bean name.
   * <p>Does consider objects created by FactoryBeans, which means that FactoryBeans
   * will get initialized. If the object created by the FactoryBean doesn't match,
   * the raw FactoryBean itself will be matched against the type.
   * <p>This version of {@code beanOfTypeIncludingAncestors} automatically includes
   * prototypes and FactoryBeans.
   * <p><b>Note: Beans of the same name will take precedence at the 'lowest' factory level,
   * i.e. such beans will be returned from the lowest factory that they are being found in,
   * hiding corresponding beans in ancestor factories.</b> This feature allows for
   * 'replacing' beans by explicitly choosing the same bean name in a child factory;
   * the bean in the ancestor factory won't be visible then, not even for by-type lookups.
   *
   * @param factory the bean factory
   * @param type type of bean to match
   * @return the matching bean instance
   * @throws NoSuchBeanDefinitionException if no bean of the given type was found
   * @throws NoUniqueBeanDefinitionException if more than one bean of the given type was found
   * @throws BeansException if the bean could not be created
   * @see #beansOfTypeIncludingAncestors(BeanFactory, Class)
   */
  public static <T> T beanOfTypeIncludingAncestors(BeanFactory factory, Class<T> type) throws BeansException {
    Map<String, T> beansOfType = beansOfTypeIncludingAncestors(factory, type);
    return uniqueBean(type, beansOfType);
  }

  /**
   * Return a single bean of the given type or subtypes, also picking up beans
   * defined in ancestor bean factories if the current bean factory is a
   * HierarchicalBeanFactory. Useful convenience method when we expect a
   * single bean and don't care about the bean name.
   * <p>Does consider objects created by FactoryBeans if the "allowEagerInit" flag is set,
   * which means that FactoryBeans will get initialized. If the object created by the
   * FactoryBean doesn't match, the raw FactoryBean itself will be matched against the
   * type. If "allowEagerInit" is not set, only raw FactoryBeans will be checked
   * (which doesn't require initialization of each FactoryBean).
   * <p><b>Note: Beans of the same name will take precedence at the 'lowest' factory level,
   * i.e. such beans will be returned from the lowest factory that they are being found in,
   * hiding corresponding beans in ancestor factories.</b> This feature allows for
   * 'replacing' beans by explicitly choosing the same bean name in a child factory;
   * the bean in the ancestor factory won't be visible then, not even for by-type lookups.
   *
   * @param factory the bean factory
   * @param type type of bean to match
   * @param includeNonSingletons whether to include prototype or scoped beans too
   * or just singletons (also applies to FactoryBeans)
   * @param allowEagerInit whether to initialize <i>lazy-init singletons</i> and
   * <i>objects created by FactoryBeans</i> (or by factory methods with a
   * "factory-bean" reference) for the type check. Note that FactoryBeans need to be
   * eagerly initialized to determine their type: So be aware that passing in "true"
   * for this flag will initialize FactoryBeans and "factory-bean" references.
   * @return the matching bean instance
   * @throws NoSuchBeanDefinitionException if no bean of the given type was found
   * @throws NoUniqueBeanDefinitionException if more than one bean of the given type was found
   * @throws BeansException if the bean could not be created
   * @see #beansOfTypeIncludingAncestors(BeanFactory, Class, boolean, boolean)
   */
  public static <T> T beanOfTypeIncludingAncestors(BeanFactory factory,
          Class<T> type, boolean includeNonSingletons, boolean allowEagerInit) throws BeansException {

    Map<String, T> beansOfType = beansOfTypeIncludingAncestors(factory, type, includeNonSingletons, allowEagerInit);
    return uniqueBean(type, beansOfType);
  }

  /**
   * Return a single bean of the given type or subtypes, not looking in ancestor
   * factories. Useful convenience method when we expect a single bean and
   * don't care about the bean name.
   * <p>Does consider objects created by FactoryBeans, which means that FactoryBeans
   * will get initialized. If the object created by the FactoryBean doesn't match,
   * the raw FactoryBean itself will be matched against the type.
   * <p>This version of {@code beanOfType} automatically includes
   * prototypes and FactoryBeans.
   *
   * @param factory the bean factory
   * @param type type of bean to match
   * @return the matching bean instance
   * @throws NoSuchBeanDefinitionException if no bean of the given type was found
   * @throws NoUniqueBeanDefinitionException if more than one bean of the given type was found
   * @throws BeansException if the bean could not be created
   * @see BeanFactory#getBeansOfType(Class)
   */
  public static <T> T beanOfType(BeanFactory factory, Class<T> type) throws BeansException {
    Assert.notNull(factory, "BeanFactory is required");
    Map<String, T> beansOfType = factory.getBeansOfType(type);
    return uniqueBean(type, beansOfType);
  }

  /**
   * Return a single bean of the given type or subtypes, not looking in ancestor
   * factories. Useful convenience method when we expect a single bean and
   * don't care about the bean name.
   * <p>Does consider objects created by FactoryBeans if the "allowEagerInit"
   * flag is set, which means that FactoryBeans will get initialized. If the
   * object created by the FactoryBean doesn't match, the raw FactoryBean itself
   * will be matched against the type. If "allowEagerInit" is not set,
   * only raw FactoryBeans will be checked (which doesn't require initialization
   * of each FactoryBean).
   *
   * @param factory the bean factory
   * @param type type of bean to match
   * @param includeNonSingletons whether to include prototype or scoped beans too
   * or just singletons (also applies to FactoryBeans)
   * @param allowEagerInit whether to initialize <i>lazy-init singletons</i> and
   * <i>objects created by FactoryBeans</i> (or by factory methods with a
   * "factory-bean" reference) for the type check. Note that FactoryBeans need to be
   * eagerly initialized to determine their type: So be aware that passing in "true"
   * for this flag will initialize FactoryBeans and "factory-bean" references.
   * @return the matching bean instance
   * @throws NoSuchBeanDefinitionException if no bean of the given type was found
   * @throws NoUniqueBeanDefinitionException if more than one bean of the given type was found
   * @throws BeansException if the bean could not be created
   * @see BeanFactory#getBeansOfType(Class, boolean, boolean)
   */
  public static <T> T beanOfType(BeanFactory factory, Class<T> type,
          boolean includeNonSingletons, boolean allowEagerInit) throws BeansException {
    Assert.notNull(factory, "BeanFactory is required");

    Map<String, T> beansOfType = factory.getBeansOfType(type, includeNonSingletons, allowEagerInit);
    return uniqueBean(type, beansOfType);
  }

  /**
   * Merge the given bean names result with the given parent result.
   *
   * @param result the local bean name result ,the merged result (possibly the local result as-is)
   * @param parentResult the parent bean name result (possibly empty)
   * @param hbf the local bean factory
   */
  private static void mergeNamesWithParent(Set<String> result,
          Set<String> parentResult, HierarchicalBeanFactory hbf) {
    if (!parentResult.isEmpty()) {
      for (String beanName : parentResult) {
        if (!result.contains(beanName) && !hbf.containsLocalBean(beanName)) {
          result.add(beanName);
        }
      }
    }
  }

  /**
   * Extract a unique bean for the given type from the given Map of matching beans.
   *
   * @param type type of bean to match
   * @param matchingBeans all matching beans found
   * @return the unique bean instance
   * @throws NoSuchBeanDefinitionException if no bean of the given type was found
   * @throws NoUniqueBeanDefinitionException if more than one bean of the given type was found
   */
  private static <T> T uniqueBean(Class<T> type, Map<String, T> matchingBeans) {
    int count = matchingBeans.size();
    if (count == 1) {
      return matchingBeans.values().iterator().next();
    }
    else if (count > 1) {
      throw new NoUniqueBeanDefinitionException(type, matchingBeans.keySet());
    }
    else {
      throw new NoSuchBeanDefinitionException(type);
    }
  }

  @Nullable
  public static <T> T find(BeanFactory beanFactory, String beanName) {
    return find(beanFactory, beanName, null);
  }

  @Nullable
  public static <T> T find(BeanFactory beanFactory, String beanName, @Nullable Class<T> requiredType) {
    return find(beanFactory, beanName, requiredType, null);
  }

  @Nullable
  public static <T> T find(BeanFactory beanFactory, String beanName,
          @Nullable Class<T> requiredType, @Nullable Supplier<T> defaultValue) {
    if (beanFactory.containsBean(beanName)) {
      return beanFactory.getBean(beanName, requiredType);
    }
    if (defaultValue != null) {
      return defaultValue.get();
    }
    return null;
  }

  @Nullable
  public static <T> T findLocal(BeanFactory beanFactory, String beanName, Class<T> requiredType) {
    if (beanFactory instanceof HierarchicalBeanFactory hbc) {
      if (hbc.containsLocalBean(beanName)) {
        return hbc.getBean(beanName, requiredType);
      }
    }
    else {
      try {
        return beanFactory.getBean(beanName, requiredType);
      }
      catch (NoSuchBeanDefinitionException ignored) {
      }
    }
    return null;
  }

  // type lookup

  @Nullable
  public static <T> T find(BeanFactory beanFactory, Class<T> requiredType) {
    return find(beanFactory, requiredType, null);
  }

  @Nullable
  public static <T> T find(BeanFactory beanFactory, Class<T> requiredType, @Nullable Supplier<T> defaultValue) {
    try {
      return beanFactory.getBean(requiredType);
    }
    catch (NoSuchBeanDefinitionException ignored) {
    }
    if (defaultValue != null) {
      return defaultValue.get();
    }
    return null;
  }

}

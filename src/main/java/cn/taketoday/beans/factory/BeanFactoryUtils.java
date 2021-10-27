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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.Primary;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.StringUtils;

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
  private static final Logger log = LoggerFactory.getLogger(BeanFactoryUtils.class);

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
    return (name != null && StringUtils.matchesFirst(name, BeanFactory.FACTORY_BEAN_PREFIX_CHAR));
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
    Assert.notNull(name, "'name' must not be null");
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
    Assert.notNull(name, "'name' must not be null");
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
   * @param lbf the bean factory
   * @return count of beans including those defined in ancestor factories
   * @see #beanNamesIncludingAncestors
   */
  public static int countBeansIncludingAncestors(BeanFactory lbf) {
    return beanNamesIncludingAncestors(lbf).size();
  }

  /**
   * Return all bean names in the factory, including ancestor factories.
   *
   * @param lbf the bean factory
   * @return the array of matching bean names, or an empty array if none
   * @see #beanNamesForTypeIncludingAncestors
   */
  public static Set<String> beanNamesIncludingAncestors(BeanFactory lbf) {
    return beanNamesForTypeIncludingAncestors(lbf, Object.class);
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
   * @param lbf the bean factory
   * @param type the type that beans must match (as a {@code ResolvableType})
   * @return the array of matching bean names, or an empty array if none
   * @see BeanFactory#getBeansOfType(ResolvableType, boolean, boolean)
   */
  public static Set<String> beanNamesForTypeIncludingAncestors(BeanFactory lbf, ResolvableType type) {
    Assert.notNull(lbf, "BeanFactory must not be null");
    Set<String> result = lbf.getBeanNamesOfType(type, true, true);
    if (lbf instanceof HierarchicalBeanFactory) {
      HierarchicalBeanFactory hbf = (HierarchicalBeanFactory) lbf;
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
   * @param lbf the bean factory
   * @param type the type that beans must match (as a {@code ResolvableType})
   * @param includeNonSingletons whether to include prototype or scoped beans too
   * or just singletons (also applies to FactoryBeans)
   * @param includeNoneRegistered whether to include singletons already in {@code singletons}
   * but not in {@code beanDefinitionMap}
   * @return the array of matching bean names, or an empty array if none
   * @see BeanFactory#getBeanNamesOfType(ResolvableType, boolean, boolean)
   */
  public static Set<String> beanNamesForTypeIncludingAncestors(
          BeanFactory lbf, ResolvableType type, boolean includeNoneRegistered, boolean includeNonSingletons) {

    Assert.notNull(lbf, "BeanFactory must not be null");
    Set<String> result = lbf.getBeanNamesOfType(type, includeNoneRegistered, includeNonSingletons);
    if (lbf instanceof HierarchicalBeanFactory) {
      HierarchicalBeanFactory hbf = (HierarchicalBeanFactory) lbf;
      if (hbf.getParentBeanFactory() != null) {
        Set<String> parentResult = beanNamesForTypeIncludingAncestors(
                hbf.getParentBeanFactory(), type, includeNoneRegistered, includeNonSingletons);
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
   * @param lbf the bean factory
   * @param type the type that beans must match (as a {@code Class})
   * @return the array of matching bean names, or an empty array if none
   * @see BeanFactory#getBeanNamesOfType(Class)
   */
  public static Set<String> beanNamesForTypeIncludingAncestors(BeanFactory lbf, Class<?> type) {
    Assert.notNull(lbf, "BeanFactory must not be null");
    Set<String> result = lbf.getBeanNamesOfType(type);
    if (lbf instanceof HierarchicalBeanFactory) {
      HierarchicalBeanFactory hbf = (HierarchicalBeanFactory) lbf;
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
   * @param lbf the bean factory
   * @param includeNonSingletons whether to include prototype or scoped beans too
   * or just singletons (also applies to FactoryBeans)
   * @param includeNoneRegistered whether to include singletons already in {@code singletons}
   * but not in {@code beanDefinitionMap}
   * @param type the type that beans must match
   * @return the array of matching bean names, or an empty array if none
   * @see BeanFactory#getBeanNamesOfType(Class, boolean, boolean)
   */
  public static Set<String> beanNamesForTypeIncludingAncestors(
          BeanFactory lbf, Class<?> type, boolean includeNoneRegistered, boolean includeNonSingletons) {

    Assert.notNull(lbf, "BeanFactory must not be null");
    Set<String> result = lbf.getBeanNamesOfType(type, includeNoneRegistered, includeNonSingletons);
    if (lbf instanceof HierarchicalBeanFactory) {
      HierarchicalBeanFactory hbf = (HierarchicalBeanFactory) lbf;
      if (hbf.getParentBeanFactory() != null) {
        Set<String> parentResult = beanNamesForTypeIncludingAncestors(
                hbf.getParentBeanFactory(), type, includeNoneRegistered, includeNonSingletons);
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
   * @param lbf the bean factory
   * @param annotationType the type of annotation to look for
   * @return the array of matching bean names, or an empty array if none
   * @see BeanFactory#getBeanNamesForAnnotation(Class)
   */
  public static Set<String> beanNamesForAnnotationIncludingAncestors(
          BeanFactory lbf, Class<? extends Annotation> annotationType) {

    Assert.notNull(lbf, "BeanFactory must not be null");
    Set<String> result = lbf.getBeanNamesForAnnotation(annotationType);
    if (lbf instanceof HierarchicalBeanFactory) {
      HierarchicalBeanFactory hbf = (HierarchicalBeanFactory) lbf;
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
   * @param lbf the bean factory
   * @param type type of bean to match
   * @return the Map of matching bean instances, or an empty Map if none
   * @throws BeansException if a bean could not be created
   * @see BeanFactory#getBeansOfType(Class)
   */
  public static <T> Map<String, T> beansOfTypeIncludingAncestors(
          BeanFactory lbf, Class<T> type) throws BeansException {

    Assert.notNull(lbf, "BeanFactory must not be null");
    Map<String, T> result = new LinkedHashMap<>(4);
    result.putAll(lbf.getBeansOfType(type));
    if (lbf instanceof HierarchicalBeanFactory) {
      HierarchicalBeanFactory hbf = (HierarchicalBeanFactory) lbf;
      if (hbf.getParentBeanFactory() != null) {
        Map<String, T> parentResult = beansOfTypeIncludingAncestors(
                hbf.getParentBeanFactory(), type);

        for (final Map.Entry<String, T> entry : parentResult.entrySet()) {
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
   * @param lbf the bean factory
   * @param type type of bean to match
   * @param includeNonSingletons whether to include prototype or scoped beans too
   * or just singletons (also applies to FactoryBeans)
   * @param includeNoneRegistered whether to include singletons already in {@code singletons}
   * but not in {@code beanDefinitionMap}
   * @return the Map of matching bean instances, or an empty Map if none
   * @throws BeansException if a bean could not be created
   * @see BeanFactory#getBeansOfType(Class, boolean, boolean)
   */
  public static <T> Map<String, T> beansOfTypeIncludingAncestors(
          BeanFactory lbf, Class<T> type, boolean includeNoneRegistered, boolean includeNonSingletons)
          throws BeansException //
  {
    Assert.notNull(lbf, "BeanFactory must not be null");

    LinkedHashMap<String, T> result = new LinkedHashMap<>(4);
    result.putAll(lbf.getBeansOfType(type, includeNoneRegistered, includeNonSingletons));
    if (lbf instanceof HierarchicalBeanFactory) {
      HierarchicalBeanFactory hbf = (HierarchicalBeanFactory) lbf;
      if (hbf.getParentBeanFactory() != null) {
        Map<String, T> parentResult = beansOfTypeIncludingAncestors(
                hbf.getParentBeanFactory(), type, includeNoneRegistered, includeNonSingletons);

        for (final Map.Entry<String, T> entry : parentResult.entrySet()) {
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
   * @param lbf the bean factory
   * @param type type of bean to match
   * @return the matching bean instance
   * @throws NoSuchBeanDefinitionException if no bean of the given type was found
   * @throws NoUniqueBeanException if more than one bean of the given type was found
   * @throws BeansException if the bean could not be created
   * @see #beansOfTypeIncludingAncestors(BeanFactory, Class)
   */
  public static <T> T beanOfTypeIncludingAncestors(BeanFactory lbf, Class<T> type) throws BeansException {
    Map<String, T> beansOfType = beansOfTypeIncludingAncestors(lbf, type);
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
   * @param lbf the bean factory
   * @param type type of bean to match
   * @param includeNonSingletons whether to include prototype or scoped beans too
   * or just singletons (also applies to FactoryBeans)
   * @param includeNoneRegistered whether to include singletons already in {@code singletons}
   * but not in {@code beanDefinitionMap}
   * @return the matching bean instance
   * @throws NoSuchBeanDefinitionException if no bean of the given type was found
   * @throws NoUniqueBeanException if more than one bean of the given type was found
   * @throws BeansException if the bean could not be created
   * @see #beansOfTypeIncludingAncestors(BeanFactory, Class, boolean, boolean)
   */
  public static <T> T beanOfTypeIncludingAncestors(
          BeanFactory lbf, Class<T> type, boolean includeNoneRegistered, boolean includeNonSingletons)
          throws BeansException //
  {
    Map<String, T> beansOfType = beansOfTypeIncludingAncestors(
            lbf, type, includeNoneRegistered, includeNonSingletons);
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
   * @param lbf the bean factory
   * @param type type of bean to match
   * @return the matching bean instance
   * @throws NoSuchBeanDefinitionException if no bean of the given type was found
   * @throws NoUniqueBeanException if more than one bean of the given type was found
   * @throws BeansException if the bean could not be created
   * @see BeanFactory#getBeansOfType(Class)
   */
  public static <T> T beanOfType(BeanFactory lbf, Class<T> type) throws BeansException {
    Assert.notNull(lbf, "BeanFactory must not be null");
    Map<String, T> beansOfType = lbf.getBeansOfType(type);
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
   * @param lbf the bean factory
   * @param type type of bean to match
   * @param includeNonSingletons whether to include prototype or scoped beans too
   * or just singletons (also applies to FactoryBeans)
   * @param includeNoneRegistered whether to include singletons already in {@code singletons}
   * but not in {@code beanDefinitionMap}
   * @return the matching bean instance
   * @throws NoSuchBeanDefinitionException if no bean of the given type was found
   * @throws NoUniqueBeanException if more than one bean of the given type was found
   * @throws BeansException if the bean could not be created
   * @see BeanFactory#getBeansOfType(Class, boolean, boolean)
   */
  public static <T> T beanOfType(
          BeanFactory lbf, Class<T> type,
          boolean includeNoneRegistered, boolean includeNonSingletons) throws BeansException {
    Assert.notNull(lbf, "BeanFactory must not be null");
    Map<String, T> beansOfType = lbf.getBeansOfType(type, includeNoneRegistered, includeNonSingletons);
    return uniqueBean(type, beansOfType);
  }

  /**
   * Merge the given bean names result with the given parent result.
   *
   * @param result the local bean name result ,the merged result (possibly the local result as-is)
   * @param parentResult the parent bean name result (possibly empty)
   * @param hbf the local bean factory
   */
  private static void mergeNamesWithParent(
          Set<String> result, Set<String> parentResult, HierarchicalBeanFactory hbf) {
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
   * @throws NoUniqueBeanException if more than one bean of the given type was found
   */
  private static <T> T uniqueBean(Class<T> type, Map<String, T> matchingBeans) {
    int count = matchingBeans.size();
    if (count == 1) {
      return matchingBeans.values().iterator().next();
    }
    else if (count > 1) {
      throw new NoUniqueBeanException(type, matchingBeans.keySet());
    }
    else {
      throw new NoSuchBeanDefinitionException(type);
    }
  }

  //

  /**
   * Get {@link Primary} {@link BeanDefinition}
   *
   * @param defs All suitable {@link BeanDefinition}s
   * @return A {@link Primary} {@link BeanDefinition}
   */
  public static BeanDefinition getPrimaryBeanDefinition(List<BeanDefinition> defs) {
    if (defs.size() > 1) {
      log.debug("Finding primary bean which annotated @Primary or primary flag is set, in {}", defs);
      ArrayList<BeanDefinition> primaries = new ArrayList<>(defs.size());
      for (BeanDefinition def : defs) {
        if (def.isPrimary()) {
          primaries.add(def);
        }
      }
      if (!primaries.isEmpty()) {
        AnnotationAwareOrderComparator.sort(primaries); // size > 1 sort  FIXME BeanDefinition cannot sort
        log.debug("Found primary beans {} use first one", primaries);
        return primaries.get(0);
      }
      // not found sort bean-defs
      AnnotationAwareOrderComparator.sort(defs);
    }
    return defs.get(0);
  }

}

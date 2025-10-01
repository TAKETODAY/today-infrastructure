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

package infra.beans.factory.annotation;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

import infra.beans.BeansException;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryUtils;
import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.beans.factory.NoUniqueBeanDefinitionException;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.support.AbstractBeanDefinition;
import infra.beans.factory.support.AutowireCandidateQualifier;
import infra.beans.factory.support.RootBeanDefinition;
import infra.core.annotation.AnnotationUtils;

/**
 * Convenience methods performing bean lookups related to Framework-specific annotations,
 * for example Framework's {@link Qualifier @Qualifier} annotation.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see BeanFactoryUtils
 * @since 4.0 2022/3/8 17:31
 */
public abstract class BeanFactoryAnnotationUtils {

  /**
   * Retrieve all beans of type {@code T} from the given {@code BeanFactory} declaring a
   * qualifier (e.g. via {@code <qualifier>} or {@code @Qualifier}) matching the given
   * qualifier, or having a bean name matching the given qualifier.
   *
   * @param beanFactory the factory to get the target beans from (also searching ancestors)
   * @param beanType the type of beans to retrieve
   * @param qualifier the qualifier for selecting among all type matches
   * @return the matching beans of type {@code T}
   * @throws BeansException if any of the matching beans could not be created
   * @see BeanFactoryUtils#beansOfTypeIncludingAncestors(BeanFactory, Class)
   */
  public static <T> Map<String, T> qualifiedBeansOfType(
          BeanFactory beanFactory, Class<T> beanType, String qualifier) throws BeansException {

    var candidateBeans = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, beanType);
    LinkedHashMap<String, T> result = new LinkedHashMap<>(4);
    for (String beanName : candidateBeans) {
      if (isQualifierMatch(qualifier::equals, beanName, beanFactory)) {
        result.put(beanName, beanFactory.getBean(beanName, beanType));
      }
    }
    return result;
  }

  /**
   * Obtain a bean of type {@code T} from the given {@code BeanFactory} declaring a qualifier
   * (e.g. {@code <qualifier>} or {@code @Qualifier}) matching the given qualifier).
   *
   * @param bf the factory to get the target bean from
   * @param beanType the type of bean to retrieve
   * @param qualifier the qualifier for selecting between multiple bean matches
   * @return the matching bean of type {@code T} (never {@code null})
   */
  public static <T> T qualifiedBeanOfType(BeanFactory bf, Class<T> beanType, String qualifier) {
    var candidateBeans = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(bf, beanType);
    String matchingBean = null;
    for (String beanName : candidateBeans) {
      if (isQualifierMatch(qualifier::equals, beanName, bf)) {
        if (matchingBean != null) {
          throw new NoUniqueBeanDefinitionException(beanType, matchingBean, beanName);
        }
        matchingBean = beanName;
      }
    }
    if (matchingBean != null) {
      return bf.getBean(matchingBean, beanType);
    }
    else if (bf.containsBean(qualifier) && bf.isTypeMatch(qualifier, beanType)) {
      // Fallback: target bean at least found by bean name.
      return bf.getBean(qualifier, beanType);
    }
    else {
      throw new NoSuchBeanDefinitionException(qualifier,
              "No matching %s bean found for qualifier '%s' - neither qualifier match nor bean name match!"
                      .formatted(beanType.getSimpleName(), qualifier));
    }
  }

  /**
   * Check whether the named bean declares a qualifier of the given name.
   *
   * @param qualifier the qualifier to match
   * @param beanName the name of the candidate bean
   * @param beanFactory the factory from which to retrieve the named bean
   * @return {@code true} if either the bean definition (in the XML case)
   * or the bean's factory method (in the {@code @Bean} case) defines a matching
   * qualifier value (through {@code <qualifier>} or {@code @Qualifier})
   */
  public static boolean isQualifierMatch(Predicate<String> qualifier, String beanName, @Nullable BeanFactory beanFactory) {

    // Try quick bean name or alias match first...
    if (qualifier.test(beanName)) {
      return true;
    }
    if (beanFactory != null) {
      for (String alias : beanFactory.getAliases(beanName)) {
        if (qualifier.test(alias)) {
          return true;
        }
      }
      try {
        Class<?> beanType = beanFactory.getType(beanName);
        if (beanFactory instanceof ConfigurableBeanFactory cbf) {
          BeanDefinition bd = cbf.getMergedBeanDefinition(beanName);
          // Explicit qualifier metadata on bean definition? (typically in XML definition)
          if (bd instanceof AbstractBeanDefinition abd) {
            AutowireCandidateQualifier candidate = abd.getQualifier(Qualifier.class.getName());
            if (candidate != null) {
              Object value = candidate.getAttribute(AutowireCandidateQualifier.VALUE_KEY);
              if (value != null && qualifier.test(value.toString())) {
                return true;
              }
            }
          }
          // Corresponding qualifier on factory method? (typically in configuration class)
          if (bd instanceof RootBeanDefinition rbd) {
            Method factoryMethod = rbd.getResolvedFactoryMethod();
            if (factoryMethod != null) {
              Qualifier targetAnnotation = AnnotationUtils.getAnnotation(factoryMethod, Qualifier.class);
              if (targetAnnotation != null) {
                return qualifier.test(targetAnnotation.value());
              }
            }
          }
        }
        // Corresponding qualifier on bean implementation class? (for custom user types)
        if (beanType != null) {
          Qualifier targetAnnotation = AnnotationUtils.getAnnotation(beanType, Qualifier.class);
          if (targetAnnotation != null) {
            return qualifier.test(targetAnnotation.value());
          }
        }
      }
      catch (NoSuchBeanDefinitionException ex) {
        // Ignore - can't compare qualifiers for a manually registered singleton object
      }
    }
    return false;
  }

  /**
   * Determine the {@link Qualifier#value() qualifier value} for the given
   * annotated element.
   *
   * @param annotatedElement the class, method or parameter to introspect
   * @return the associated qualifier value, or {@code null} if none
   * @since 4.0
   */
  @Nullable
  public static String getQualifierValue(AnnotatedElement annotatedElement) {
    Qualifier qualifier = AnnotationUtils.getAnnotation(annotatedElement, Qualifier.class);
    return (qualifier != null ? qualifier.value() : null);
  }

}

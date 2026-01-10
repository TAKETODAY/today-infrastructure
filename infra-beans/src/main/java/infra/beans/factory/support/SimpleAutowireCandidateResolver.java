/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.beans.factory.support;

import java.util.LinkedHashMap;
import java.util.Map;

import infra.beans.BeansException;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryUtils;
import infra.beans.factory.config.ConfigurableBeanFactory;

/**
 * {@link AutowireCandidateResolver} implementation to use when no annotation
 * support is available. This implementation checks the bean definition only.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/22 21:30
 */
public class SimpleAutowireCandidateResolver implements AutowireCandidateResolver {

  /**
   * This implementation returns {@code this} as-is.
   */
  @Override
  public AutowireCandidateResolver cloneIfNecessary() {
    return this;
  }

  /**
   * Resolve a map of all beans of the given type, also picking up beans defined in
   * ancestor bean factories, with the specific condition that each bean actually
   * has autowire candidate status. This matches simple injection point resolution
   * as implemented by this {@link AutowireCandidateResolver} strategy, including
   * beans which are not marked as default candidates but excluding beans which
   * are not even marked as autowire candidates.
   *
   * @param lbf the bean factory
   * @param type the type of bean to match
   * @return the Map of matching bean instances, or an empty Map if none
   * @throws BeansException if a bean could not be created
   * @see BeanFactoryUtils#beansOfTypeIncludingAncestors(BeanFactory, Class)
   * @see infra.beans.factory.config.BeanDefinition#isAutowireCandidate()
   * @see AbstractBeanDefinition#isDefaultCandidate()
   * @since 5.0
   */
  public static <T> Map<String, T> resolveAutowireCandidates(ConfigurableBeanFactory lbf, Class<T> type) {
    return resolveAutowireCandidates(lbf, type, true, true);
  }

  /**
   * Resolve a map of all beans of the given type, also picking up beans defined in
   * ancestor bean factories, with the specific condition that each bean actually
   * has autowire candidate status. This matches simple injection point resolution
   * as implemented by this {@link AutowireCandidateResolver} strategy, including
   * beans which are not marked as default candidates but excluding beans which
   * are not even marked as autowire candidates.
   *
   * @param lbf the bean factory
   * @param type the type of bean to match
   * @param includeNonSingletons whether to include prototype or scoped beans too
   * or just singletons (also applies to FactoryBeans)
   * @param allowEagerInit whether to initialize <i>lazy-init singletons</i> and
   * <i>objects created by FactoryBeans</i> (or by factory methods with a
   * "factory-bean" reference) for the type check. Note that FactoryBeans need to be
   * eagerly initialized to determine their type: So be aware that passing in "true"
   * for this flag will initialize FactoryBeans and "factory-bean" references.
   * @return the Map of matching bean instances, or an empty Map if none
   * @throws BeansException if a bean could not be created
   * @see BeanFactoryUtils#beansOfTypeIncludingAncestors(BeanFactory, Class, boolean, boolean)
   * @see infra.beans.factory.config.BeanDefinition#isAutowireCandidate()
   * @see AbstractBeanDefinition#isDefaultCandidate()
   * @since 5.0
   */
  @SuppressWarnings("unchecked")
  public static <T> Map<String, T> resolveAutowireCandidates(ConfigurableBeanFactory lbf, Class<T> type,
          boolean includeNonSingletons, boolean allowEagerInit) {

    LinkedHashMap<String, T> candidates = new LinkedHashMap<>();
    for (String beanName : BeanFactoryUtils.beanNamesForTypeIncludingAncestors(lbf, type, includeNonSingletons, allowEagerInit)) {
      if (AutowireUtils.isAutowireCandidate(lbf, beanName)) {
        Object bean = lbf.getBean(beanName);
        if (bean != null) {
          candidates.put(beanName, (T) bean);
        }
      }
    }
    return candidates;
  }
}


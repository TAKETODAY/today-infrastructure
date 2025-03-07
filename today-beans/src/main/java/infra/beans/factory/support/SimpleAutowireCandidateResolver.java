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
  @SuppressWarnings("unchecked")
  public static <T> Map<String, T> resolveAutowireCandidates(ConfigurableBeanFactory lbf, Class<T> type) {
    Map<String, T> candidates = new LinkedHashMap<>();
    for (String beanName : BeanFactoryUtils.beanNamesForTypeIncludingAncestors(lbf, type)) {
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


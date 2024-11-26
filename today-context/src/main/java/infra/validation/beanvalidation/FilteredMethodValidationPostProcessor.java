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

package infra.validation.beanvalidation;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;

import infra.aop.support.ComposablePointcut;
import infra.aop.support.DefaultPointcutAdvisor;

/**
 * Custom {@link MethodValidationPostProcessor} that applies
 * {@link MethodValidationExcludeFilter exclusion filters}.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class FilteredMethodValidationPostProcessor extends MethodValidationPostProcessor {

  @Serial
  private static final long serialVersionUID = 1L;

  private final Collection<MethodValidationExcludeFilter> excludeFilters;

  /**
   * Creates a new {@code FilteredMethodValidationPostProcessor} that will apply the
   * given {@code excludeFilters} when identifying beans that are eligible for method
   * validation post-processing.
   *
   * @param excludeFilters filters to apply
   */
  public FilteredMethodValidationPostProcessor(Stream<MethodValidationExcludeFilter> excludeFilters) {
    this.excludeFilters = excludeFilters.toList();
  }

  /**
   * Creates a new {@code FilteredMethodValidationPostProcessor} that will apply the
   * given {@code excludeFilters} when identifying beans that are eligible for method
   * validation post-processing.
   *
   * @param excludeFilters filters to apply
   */
  public FilteredMethodValidationPostProcessor(Collection<MethodValidationExcludeFilter> excludeFilters) {
    this.excludeFilters = new ArrayList<>(excludeFilters);
  }

  @Override
  public void afterPropertiesSet() {
    super.afterPropertiesSet();
    if (advisor instanceof DefaultPointcutAdvisor advisor) {
      advisor.setPointcut(new ComposablePointcut(advisor.getPointcut())
              .intersection(this::isIncluded));
    }
  }

  private boolean isIncluded(Class<?> candidate) {
    for (MethodValidationExcludeFilter exclusionFilter : this.excludeFilters) {
      if (exclusionFilter.isExcluded(candidate)) {
        return false;
      }
    }
    return true;
  }

}

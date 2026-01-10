/*
 * Copyright 2012-present the original author or authors.
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

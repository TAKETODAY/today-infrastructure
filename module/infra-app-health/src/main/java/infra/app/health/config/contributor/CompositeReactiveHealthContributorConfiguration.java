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

package infra.app.health.config.contributor;

import java.util.Map;
import java.util.function.Function;

import infra.app.health.contributor.CompositeReactiveHealthContributor;
import infra.app.health.contributor.ReactiveHealthContributor;
import infra.app.health.contributor.ReactiveHealthIndicator;

/**
 * Base class for health contributor configurations that can combine source beans into a
 * composite.
 *
 * @param <I> the health indicator type
 * @param <B> the bean type
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public abstract class CompositeReactiveHealthContributorConfiguration<I extends ReactiveHealthIndicator, B>
        extends AbstractCompositeHealthContributorConfiguration<ReactiveHealthContributor, I, B> {

  /**
   * Creates a {@code CompositeReactiveHealthContributorConfiguration} that will use the
   * given {@code indicatorFactory} to create {@link ReactiveHealthIndicator} instances.
   *
   * @param indicatorFactory the function to create health indicator instances
   */
  public CompositeReactiveHealthContributorConfiguration(Function<B, I> indicatorFactory) {
    super(indicatorFactory);
  }

  @Override
  protected final ReactiveHealthContributor createComposite(Map<String, B> beans) {
    return CompositeReactiveHealthContributor.fromMap(beans, this::createIndicator);
  }

}

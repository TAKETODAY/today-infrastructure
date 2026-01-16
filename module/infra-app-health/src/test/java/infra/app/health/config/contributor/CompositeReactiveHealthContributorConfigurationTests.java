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

import java.util.stream.Stream;

import infra.app.health.config.contributor.CompositeReactiveHealthContributorConfigurationTests.TestReactiveHealthIndicator;
import infra.app.health.contributor.AbstractReactiveHealthIndicator;
import infra.app.health.contributor.Health;
import infra.app.health.contributor.ReactiveHealthContributor;
import infra.app.health.contributor.ReactiveHealthContributors;
import reactor.core.publisher.Mono;

/**
 * Tests for {@link CompositeReactiveHealthContributorConfiguration}.
 *
 * @author Phillip Webb
 */
class CompositeReactiveHealthContributorConfigurationTests extends
        AbstractCompositeHealthContributorConfigurationTests<ReactiveHealthContributor, TestReactiveHealthIndicator> {

  @Override
  protected AbstractCompositeHealthContributorConfiguration<ReactiveHealthContributor, TestReactiveHealthIndicator, TestBean> newComposite() {
    return new TestCompositeReactiveHealthContributorConfiguration();
  }

  @Override
  protected Stream<String> allNamesFromComposite(ReactiveHealthContributor composite) {
    return ((ReactiveHealthContributors) composite).stream().map(ReactiveHealthContributors.Entry::name);
  }

  static class TestCompositeReactiveHealthContributorConfiguration
          extends CompositeReactiveHealthContributorConfiguration<TestReactiveHealthIndicator, TestBean> {

    TestCompositeReactiveHealthContributorConfiguration() {
      super(TestReactiveHealthIndicator::new);
    }

  }

  static class TestReactiveHealthIndicator extends AbstractReactiveHealthIndicator {

    TestReactiveHealthIndicator(TestBean testBean) {
    }

    @Override
    protected Mono<Health> doHealthCheck(Health.Builder builder) {
      return Mono.just(builder.up().build());
    }

  }

}

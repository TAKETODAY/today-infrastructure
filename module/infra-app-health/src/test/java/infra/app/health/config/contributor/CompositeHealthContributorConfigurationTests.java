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

import infra.app.health.config.contributor.CompositeHealthContributorConfigurationTests.TestHealthIndicator;
import infra.app.health.contributor.AbstractHealthIndicator;
import infra.app.health.contributor.Health;
import infra.app.health.contributor.HealthContributor;
import infra.app.health.contributor.HealthContributors;

/**
 * Tests for {@link CompositeHealthContributorConfiguration}.
 *
 * @author Phillip Webb
 */
class CompositeHealthContributorConfigurationTests
        extends AbstractCompositeHealthContributorConfigurationTests<HealthContributor, TestHealthIndicator> {

  @Override
  protected AbstractCompositeHealthContributorConfiguration<HealthContributor, TestHealthIndicator, TestBean> newComposite() {
    return new TestCompositeHealthContributorConfiguration();
  }

  @Override
  protected Stream<String> allNamesFromComposite(HealthContributor composite) {
    return ((HealthContributors) composite).stream().map(HealthContributors.Entry::name);
  }

  static class TestCompositeHealthContributorConfiguration
          extends CompositeHealthContributorConfiguration<TestHealthIndicator, TestBean> {

    TestCompositeHealthContributorConfiguration() {
      super(TestHealthIndicator::new);
    }

  }

  static class TestHealthIndicator extends AbstractHealthIndicator {

    TestHealthIndicator(TestBean testBean) {
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
      builder.up();
    }

  }

}

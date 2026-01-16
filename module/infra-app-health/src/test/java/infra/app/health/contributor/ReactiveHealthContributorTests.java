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

package infra.app.health.contributor;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ReactiveHealthContributor}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class ReactiveHealthContributorTests {

  @Test
  void adaptWhenHealthIndicatorReturnsHealthIndicatorReactiveAdapter() {
    HealthIndicator indicator = () -> Health.outOfService().build();
    ReactiveHealthContributor adapted = ReactiveHealthContributor.adapt(indicator);
    assertThat(adapted).isInstanceOf(HealthIndicatorAdapter.class);
    Health health = ((ReactiveHealthIndicator) adapted).health().block();
    assertThat(health).isNotNull();
    assertThat(health.getStatus()).isEqualTo(Status.OUT_OF_SERVICE);
  }

  @Test
  void adaptWhenCompositeHealthContributorReturnsCompositeHealthContributorReactiveAdapter() {
    HealthIndicator indicator = () -> Health.outOfService().build();
    CompositeHealthContributor contributor = CompositeHealthContributor
            .fromMap(Collections.singletonMap("a", indicator));
    ReactiveHealthContributor adapted = ReactiveHealthContributor.adapt(contributor);
    assertThat(adapted).isInstanceOf(CompositeHealthContributorAdapter.class);
    ReactiveHealthContributor contained = ((CompositeReactiveHealthContributor) adapted).getContributor("a");
    assertThat(contained).isNotNull();
    Health health = ((ReactiveHealthIndicator) contained).health().block();
    assertThat(health).isNotNull();
    assertThat(health.getStatus()).isEqualTo(Status.OUT_OF_SERVICE);
  }

}

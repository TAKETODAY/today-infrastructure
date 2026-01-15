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

package infra.app.health.config.registry;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

import infra.app.health.contributor.HealthContributor;
import infra.app.health.contributor.ReactiveHealthContributor;
import infra.app.health.registry.DefaultHealthContributorRegistry;
import infra.app.health.registry.DefaultReactiveHealthContributorRegistry;
import infra.app.health.registry.HealthContributorNameValidator;
import infra.app.health.registry.HealthContributorRegistry;
import infra.app.health.registry.ReactiveHealthContributorRegistry;
import infra.beans.factory.annotation.NonOrdered;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.Lazy;
import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.annotation.config.EnableAutoConfiguration;
import infra.context.condition.ConditionalOnClass;
import infra.context.condition.ConditionalOnMissingBean;
import reactor.core.publisher.Flux;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link HealthContributorRegistry} and {@link ReactiveHealthContributorRegistry}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
@Lazy
@DisableDIAutoConfiguration
public final class HealthContributorRegistryAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(HealthContributorRegistry.class)
  static DefaultHealthContributorRegistry healthContributorRegistry(Map<String, HealthContributor> contributorBeans,
          @Nullable HealthContributorNameGenerator nameGenerator, @NonOrdered List<HealthContributorNameValidator> nameValidators) {

    if (nameGenerator == null) {
      nameGenerator = HealthContributorNameGenerator.withoutStandardSuffixes();
    }

    return new DefaultHealthContributorRegistry(nameValidators, nameGenerator.registrar(contributorBeans));
  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(Flux.class)
  static class ReactiveHealthContributorRegistryConfiguration {

    @Bean
    @ConditionalOnMissingBean(ReactiveHealthContributorRegistry.class)
    static DefaultReactiveHealthContributorRegistry reactiveHealthContributorRegistry(
            Map<String, ReactiveHealthContributor> contributorBeans, @Nullable HealthContributorNameGenerator nameGenerator,
            @NonOrdered List<HealthContributorNameValidator> nameValidators) {

      if (nameGenerator == null) {
        nameGenerator = HealthContributorNameGenerator.withoutStandardSuffixes();
      }

      return new DefaultReactiveHealthContributorRegistry(nameValidators, nameGenerator.registrar(contributorBeans));
    }

  }

}

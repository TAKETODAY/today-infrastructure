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

package infra.app.health.registry;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import infra.app.health.contributor.HealthContributor;
import infra.app.health.contributor.HealthContributors.Entry;

/**
 * Default {@link HealthContributorRegistry} implementation.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public class DefaultHealthContributorRegistry extends AbstractRegistry<HealthContributor, Entry> implements HealthContributorRegistry {

  /**
   * Create a new empty {@link DefaultHealthContributorRegistry} instance.
   */
  public DefaultHealthContributorRegistry() {
    this(null, null);
  }

  /**
   * Create a new {@link DefaultHealthContributorRegistry} instance.
   *
   * @param nameValidators the name validators to apply
   * @param initialRegistrations callback to setup any initial registrations
   */
  public DefaultHealthContributorRegistry(@Nullable Collection<? extends HealthContributorNameValidator> nameValidators,
          @Nullable Consumer<BiConsumer<String, HealthContributor>> initialRegistrations) {
    super(Entry::new, nameValidators, initialRegistrations);
  }

}

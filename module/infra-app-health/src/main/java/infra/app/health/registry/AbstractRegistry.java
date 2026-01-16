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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Stream;

import infra.lang.Assert;
import infra.util.CollectionUtils;
import infra.util.StringUtils;

/**
 * Internal base class for health registries.
 *
 * @param <C> the contributor type
 * @param <E> the entry type
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
abstract class AbstractRegistry<C, E> {

  private final Collection<HealthContributorNameValidator> nameValidators;

  private final BiFunction<String, C, E> entryAdapter;

  private volatile Map<String, C> contributors;

  private final Object monitor = new Object();

  AbstractRegistry(BiFunction<String, C, E> entryAdapter,
          @Nullable Collection<? extends HealthContributorNameValidator> nameValidators,
          @Nullable Consumer<BiConsumer<String, C>> initialRegistrations) {
    this.nameValidators = nameValidators != null ? List.copyOf(nameValidators) : Collections.emptyList();
    this.entryAdapter = entryAdapter;
    Map<String, C> contributors = new LinkedHashMap<>();
    if (initialRegistrations != null) {
      initialRegistrations.accept((name, contributor) -> registerContributor(contributors, name, contributor));
    }
    this.contributors = Collections.unmodifiableMap(contributors);
  }

  public void registerContributor(String name, C contributor) {
    synchronized(this.monitor) {
      Map<String, C> contributors = new LinkedHashMap<>(this.contributors);
      registerContributor(contributors, name, contributor);
      this.contributors = Collections.unmodifiableMap(contributors);
    }
  }

  private void registerContributor(Map<String, C> contributors, String name, C contributor) {
    Assert.hasText(name, "'name' must not be empty");
    Assert.notNull(contributor, "'contributor' is required");
    assertName(name, contributor);
    if (contributors.containsKey(name)) {
      throw new IllegalStateException("A contributor named \"" + name + "\" has already been registered");
    }
    contributors.put(name, contributor);
  }

  public @Nullable C unregisterContributor(String name) {
    Assert.notNull(name, "'name' is required");
    synchronized(this.monitor) {
      C unregistered = this.contributors.get(name);
      if (unregistered != null) {
        Map<String, C> contributors = new LinkedHashMap<>(this.contributors);
        contributors.remove(name);
        this.contributors = Collections.unmodifiableMap(contributors);
      }
      return unregistered;
    }
  }

  public @Nullable C getContributor(String name) {
    return this.contributors.get(name);
  }

  public Stream<E> stream() {
    return this.contributors.entrySet()
            .stream()
            .map(entry -> this.entryAdapter.apply(entry.getKey(), entry.getValue()));
  }

  private void assertName(String name, C contributor) {
    if (StringUtils.isBlank(name)) {
      throw new IllegalStateException("Name for contributor '%s' must not be empty".formatted(contributor));
    }
    if (CollectionUtils.isNotEmpty(this.nameValidators)) {
      for (var validator : nameValidators) {
        validator.validate(name);
      }
    }
  }

}

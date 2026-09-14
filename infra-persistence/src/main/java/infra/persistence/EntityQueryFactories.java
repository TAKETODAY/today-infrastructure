/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.persistence;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import infra.util.Assert;
import infra.util.InfraStrategies;

/**
 * Registry and aggregator of the {@link EntityQueryFactory factories} used to
 * turn an example object into a {@link QueryStatement} or {@link QueryCondition}.
 *
 * <p>This is the central place to manage {@link EntityQueryFactory} instances:
 * use {@link #addFactory} to register one, or {@link #setFactories} to replace the
 * registered ones. Factories are consulted in order and the first non-null result
 * wins. The lookup order is:
 * <ol>
 *   <li>factories registered via {@link #addFactory}/{@link #setFactories}, in
 *       registration order</li>
 *   <li>factories discovered as {@link EntityQueryFactory} strategies, already
 *       sorted by {@link infra.core.annotation.AnnotationAwareOrderComparator}
 *       (so {@code @Order}/{@link infra.core.Ordered} are honored)</li>
 *   <li>the built-in {@link MapEntityQueryFactory}, handling {@code Map} examples</li>
 *   <li>the built-in {@link DefaultEntityQueryFactory}, as the final fallback</li>
 * </ol>
 *
 * <p>{@link #getFactories()} returns an immutable snapshot of all factories in the
 * above lookup order.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/4/10 17:55
 */
public final class EntityQueryFactories implements EntityQueryFactory {

  private final List<EntityQueryFactory> registeredFactories = new ArrayList<>();

  private final List<EntityQueryFactory> builtInFactories;

  private @Nullable DefaultEntityQueryFactory defaultFactory;

  private List<EntityQueryFactory> factories;

  /**
   * Create a registry with the discovered and built-in factories.
   *
   * @param entityMetadataFactory the metadata factory used by the fallback factory
   */
  public EntityQueryFactories(EntityMetadataFactory entityMetadataFactory) {
    this(entityMetadataFactory, List.of());
  }

  /**
   * Create a registry pre-populated with the given condition property extractors
   * and registered factories.
   *
   * @param entityMetadataFactory the metadata factory used by the fallback factory
   * @param registeredFactories factories to register, consulted before the discovered ones
   */
  public EntityQueryFactories(EntityMetadataFactory entityMetadataFactory, List<EntityQueryFactory> registeredFactories) {
    Assert.notNull(entityMetadataFactory, "EntityMetadataFactory is required");
    this.registeredFactories.addAll(registeredFactories);

    List<EntityQueryFactory> builtIn = new ArrayList<>(4);
    builtIn.addAll(InfraStrategies.find(EntityQueryFactory.class));
    builtIn.add(new MapEntityQueryFactory());
    this.builtInFactories = List.copyOf(builtIn);

    setEntityMetadataFactory(entityMetadataFactory);
  }

  EntityQueryFactories(List<EntityQueryFactory> factories) {
    this.registeredFactories.addAll(factories);
    this.builtInFactories = List.of();
    rebuild();
  }

  /**
   * Register a {@link EntityQueryFactory} consulted before the discovered and
   * built-in factories, in registration order.
   *
   * @param factory the factory to register; must not be null
   */
  public void addFactory(EntityQueryFactory factory) {
    Assert.notNull(factory, "EntityQueryFactory is required");
    registeredFactories.add(factory);
    rebuild();
  }

  /**
   * Replace the registered factories. When {@code null}, the current registrations
   * are cleared. Discovered and built-in factories are kept.
   *
   * @param factories the factories to register, or {@code null} to clear
   */
  public void setFactories(@Nullable List<EntityQueryFactory> factories) {
    registeredFactories.clear();
    if (factories != null) {
      registeredFactories.addAll(factories);
    }
    rebuild();
  }

  /**
   * Return an immutable snapshot of all factories in lookup order: registered,
   * discovered, built-in and finally the fallback factory.
   *
   * @return the factories in the order they are consulted
   */
  public List<EntityQueryFactory> getFactories() {
    return factories;
  }

  /**
   * Update the metadata factory used by the fallback factory.
   */
  void setEntityMetadataFactory(EntityMetadataFactory entityMetadataFactory) {
    Assert.notNull(entityMetadataFactory, "EntityMetadataFactory is required");
    this.defaultFactory = new DefaultEntityQueryFactory(entityMetadataFactory);
    rebuild();
  }

  private void rebuild() {
    List<EntityQueryFactory> list = new ArrayList<>(registeredFactories.size() + builtInFactories.size() + 1);
    list.addAll(registeredFactories);
    list.addAll(builtInFactories);
    if (defaultFactory != null) {
      list.add(defaultFactory);
    }
    this.factories = List.copyOf(list);
  }

  @Override
  public @Nullable QueryStatement createQuery(Object example) {
    Assert.notNull(example, "Example object is required");
    for (EntityQueryFactory factory : factories) {
      QueryStatement query = factory.createQuery(example);
      if (query != null) {
        return query;
      }
    }
    return null;
  }

  @Override
  public @Nullable QueryCondition createCondition(Object example) {
    Assert.notNull(example, "Example object is required");
    for (EntityQueryFactory factory : factories) {
      QueryCondition condition = factory.createCondition(example);
      if (condition != null) {
        return condition;
      }
    }
    return null;
  }

}

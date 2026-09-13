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
import java.util.Collections;
import java.util.List;

import infra.util.Assert;
import infra.util.InfraStrategies;

/**
 * Registry and aggregator of the {@link QueryStatementFactory factories} used to
 * turn an example object into a {@link QueryStatement} or {@link QueryCondition}.
 *
 * <p>This is the central place to manage {@link QueryStatementFactory} instances:
 * use {@link #addFactory} to register one, or {@link #setFactories} to replace the
 * registered ones. Factories are consulted in order and the first non-null result
 * wins. The lookup order is:
 * <ol>
 *   <li>factories registered via {@link #addFactory}/{@link #setFactories}, in
 *       registration order</li>
 *   <li>factories discovered as {@link QueryStatementFactory} strategies, already
 *       sorted by {@link infra.core.annotation.AnnotationAwareOrderComparator}
 *       (so {@code @Order}/{@link infra.core.Ordered} are honored)</li>
 *   <li>the built-in {@link MapQueryStatementFactory}, handling {@code Map} examples</li>
 *   <li>the built-in {@link DefaultQueryStatementFactory}, as the final fallback</li>
 * </ol>
 *
 * <p>This class also manages the {@link ConditionPropertyExtractor extractors}
 * used by the fallback factory: see {@link #addConditionPropertyExtractor} and
 * {@link #setConditionPropertyExtractors}.
 *
 * <p>{@link #getFactories()} returns an immutable snapshot of all factories in the
 * above lookup order.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/4/10 17:55
 */
@SuppressWarnings("rawtypes")
public final class QueryStatementFactories implements QueryStatementFactory {

  private final List<QueryStatementFactory> registeredFactories = new ArrayList<>();

  private final List<QueryStatementFactory> builtInFactories;

  private final List<ConditionPropertyExtractor> extractors = new ArrayList<>();

  private @Nullable DefaultQueryStatementFactory defaultFactory;

  private List<QueryStatementFactory> factories;

  /**
   * Create a registry with the discovered and built-in factories.
   *
   * @param entityMetadataFactory the metadata factory used by the fallback factory
   */
  public QueryStatementFactories(EntityMetadataFactory entityMetadataFactory) {
    this(entityMetadataFactory, List.of());
  }

  /**
   * Create a registry pre-populated with the given condition property extractors.
   *
   * @param entityMetadataFactory the metadata factory used by the fallback factory
   * @param extractors condition property extractors used by the fallback factory
   */
  public QueryStatementFactories(EntityMetadataFactory entityMetadataFactory, List<ConditionPropertyExtractor> extractors) {
    this(entityMetadataFactory, extractors, List.of());
  }

  /**
   * Create a registry pre-populated with the given condition property extractors
   * and registered factories.
   *
   * @param entityMetadataFactory the metadata factory used by the fallback factory
   * @param extractors condition property extractors used by the fallback factory
   * @param registeredFactories factories to register, consulted before the discovered ones
   */
  public QueryStatementFactories(EntityMetadataFactory entityMetadataFactory, List<ConditionPropertyExtractor> extractors,
          List<QueryStatementFactory> registeredFactories) {
    Assert.notNull(entityMetadataFactory, "EntityMetadataFactory is required");
    Assert.notNull(extractors, "ConditionPropertyExtractors is required");
    this.extractors.addAll(extractors);
    this.registeredFactories.addAll(registeredFactories);

    List<QueryStatementFactory> builtIn = new ArrayList<>(4);
    builtIn.addAll(InfraStrategies.find(QueryStatementFactory.class));
    builtIn.add(new MapQueryStatementFactory());
    this.builtInFactories = List.copyOf(builtIn);

    setEntityMetadataFactory(entityMetadataFactory);
  }

  QueryStatementFactories(List<QueryStatementFactory> factories) {
    this.registeredFactories.addAll(factories);
    this.builtInFactories = List.of();
    rebuild();
  }

  /**
   * Register a {@link QueryStatementFactory} consulted before the discovered and
   * built-in factories, in registration order.
   *
   * @param factory the factory to register; must not be null
   */
  public void addFactory(QueryStatementFactory factory) {
    Assert.notNull(factory, "QueryStatementFactory is required");
    registeredFactories.add(factory);
    rebuild();
  }

  /**
   * Replace the registered factories. When {@code null}, the current registrations
   * are cleared. Discovered and built-in factories are kept.
   *
   * @param factories the factories to register, or {@code null} to clear
   */
  public void setFactories(@Nullable List<QueryStatementFactory> factories) {
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
  public List<QueryStatementFactory> getFactories() {
    return factories;
  }

  /**
   * Add a {@link ConditionPropertyExtractor} used to extract condition values from
   * example objects, consulted in registration order by the fallback factory.
   *
   * @param extractor the extractor to add; must not be null
   */
  public void addConditionPropertyExtractor(ConditionPropertyExtractor extractor) {
    Assert.notNull(extractor, "ConditionPropertyExtractor is required");
    this.extractors.add(extractor);
  }

  /**
   * Replace the condition property extractors. When {@code null}, the current
   * extractors are cleared.
   *
   * @param extractors the extractors to set, or {@code null} to clear
   */
  public void setConditionPropertyExtractors(@Nullable List<ConditionPropertyExtractor> extractors) {
    this.extractors.clear();
    if (extractors != null) {
      this.extractors.addAll(extractors);
    }
  }

  /**
   * Return an unmodifiable live view of the condition property extractors.
   *
   * @return the current condition property extractors
   */
  public List<ConditionPropertyExtractor> getConditionPropertyExtractors() {
    return Collections.unmodifiableList(extractors);
  }

  /**
   * Update the metadata factory used by the fallback factory.
   */
  void setEntityMetadataFactory(EntityMetadataFactory entityMetadataFactory) {
    Assert.notNull(entityMetadataFactory, "EntityMetadataFactory is required");
    this.defaultFactory = new DefaultQueryStatementFactory(entityMetadataFactory, extractors);
    rebuild();
  }

  private void rebuild() {
    List<QueryStatementFactory> list = new ArrayList<>(registeredFactories.size() + builtInFactories.size() + 1);
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
    for (QueryStatementFactory factory : factories) {
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
    for (QueryStatementFactory factory : factories) {
      QueryCondition condition = factory.createCondition(example);
      if (condition != null) {
        return condition;
      }
    }
    return null;
  }

}

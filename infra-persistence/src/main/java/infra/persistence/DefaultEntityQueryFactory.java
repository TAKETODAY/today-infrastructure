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

import java.util.ArrayList;
import java.util.List;

import infra.persistence.support.DefaultConditionStrategy;
import infra.persistence.support.FuzzyQueryConditionStrategy;
import infra.persistence.support.MultiValueConditionStrategy;
import infra.persistence.support.WhereAnnotationConditionStrategy;
import infra.util.Assert;
import infra.util.InfraStrategies;

/**
 * Default {@link EntityQueryFactory} implementation.
 *
 * <p>It turns an example object into its query representation by resolving the
 * {@link EntityMetadata} through the configured {@link EntityMetadataFactory} and
 * applying the {@link PropertyConditionStrategy condition strategies} in order,
 * stopping at the first one that produces a condition for a property. The strategies
 * are the built-in ones plus any discovered via {@link InfraStrategies}; more can be
 * appended with {@link #addStrategy(PropertyConditionStrategy)}.
 *
 * <p>Property values are normalized through the configured {@link ValueNormalizer
 * value normalizers} before a strategy turns them into a condition; normalizers are
 * applied in the order they were added and end with the shared
 * {@link ValueNormalizer#DEFAULT}. More can be appended with
 * {@link #addNormalizer(ValueNormalizer)}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/4/10 16:53
 */
final class DefaultEntityQueryFactory implements EntityQueryFactory {

  private final List<PropertyConditionStrategy> strategies = new ArrayList<>();

  private final PropertyConditionStrategy fallbackStrategy = new DefaultConditionStrategy();

  private final List<ValueNormalizer> valueNormalizers = new ArrayList<>();

  private final ValueNormalizer fallbackNormalizer = ValueNormalizer.DEFAULT;

  private List<PropertyConditionStrategy> resolvedStrategies;

  private List<ValueNormalizer> resolvedValueNormalizers;

  private EntityMetadataFactory factory;

  public DefaultEntityQueryFactory(EntityMetadataFactory factory) {
    this.factory = factory;
    this.strategies.addAll(InfraStrategies.find(PropertyConditionStrategy.class));
    this.strategies.add(new WhereAnnotationConditionStrategy());
    this.strategies.add(new FuzzyQueryConditionStrategy());
    this.strategies.add(new MultiValueConditionStrategy());
    this.resolvedStrategies = resolveStrategies();
    this.resolvedValueNormalizers = resolveValueNormalizers();
  }

  /**
   * Add a {@link PropertyConditionStrategy}, consulted before the fallback
   * {@link DefaultConditionStrategy}. Strategies are consulted in the order they are added.
   *
   * @param strategy the strategy to add; must not be {@code null}
   */
  public void addStrategy(PropertyConditionStrategy strategy) {
    Assert.notNull(strategy, "PropertyConditionStrategy is required");
    this.strategies.add(strategy);
    this.resolvedStrategies = resolveStrategies();
  }

  /**
   * Return an unmodifiable view of the condition strategies in evaluation order,
   * ending with the fallback {@link DefaultConditionStrategy}.
   *
   * @return the condition strategies
   */
  public List<PropertyConditionStrategy> getStrategies() {
    return resolvedStrategies;
  }

  /**
   * Add a {@link ValueNormalizer}, applied before the fallback
   * {@link ValueNormalizer#DEFAULT}. Normalizers are applied in the order they are
   * added.
   *
   * @param normalizer the normalizer to add; must not be {@code null}
   */
  public void addNormalizer(ValueNormalizer normalizer) {
    Assert.notNull(normalizer, "ValueNormalizer is required");
    this.valueNormalizers.add(normalizer);
    this.resolvedValueNormalizers = resolveValueNormalizers();
  }

  /**
   * Return an unmodifiable view of the value normalizers in application order,
   * ending with the fallback {@link ValueNormalizer#DEFAULT}.
   *
   * @return the value normalizers
   */
  public List<ValueNormalizer> getValueNormalizers() {
    return resolvedValueNormalizers;
  }

  private List<ValueNormalizer> resolveValueNormalizers() {
    List<ValueNormalizer> all = new ArrayList<>(valueNormalizers.size() + 1);
    all.addAll(valueNormalizers);
    all.add(fallbackNormalizer);
    return List.copyOf(all);
  }

  void setEntityMetadataFactory(EntityMetadataFactory factory) {
    this.factory = factory;
  }

  private List<PropertyConditionStrategy> resolveStrategies() {
    List<PropertyConditionStrategy> all = new ArrayList<>(strategies.size() + 1);
    all.addAll(strategies);
    all.add(fallbackStrategy);
    return List.copyOf(all);
  }

  @Override
  public QueryStatement createQuery(Object example) {
    return new ExampleQuery(factory, example, resolvedStrategies, resolvedValueNormalizers);
  }

  @Override
  public QueryCondition createCondition(Object example) {
    return new ExampleQuery(factory, example, resolvedStrategies, resolvedValueNormalizers);
  }

}

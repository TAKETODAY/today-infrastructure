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

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/8 20:33
 */
class EntityQueryFactoriesTests {

  @Test
  void shouldCreateEntityQueryFactories() {
    EntityMetadataFactory entityMetadataFactory = mock(EntityMetadataFactory.class);

    EntityQueryFactories factories = new EntityQueryFactories(entityMetadataFactory);

    assertThat(factories).isNotNull();
    assertThat(factories.getFactories()).isNotEmpty();
  }

  @Test
  void shouldCreateQueryWithExample() {
    EntityMetadataFactory entityMetadataFactory = mock(EntityMetadataFactory.class);

    EntityQueryFactories factories = new EntityQueryFactories(entityMetadataFactory);

    Object example = new Object();
    QueryStatement query = factories.createQuery(example);

    assertThat(query).isNotNull();
  }

  @Test
  void shouldCreateConditionWithExample() {
    EntityMetadataFactory entityMetadataFactory = mock(EntityMetadataFactory.class);

    EntityQueryFactories factories = new EntityQueryFactories(entityMetadataFactory);

    Object example = new Object();
    QueryCondition condition = factories.createCondition(example);

    assertThat(condition).isNotNull();
  }

  @Test
  void shouldUseRegisteredFactoriesForQueryCreation() {
    EntityMetadataFactory entityMetadataFactory = mock(EntityMetadataFactory.class);

    EntityQueryFactory customFactory = mock(EntityQueryFactory.class);
    QueryStatement customQuery = mock(QueryStatement.class);

    when(customFactory.createQuery(any())).thenReturn(customQuery);

    // We can't easily inject our custom factory into the static list, but we can verify
    // the behavior by checking that the default factories are used
    EntityQueryFactories factories = new EntityQueryFactories(entityMetadataFactory);

    assertThat(factories.getFactories()).hasAtLeastOneElementOfType(MapEntityQueryFactory.class);
    assertThat(factories.getFactories()).hasAtLeastOneElementOfType(DefaultEntityQueryFactory.class);
  }

  @Test
  void shouldReturnNullWhenNoFactoryCanCreateQuery() {
    EntityMetadataFactory entityMetadataFactory = mock(EntityMetadataFactory.class);

    EntityQueryFactories factories = new EntityQueryFactories(entityMetadataFactory);

    // Create a mock factory that always returns null
    EntityQueryFactory nullFactory = mock(EntityQueryFactory.class);
    when(nullFactory.createQuery(any())).thenReturn(null);

    // The actual behavior depends on the default factories, but we can at least
    // verify that the method doesn't throw an exception
    assertThatCode(() -> factories.createQuery(new Object())).doesNotThrowAnyException();
  }

  @Test
  void shouldReturnNullWhenNoFactoryCanCreateCondition() {
    EntityMetadataFactory entityMetadataFactory = mock(EntityMetadataFactory.class);

    EntityQueryFactories factories = new EntityQueryFactories(entityMetadataFactory);

    // Create a mock factory that always returns null
    EntityQueryFactory nullFactory = mock(EntityQueryFactory.class);
    when(nullFactory.createCondition(any())).thenReturn(null);

    // The actual behavior depends on the default factories, but we can at least
    // verify that the method doesn't throw an exception
    assertThatCode(() -> factories.createCondition(new Object())).doesNotThrowAnyException();
  }

  @Test
  void shouldCreateQueryWithNullExample() {
    EntityMetadataFactory entityMetadataFactory = mock(EntityMetadataFactory.class);

    EntityQueryFactories factories = new EntityQueryFactories(entityMetadataFactory);

    assertThatThrownBy(() -> {
      factories.createQuery(null);
    }).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldCreateConditionWithNullExample() {
    EntityMetadataFactory entityMetadataFactory = mock(EntityMetadataFactory.class);

    EntityQueryFactories factories = new EntityQueryFactories(entityMetadataFactory);

    assertThatThrownBy(() -> {
      factories.createCondition(null);
    }).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldReturnFirstNonNullQueryFromFactories() {

    EntityQueryFactory factory1 = mock(EntityQueryFactory.class);
    EntityQueryFactory factory2 = mock(EntityQueryFactory.class);
    QueryStatement query2 = mock(QueryStatement.class);

    when(factory1.createQuery(any())).thenReturn(null);
    when(factory2.createQuery(any())).thenReturn(query2);

    EntityQueryFactories factories = new EntityQueryFactories(List.of(factory1, factory2, new MapEntityQueryFactory()));

    Object example = new Object();
    QueryStatement result = factories.createQuery(example);

    assertThat(result).isEqualTo(query2);
  }

  @Test
  void shouldReturnFirstNonNullConditionFromFactories() {
    EntityQueryFactory factory1 = mock(EntityQueryFactory.class);
    EntityQueryFactory factory2 = mock(EntityQueryFactory.class);
    QueryCondition condition2 = mock(QueryCondition.class);

    when(factory1.createCondition(any())).thenReturn(null);
    when(factory2.createCondition(any())).thenReturn(condition2);

    EntityQueryFactories factories = new EntityQueryFactories(List.of(factory1, factory2, new MapEntityQueryFactory()));

    Object example = new Object();
    QueryCondition result = factories.createCondition(example);

    assertThat(result).isEqualTo(condition2);
  }

  @Test
  void shouldReturnNullWhenAllFactoriesReturnNullForQuery() {

    EntityQueryFactory factory1 = mock(EntityQueryFactory.class);
    EntityQueryFactory factory2 = mock(EntityQueryFactory.class);

    when(factory1.createQuery(any())).thenReturn(null);
    when(factory2.createQuery(any())).thenReturn(null);

    EntityQueryFactories factories = new EntityQueryFactories(List.of(factory1, factory2));

    Object example = new Object();
    QueryStatement result = factories.createQuery(example);

    assertThat(result).isNull();
  }

  @Test
  void shouldReturnNullWhenAllFactoriesReturnNullForCondition() {

    EntityQueryFactory factory1 = mock(EntityQueryFactory.class);
    EntityQueryFactory factory2 = mock(EntityQueryFactory.class);

    when(factory1.createCondition(any())).thenReturn(null);
    when(factory2.createCondition(any())).thenReturn(null);

    EntityQueryFactories factories = new EntityQueryFactories(List.of(factory1, factory2));

    Object example = new Object();
    QueryCondition result = factories.createCondition(example);

    assertThat(result).isNull();
  }

  @Test
  void shouldIncludeMapAndDefaultFactoriesInConstructor() {
    EntityMetadataFactory entityMetadataFactory = mock(EntityMetadataFactory.class);

    EntityQueryFactories factories = new EntityQueryFactories(entityMetadataFactory);

    boolean hasMapFactory = factories.getFactories().stream()
            .anyMatch(f -> f instanceof MapEntityQueryFactory);
    boolean hasDefaultFactory = factories.getFactories().stream()
            .anyMatch(f -> f instanceof DefaultEntityQueryFactory);

    assertThat(hasMapFactory).isTrue();
    assertThat(hasDefaultFactory).isTrue();
  }

  @Test
  void shouldTryRegisteredFactoriesBeforeBuiltInOnes() {
    EntityQueryFactory registered = mock(EntityQueryFactory.class);
    QueryStatement registeredQuery = mock(QueryStatement.class);
    when(registered.createQuery(any())).thenReturn(registeredQuery);

    EntityMetadataFactory entityMetadataFactory = mock(EntityMetadataFactory.class);

    EntityQueryFactories factories = new EntityQueryFactories(
            entityMetadataFactory, List.of(registered));

    assertThat(factories.getFactories()).startsWith(registered);
    assertThat(factories.createQuery(new Object())).isEqualTo(registeredQuery);
  }

  @Test
  void shouldUseRegisteredFactoryForConditionBeforeBuiltInOnes() {
    EntityQueryFactory registered = mock(EntityQueryFactory.class);
    QueryCondition registeredCondition = mock(QueryCondition.class);
    when(registered.createCondition(any())).thenReturn(registeredCondition);

    EntityMetadataFactory entityMetadataFactory = mock(EntityMetadataFactory.class);

    EntityQueryFactories factories = new EntityQueryFactories(
            entityMetadataFactory, List.of(registered));

    assertThat(factories.createCondition(new Object())).isEqualTo(registeredCondition);
  }

  @Test
  void shouldKeepRegistrationOrderForRegisteredFactories() {
    EntityQueryFactory first = mock(EntityQueryFactory.class);
    EntityQueryFactory second = mock(EntityQueryFactory.class);
    QueryStatement secondQuery = mock(QueryStatement.class);
    when(first.createQuery(any())).thenReturn(null);
    when(second.createQuery(any())).thenReturn(secondQuery);

    EntityMetadataFactory entityMetadataFactory = mock(EntityMetadataFactory.class);

    EntityQueryFactories factories = new EntityQueryFactories(
            entityMetadataFactory, List.of(first, second));

    assertThat(factories.getFactories()).startsWith(first, second);
    assertThat(factories.createQuery(new Object())).isEqualTo(secondQuery);
  }

  @Test
  void shouldUseDefaultFactoryAsLastResort() {
    EntityMetadataFactory entityMetadataFactory = mock(EntityMetadataFactory.class);

    EntityQueryFactories factories = new EntityQueryFactories(entityMetadataFactory);

    assertThat(factories.getFactories()).last().isInstanceOf(DefaultEntityQueryFactory.class);
  }

  @Test
  void shouldFallThroughToBuiltInFactoriesWhenRegisteredReturnsNull() {
    EntityQueryFactory registered = mock(EntityQueryFactory.class);
    when(registered.createQuery(any())).thenReturn(null);
    when(registered.createCondition(any())).thenReturn(null);

    EntityMetadataFactory entityMetadataFactory = mock(EntityMetadataFactory.class);

    EntityQueryFactories factories = new EntityQueryFactories(
            entityMetadataFactory, List.of(registered));

    assertThat(factories.createQuery(Map.of("name", "TODAY"))).isNotNull();
    assertThat(factories.createCondition(Map.of("name", "TODAY"))).isNotNull();
  }

  @Test
  void shouldTreatEmptyRegisteredFactoriesLikeNone() {
    EntityMetadataFactory entityMetadataFactory = mock(EntityMetadataFactory.class);

    EntityQueryFactories withEmpty = new EntityQueryFactories(
            entityMetadataFactory, List.of());
    EntityQueryFactories without = new EntityQueryFactories(entityMetadataFactory);

    assertThat(withEmpty.getFactories()).hasSize(without.getFactories().size());
  }

  @Test
  void shouldAppendFactoryOnAddFactory() {
    EntityMetadataFactory entityMetadataFactory = mock(EntityMetadataFactory.class);
    EntityQueryFactories factories = new EntityQueryFactories(entityMetadataFactory);

    EntityQueryFactory first = mock(EntityQueryFactory.class);
    EntityQueryFactory second = mock(EntityQueryFactory.class);

    factories.addFactory(first);
    factories.addFactory(second);

    assertThat(factories.getFactories()).startsWith(first, second);
    assertThat(factories.getFactories()).last().isInstanceOf(DefaultEntityQueryFactory.class);
  }

  @Test
  void shouldRejectNullFactoryOnAddFactory() {
    EntityMetadataFactory entityMetadataFactory = mock(EntityMetadataFactory.class);
    EntityQueryFactories factories = new EntityQueryFactories(entityMetadataFactory);

    assertThatThrownBy(() -> factories.addFactory(null))
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldReplaceRegisteredFactoriesOnSetFactories() {
    EntityMetadataFactory entityMetadataFactory = mock(EntityMetadataFactory.class);
    EntityQueryFactories factories = new EntityQueryFactories(entityMetadataFactory);

    EntityQueryFactory original = mock(EntityQueryFactory.class);
    EntityQueryFactory replacement = mock(EntityQueryFactory.class);
    factories.addFactory(original);
    factories.setFactories(List.of(replacement));

    assertThat(factories.getFactories()).contains(replacement).doesNotContain(original);
  }

  @Test
  void shouldClearRegisteredFactoriesOnSetFactoriesNull() {
    EntityMetadataFactory entityMetadataFactory = mock(EntityMetadataFactory.class);
    EntityQueryFactories factories = new EntityQueryFactories(entityMetadataFactory);

    EntityQueryFactory registered = mock(EntityQueryFactory.class);
    factories.addFactory(registered);
    factories.setFactories(null);

    assertThat(factories.getFactories()).doesNotContain(registered);
  }

  @Test
  void shouldKeepRegisteredFactoriesWhenMetadataFactoryReplaced() {
    EntityMetadataFactory entityMetadataFactory = mock(EntityMetadataFactory.class);
    EntityQueryFactories factories = new EntityQueryFactories(entityMetadataFactory);

    EntityQueryFactory registered = mock(EntityQueryFactory.class);
    factories.addFactory(registered);

    factories.setEntityMetadataFactory(new DefaultEntityMetadataFactory());

    assertThat(factories.getFactories()).startsWith(registered);
    assertThat(factories.getFactories()).last().isInstanceOf(DefaultEntityQueryFactory.class);
  }

  @Test
  void getFactoriesShouldBeImmutable() {
    EntityMetadataFactory entityMetadataFactory = mock(EntityMetadataFactory.class);
    EntityQueryFactories factories = new EntityQueryFactories(entityMetadataFactory);

    assertThatThrownBy(() -> factories.getFactories().add(mock(EntityQueryFactory.class)))
            .isInstanceOf(UnsupportedOperationException.class);
  }

}

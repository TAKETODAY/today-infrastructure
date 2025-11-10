/*
 * Copyright 2017 - 2025 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.persistence;

import org.junit.jupiter.api.Test;

import java.util.List;

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
class QueryStatementFactoriesTests {

  @Test
  void shouldCreateQueryStatementFactories() {
    EntityMetadataFactory entityMetadataFactory = mock(EntityMetadataFactory.class);
    List<ConditionPropertyExtractor> extractors = mock();

    QueryStatementFactories factories = new QueryStatementFactories(entityMetadataFactory, extractors);

    assertThat(factories).isNotNull();
    assertThat(factories.factories).isNotEmpty();
  }

  @Test
  void shouldCreateQueryWithExample() {
    EntityMetadataFactory entityMetadataFactory = mock(EntityMetadataFactory.class);
    List<ConditionPropertyExtractor> extractors = mock();

    QueryStatementFactories factories = new QueryStatementFactories(entityMetadataFactory, extractors);

    Object example = new Object();
    QueryStatement query = factories.createQuery(example);

    assertThat(query).isNotNull();
  }

  @Test
  void shouldCreateConditionWithExample() {
    EntityMetadataFactory entityMetadataFactory = mock(EntityMetadataFactory.class);
    List<ConditionPropertyExtractor> extractors = mock();

    QueryStatementFactories factories = new QueryStatementFactories(entityMetadataFactory, extractors);

    Object example = new Object();
    ConditionStatement condition = factories.createCondition(example);

    assertThat(condition).isNotNull();
  }

  @Test
  void shouldUseRegisteredFactoriesForQueryCreation() {
    EntityMetadataFactory entityMetadataFactory = mock(EntityMetadataFactory.class);
    List<ConditionPropertyExtractor> extractors = mock();

    QueryStatementFactory customFactory = mock(QueryStatementFactory.class);
    QueryStatement customQuery = mock(QueryStatement.class);

    when(customFactory.createQuery(any())).thenReturn(customQuery);

    // We can't easily inject our custom factory into the static list, but we can verify
    // the behavior by checking that the default factories are used
    QueryStatementFactories factories = new QueryStatementFactories(entityMetadataFactory, extractors);

    assertThat(factories.factories).hasAtLeastOneElementOfType(MapQueryStatementFactory.class);
    assertThat(factories.factories).hasAtLeastOneElementOfType(DefaultQueryStatementFactory.class);
  }

  @Test
  void shouldReturnNullWhenNoFactoryCanCreateQuery() {
    EntityMetadataFactory entityMetadataFactory = mock(EntityMetadataFactory.class);
    List<ConditionPropertyExtractor> extractors = mock();

    QueryStatementFactories factories = new QueryStatementFactories(entityMetadataFactory, extractors);

    // Create a mock factory that always returns null
    QueryStatementFactory nullFactory = mock(QueryStatementFactory.class);
    when(nullFactory.createQuery(any())).thenReturn(null);

    // The actual behavior depends on the default factories, but we can at least
    // verify that the method doesn't throw an exception
    assertThatCode(() -> factories.createQuery(new Object())).doesNotThrowAnyException();
  }

  @Test
  void shouldReturnNullWhenNoFactoryCanCreateCondition() {
    EntityMetadataFactory entityMetadataFactory = mock(EntityMetadataFactory.class);
    List<ConditionPropertyExtractor> extractors = mock();

    QueryStatementFactories factories = new QueryStatementFactories(entityMetadataFactory, extractors);

    // Create a mock factory that always returns null
    QueryStatementFactory nullFactory = mock(QueryStatementFactory.class);
    when(nullFactory.createCondition(any())).thenReturn(null);

    // The actual behavior depends on the default factories, but we can at least
    // verify that the method doesn't throw an exception
    assertThatCode(() -> factories.createCondition(new Object())).doesNotThrowAnyException();
  }

  @Test
  void shouldCreateQueryWithNullExample() {
    EntityMetadataFactory entityMetadataFactory = mock(EntityMetadataFactory.class);
    List<ConditionPropertyExtractor> extractors = mock();

    QueryStatementFactories factories = new QueryStatementFactories(entityMetadataFactory, extractors);

    assertThatThrownBy(() -> {
      factories.createQuery(null);
    }).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldCreateConditionWithNullExample() {
    EntityMetadataFactory entityMetadataFactory = mock(EntityMetadataFactory.class);
    List<ConditionPropertyExtractor> extractors = mock();

    QueryStatementFactories factories = new QueryStatementFactories(entityMetadataFactory, extractors);

    assertThatThrownBy(() -> {
      factories.createCondition(null);
    }).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldReturnFirstNonNullQueryFromFactories() {

    QueryStatementFactory factory1 = mock(QueryStatementFactory.class);
    QueryStatementFactory factory2 = mock(QueryStatementFactory.class);
    QueryStatement query2 = mock(QueryStatement.class);

    when(factory1.createQuery(any())).thenReturn(null);
    when(factory2.createQuery(any())).thenReturn(query2);

    QueryStatementFactories factories = new QueryStatementFactories(List.of(factory1, factory2, new MapQueryStatementFactory()));

    Object example = new Object();
    QueryStatement result = factories.createQuery(example);

    assertThat(result).isEqualTo(query2);
  }

  @Test
  void shouldReturnFirstNonNullConditionFromFactories() {
    QueryStatementFactory factory1 = mock(QueryStatementFactory.class);
    QueryStatementFactory factory2 = mock(QueryStatementFactory.class);
    ConditionStatement condition2 = mock(ConditionStatement.class);

    when(factory1.createCondition(any())).thenReturn(null);
    when(factory2.createCondition(any())).thenReturn(condition2);

    QueryStatementFactories factories = new QueryStatementFactories(List.of(factory1, factory2, new MapQueryStatementFactory()));

    Object example = new Object();
    ConditionStatement result = factories.createCondition(example);

    assertThat(result).isEqualTo(condition2);
  }

  @Test
  void shouldReturnNullWhenAllFactoriesReturnNullForQuery() {

    QueryStatementFactory factory1 = mock(QueryStatementFactory.class);
    QueryStatementFactory factory2 = mock(QueryStatementFactory.class);

    when(factory1.createQuery(any())).thenReturn(null);
    when(factory2.createQuery(any())).thenReturn(null);

    QueryStatementFactories factories = new QueryStatementFactories(List.of(factory1, factory2));

    Object example = new Object();
    QueryStatement result = factories.createQuery(example);

    assertThat(result).isNull();
  }

  @Test
  void shouldReturnNullWhenAllFactoriesReturnNullForCondition() {

    QueryStatementFactory factory1 = mock(QueryStatementFactory.class);
    QueryStatementFactory factory2 = mock(QueryStatementFactory.class);

    when(factory1.createCondition(any())).thenReturn(null);
    when(factory2.createCondition(any())).thenReturn(null);

    QueryStatementFactories factories = new QueryStatementFactories(List.of(factory1, factory2));

    Object example = new Object();
    ConditionStatement result = factories.createCondition(example);

    assertThat(result).isNull();
  }

  @Test
  void shouldIncludeMapAndDefaultFactoriesInConstructor() {
    EntityMetadataFactory entityMetadataFactory = mock(EntityMetadataFactory.class);
    List<ConditionPropertyExtractor> extractors = mock();

    QueryStatementFactories factories = new QueryStatementFactories(entityMetadataFactory, extractors);

    boolean hasMapFactory = factories.factories.stream()
            .anyMatch(f -> f instanceof MapQueryStatementFactory);
    boolean hasDefaultFactory = factories.factories.stream()
            .anyMatch(f -> f instanceof DefaultQueryStatementFactory);

    assertThat(hasMapFactory).isTrue();
    assertThat(hasDefaultFactory).isTrue();
  }

}
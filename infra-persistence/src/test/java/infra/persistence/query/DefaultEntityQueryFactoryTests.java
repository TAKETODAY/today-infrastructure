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

package infra.persistence.query;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import infra.persistence.DefaultEntityMetadataFactory;
import infra.persistence.annotation.Trim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
class DefaultEntityQueryFactoryTests {

  final DefaultEntityMetadataFactory metadataFactory = new DefaultEntityMetadataFactory();

  final DefaultEntityQueryFactory factory = new DefaultEntityQueryFactory(metadataFactory);

  @Test
  void shouldExposeBuiltInStrategies() {
    assertThat(factory.getStrategies())
            .hasAtLeastOneElementOfType(WhereAnnotationConditionStrategy.class)
            .hasAtLeastOneElementOfType(FuzzyQueryConditionStrategy.class);
  }

  @Test
  void shouldAddStrategyBeforeFallback() {
    PropertyConditionStrategy strategy = mock(PropertyConditionStrategy.class);

    factory.addStrategy(strategy);

    assertThat(factory.getStrategies()).contains(strategy);
    assertThat(factory.getStrategies()).isNotEmpty();
  }

  @Test
  void shouldRejectNullStrategy() {
    assertThatThrownBy(() -> factory.addStrategy(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("PropertyConditionStrategy is required");
  }

  @Test
  void shouldReturnImmutableStrategies() {
    assertThatThrownBy(() -> factory.getStrategies().add(mock(PropertyConditionStrategy.class)))
            .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void shouldExposeDefaultValueNormalizer() {
    assertThat(factory.getValueNormalizers()).containsExactly(ValueNormalizer.DEFAULT);
  }

  @Test
  void shouldAddNormalizerBeforeDefault() {
    ValueNormalizer normalizer = mock(ValueNormalizer.class);

    factory.addNormalizer(normalizer);

    assertThat(factory.getValueNormalizers()).contains(normalizer);
    assertThat(factory.getValueNormalizers()).last().isSameAs(ValueNormalizer.DEFAULT);
  }

  @Test
  void shouldRejectNullNormalizer() {
    assertThatThrownBy(() -> factory.addNormalizer(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("ValueNormalizer is required");
  }

  @Test
  void shouldReturnImmutableValueNormalizers() {
    assertThatThrownBy(() -> factory.getValueNormalizers().add(mock(ValueNormalizer.class)))
            .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void shouldChainNormalizersThroughCreatedQuery() {
    factory.addNormalizer((property, value) ->
            value instanceof String string ? string.toUpperCase(Locale.ROOT) : value);

    QueryStatement query = factory.createQuery(new TrimModel());

    Object normalized = ((ValueNormalizer) query).normalize(
            metadataFactory.getEntityMetadata(TrimModel.class).findProperty("name"), "  today  ");

    assertThat(normalized).isEqualTo("TODAY");
  }

  static class TrimModel {

    @Trim
    public String name;

  }

}

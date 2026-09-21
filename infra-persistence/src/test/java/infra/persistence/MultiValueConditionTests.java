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

import infra.jdbc.model.UserModel;
import infra.persistence.annotation.Between;
import infra.persistence.annotation.EntityRef;
import infra.persistence.annotation.In;
import infra.persistence.annotation.Where;
import infra.persistence.platform.Platform;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies {@link Between @Between} and {@link In @In} example conditions render
 * and bind multi-value predicates.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
class MultiValueConditionTests {

  private final DefaultEntityMetadataFactory metadataFactory = new DefaultEntityMetadataFactory();

  private final List<PropertyConditionStrategy> strategies =
          new DefaultEntityQueryFactory(metadataFactory).getStrategies();

  private String renderWhere(Object example) {
    EntityMetadata metadata = metadataFactory.getEntityMetadata(example.getClass());
    StringBuilder sql = new StringBuilder();
    new ExampleQuery(metadataFactory, example, strategies)
            .appendWhereClause(Platform.mysql(), metadata, sql);
    return sql.toString();
  }

  @Test
  void between_rendersTwoPlaceholders() {
    BetweenQuery query = new BetweenQuery();
    query.age = Range.of(18, 30);
    query.name = "TODAY";

    assertThat(renderWhere(query))
            .isEqualTo(" WHERE age BETWEEN ? AND ? AND name = ?");
  }

  @Test
  void between_nullBound_isSkipped() {
    BetweenQuery query = new BetweenQuery();
    query.age = Range.of(null, 30);

    assertThat(renderWhere(query)).isEqualTo("");
  }

  @Test
  void between_withArrayValue() {
    BetweenArrayQuery query = new BetweenArrayQuery();
    query.age = new Integer[] { 18, 30 };

    assertThat(renderWhere(query))
            .isEqualTo(" WHERE age BETWEEN ? AND ?");
  }

  @Test
  void between_withCollectionValue() {
    BetweenCollectionQuery query = new BetweenCollectionQuery();
    query.age = List.of(18, 30);

    assertThat(renderWhere(query))
            .isEqualTo(" WHERE age BETWEEN ? AND ?");
  }

  @Test
  void between_wrongSizeArray_isSkipped() {
    BetweenArrayQuery query = new BetweenArrayQuery();
    query.age = new Integer[] { 18, 30, 40 };

    assertThat(renderWhere(query)).isEqualTo("");
  }

  @Test
  void in_rendersOnePlaceholderPerElement() {
    InQuery query = new InQuery();
    query.statuses = List.of(1, 2, 3);

    assertThat(renderWhere(query))
            .isEqualTo(" WHERE statuses IN (?, ?, ?)");
  }

  @Test
  void in_empty_isSkipped() {
    InQuery query = new InQuery();
    query.statuses = List.of();

    assertThat(renderWhere(query)).isEqualTo("");
  }

  @Test
  void in_withArrayValue() {
    InArrayQuery query = new InArrayQuery();
    query.statuses = new Integer[] { 1, 2 };

    assertThat(renderWhere(query))
            .isEqualTo(" WHERE statuses IN (?, ?)");
  }

  @Test
  void in_andBetween_mixed() {
    MixedQuery query = new MixedQuery();
    query.ids = List.of(1, 2);
    query.age = Range.of(18, 30);

    assertThat(renderWhere(query))
            .isEqualTo(" WHERE ids IN (?, ?) AND age BETWEEN ? AND ?");
  }

  @EntityRef(UserModel.class)
  static class BetweenQuery {

    @Between
    Range age;

    @Where("name = ?")
    String name;

  }

  @EntityRef(UserModel.class)
  static class BetweenArrayQuery {

    @Between
    Integer[] age;

  }

  @EntityRef(UserModel.class)
  static class BetweenCollectionQuery {

    @Between
    List<Integer> age;

  }

  @EntityRef(UserModel.class)
  static class InQuery {

    @In
    List<Integer> statuses;

  }

  @EntityRef(UserModel.class)
  static class InArrayQuery {

    @In
    Integer[] statuses;

  }

  @EntityRef(UserModel.class)
  static class MixedQuery {

    @In
    List<Integer> ids;

    @Between
    Range age;

  }

}
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

import java.util.List;

import infra.jdbc.model.UserModel;
import infra.persistence.DefaultEntityMetadataFactory;
import infra.persistence.EntityMetadata;
import infra.persistence.annotation.Between;
import infra.persistence.annotation.Column;
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

  private final EntityQueryFactories queryFactories = new EntityQueryFactories(new DefaultEntityMetadataFactory());

  private String renderWhere(Object example) {
    QueryCondition condition = queryFactories.createCondition(example);
    EntityMetadata metadata = new DefaultEntityMetadataFactory().getEntityMetadata(example.getClass());
    StringBuilder sql = new StringBuilder();
    condition.appendWhereClause(Platform.mysql(), metadata, sql);
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

    assertThat(renderWhere(query)).isEqualTo(" WHERE age = ?");
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

    assertThat(renderWhere(query)).isEqualTo(" WHERE age = ?");
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

    assertThat(renderWhere(query)).isEqualTo(" WHERE statuses = ?");
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

  @Test
  void between_columnAlias_overridesMappedColumn() {
    ColumnAliasQuery query = new ColumnAliasQuery();
    query.age = Range.of(18, 30);
    query.name = "TODAY";

    // column() is an alias for @Column.name, resolved through the meta-annotation
    assertThat(renderWhere(query))
            .isEqualTo(" WHERE user_age BETWEEN ? AND ? AND name = ?");
  }

  @Test
  void in_columnAlias_overridesMappedColumn() {
    InColumnAliasQuery query = new InColumnAliasQuery();
    query.statuses = List.of(1, 2, 3);

    assertThat(renderWhere(query))
            .isEqualTo(" WHERE status_code IN (?, ?, ?)");
  }

  @Test
  void between_withLoneColumnAnnotation_stillWorks() {
    // the user doesn't know @Between("...") and uses @Column("...") instead
    between_columnOnlyBefore();
    between_columnOnlyAfter();
  }

  private void between_columnOnlyBefore() {
    ColumnOnlyBeforeQuery query = new ColumnOnlyBeforeQuery();
    query.age = Range.of(18, 30);

    assertThat(renderWhere(query))
            .isEqualTo(" WHERE user_age BETWEEN ? AND ?");
  }

  private void between_columnOnlyAfter() {
    ColumnOnlyAfterQuery query = new ColumnOnlyAfterQuery();
    query.age = Range.of(18, 30);

    assertThat(renderWhere(query))
            .isEqualTo(" WHERE user_age BETWEEN ? AND ?");
  }

  @Test
  void in_withLoneColumnAnnotation_stillWorks() {
    ColumnOnlyInQuery query = new ColumnOnlyInQuery();
    query.statuses = List.of(1, 2);

    assertThat(renderWhere(query))
            .isEqualTo(" WHERE status_code IN (?, ?)");
  }

  @EntityRef(UserModel.class)
  static class BetweenQuery {

    @Between
    Range age;

    @Where("name = ?")
    String name;

  }

  @EntityRef(UserModel.class)
  static class ColumnAliasQuery {

    @Between("user_age")
    Range age;

    @Where("name = ?")
    String name;

  }

  @EntityRef(UserModel.class)
  static class InColumnAliasQuery {

    @In("status_code")
    List<Integer> statuses;

  }

  @EntityRef(UserModel.class)
  static class ColumnOnlyBeforeQuery {

    // @Column before @Between
    @Column("user_age")
    @Between
    Range age;

  }

  @EntityRef(UserModel.class)
  static class ColumnOnlyAfterQuery {

    // @Between before @Column
    @Between
    @Column("user_age")
    Range age;

  }

  @EntityRef(UserModel.class)
  static class ColumnOnlyInQuery {

    @Column("status_code")
    @In
    List<Integer> statuses;

  }

  @EntityRef(UserModel.class)
  static class BetweenArrayQuery {

    @Between("age")
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
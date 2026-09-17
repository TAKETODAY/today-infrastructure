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

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

import infra.jdbc.model.UserModel;
import infra.persistence.annotation.Column;
import infra.persistence.annotation.Like;
import infra.persistence.annotation.OR;
import infra.persistence.annotation.OrderBy;
import infra.persistence.annotation.Table;
import infra.persistence.platform.Platform;
import infra.persistence.sql.OrderByClause;
import infra.persistence.sql.Restriction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Verifies the core {@link ExampleQuery} rendering behaviour: non-null property
 * scanning, {@code @Where}/{@code @Like} strategies, {@code @OR} grouping,
 * order-by resolution and parameter binding.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
class ExampleQueryRenderingTests {

  final DefaultEntityMetadataFactory metadataFactory = new DefaultEntityMetadataFactory();

  final EntityMetadata metadata = metadataFactory.getEntityMetadata(UserModel.class);

  final List<PropertyConditionStrategy> strategies = new DefaultEntityQueryFactory(metadataFactory).getStrategies();

  @Test
  void rendersEqualityConditionForNonNullProperty() {
    UserModel example = new UserModel();
    example.name = "TODAY";

    ExampleQuery query = new ExampleQuery(metadataFactory, example, strategies);

    ArrayList<Restriction> restrictions = new ArrayList<>();
    query.collectRestrictions(metadata, restrictions);

    assertThat(restrictions).hasSize(1);
    StringBuilder sql = new StringBuilder();
    Restriction.appendWhereClause(Platform.mysql(), restrictions, sql);
    assertThat(sql.toString()).contains("name = ?");
  }

  @Test
  void skipsNullProperties() {
    ExampleQuery query = new ExampleQuery(metadataFactory, new UserModel(), strategies);

    ArrayList<Restriction> restrictions = new ArrayList<>();
    query.collectRestrictions(metadata, restrictions);

    assertThat(restrictions).isEmpty();
  }

  @Test
  void combinesMultiplePropertiesWithAndByDefault() {
    UserModel example = new UserModel();
    example.name = "TODAY";
    example.age = 10;

    ExampleQuery query = new ExampleQuery(metadataFactory, example, strategies);

    ArrayList<Restriction> restrictions = new ArrayList<>();
    query.collectRestrictions(metadata, restrictions);

    assertThat(restrictions).hasSize(2);
    StringBuilder sql = new StringBuilder();
    Restriction.appendWhereClause(Platform.mysql(), restrictions, sql);
    assertThat(sql.toString()).contains("name = ?").contains("age = ?").contains("AND");
  }

  @Test
  void rendersLikeConditionForAnnotatedProperty() {
    LikeForm example = new LikeForm();
    example.name = "TODAY";

    EntityMetadata likeMetadata = metadataFactory.getEntityMetadata(LikeForm.class);
    ExampleQuery query = new ExampleQuery(metadataFactory, example, strategies);

    StringBuilder sql = new StringBuilder();
    query.appendWhereClause(Platform.mysql(), likeMetadata, sql);

    assertThat(sql.toString()).contains("like ?");
  }

  @Test
  void groupsOrAnnotatedPropertyWithOr() {
    OrForm example = new OrForm();
    example.age = 10;

    ExampleQuery query = new ExampleQuery(metadataFactory, example, strategies);

    ArrayList<Restriction> restrictions = new ArrayList<>();
    query.collectRestrictions(metadata, restrictions);

    assertThat(restrictions).hasSize(1);
  }

  @Test
  void resolvesClassLevelOrderBy() {
    OrderByForm example = new OrderByForm();
    example.age = 1;

    ExampleQuery query = new ExampleQuery(metadataFactory, example, strategies);

    // trigger scan(), which applies the class level @OrderBy
    StatementSequence sequence = query.render(metadata);
    String sql = sequence.toStatementString(Platform.generic());
    assertThat(sql).containsIgnoringCase("order by `age` DESC");

    OrderByClause clause = query.resolveOrderByClause(metadata);
    assertThat(clause).isNotNull();
    assertThat(clause.isEmpty()).isFalse();
  }

  @Test
  void bindsParametersForAllConditions() throws Exception {
    UserModel example = new UserModel();
    example.name = "TODAY";
    example.age = 10;

    ExampleQuery query = new ExampleQuery(metadataFactory, example, strategies);
    PreparedStatement statement = mock(PreparedStatement.class);

    query.setParameter(metadata, statement);

    verify(statement).setString(2, "TODAY");
    verify(statement).setInt(1, 10);
  }

  @Test
  void rendersSelectWithWhereClause() {
    UserModel example = new UserModel();
    example.name = "TODAY";

    ExampleQuery query = new ExampleQuery(metadataFactory, example, strategies);
    StatementSequence sequence = query.render(metadata);

    String sql = sequence.toStatementString(Platform.generic());
    assertThat(sql).startsWith("SELECT").contains("FROM t_user").contains("WHERE name = ?");
  }

  @Table("t_like_form")
  static class LikeForm {

    @Like
    String name;

  }

  @Table("t_or_form")
  static class OrForm {

    @OR
    Integer age;

  }

  @Table("t_order_by_form")
  @OrderBy("`age` DESC")
  static class OrderByForm {

    @Column("age")
    Integer age;

  }

}

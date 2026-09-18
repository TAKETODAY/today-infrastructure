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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import org.junit.jupiter.api.Test;

import infra.persistence.annotation.Id;
import infra.persistence.annotation.OrderBy;
import infra.persistence.annotation.OrderByClause;
import infra.persistence.annotation.Table;
import infra.persistence.platform.Platform;
import infra.persistence.sql.OrderSpec;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the {@link OrderBy @OrderBy} annotation and its resolution into an
 * {@link OrderSpec} by {@link ExampleQuery}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
class OrderByTests {

  final DefaultEntityMetadataFactory metadataFactory = new DefaultEntityMetadataFactory();

  final List<PropertyConditionStrategy> strategies = new DefaultEntityQueryFactory(metadataFactory).getStrategies();

  @Test
  void ordersByAnnotatedPropertyAscendingByDefault() {
    OrderByModel example = new OrderByModel();
    example.id = 1;
    example.name = "n";

    ExampleQuery query = new ExampleQuery(metadataFactory, example, strategies);
    query.render(metadataFactory.getEntityMetadata(OrderByModel.class));

    OrderSpec orderSpec = query.resolveOrderByClause(metadataFactory.getEntityMetadata(OrderByModel.class));
    assertThat(orderSpec).isNotNull();
    assertThat(orderSpec.toClause(Platform.generic()).toString()).isEqualTo("name ASC");
  }

  @Test
  void ordersByAnnotatedPropertyWithExplicitDirection() {
    DescOrderByModel example = new DescOrderByModel();
    example.id = 1;
    example.age = 10;

    EntityMetadata metadata = metadataFactory.getEntityMetadata(DescOrderByModel.class);
    ExampleQuery query = new ExampleQuery(metadataFactory, example, strategies);
    query.render(metadata);

    OrderSpec orderSpec = query.resolveOrderByClause(metadata);
    assertThat(orderSpec).isNotNull();
    assertThat(orderSpec.toClause(Platform.generic()).toString()).isEqualTo("age DESC");
  }

  @Test
  void rendersOrderByIntoStatement() {
    DescOrderByModel example = new DescOrderByModel();
    example.id = 1;
    example.age = 10;

    ExampleQuery query = new ExampleQuery(metadataFactory, example, strategies);
    String sql = query.render(metadataFactory.getEntityMetadata(DescOrderByModel.class))
            .toStatementString(Platform.generic());

    assertThat(sql).containsIgnoringCase("order by age DESC");
  }

  @Test
  void missingAnnotationYieldsNoOrdering() {
    NoOrderByModel example = new NoOrderByModel();
    example.id = 1;

    EntityMetadata metadata = metadataFactory.getEntityMetadata(NoOrderByModel.class);
    ExampleQuery query = new ExampleQuery(metadataFactory, example, strategies);
    query.render(metadata);

    OrderSpec orderSpec = query.resolveOrderByClause(metadata);
    assertThat(orderSpec).isNull();
  }

  @Test
  void classLevelClauseOverridesPropertyLevelOrderBy() {
    PrecedenceModel example = new PrecedenceModel();
    example.id = 1;

    EntityMetadata metadata = metadataFactory.getEntityMetadata(PrecedenceModel.class);
    ExampleQuery query = new ExampleQuery(metadataFactory, example, strategies);
    query.render(metadata);

    OrderSpec orderSpec = query.resolveOrderByClause(metadata);
    assertThat(orderSpec).isNotNull();
    assertThat(orderSpec.toClause(Platform.generic())).isEqualTo("age DESC");
  }

  @Test
  void ordersByMetaAnnotatedProperty() {
    MetaOrderByModel example = new MetaOrderByModel();
    example.id = 1;
    example.score = 5;

    EntityMetadata metadata = metadataFactory.getEntityMetadata(MetaOrderByModel.class);
    ExampleQuery query = new ExampleQuery(metadataFactory, example, strategies);
    query.render(metadata);

    OrderSpec orderSpec = query.resolveOrderByClause(metadata);
    assertThat(orderSpec).isNotNull();
    assertThat(orderSpec.toClause(Platform.generic()).toString()).isEqualTo("score DESC");
  }

  @Table("t_order_by_asc")
  static class OrderByModel {

    @Id
    Integer id;

    @OrderBy
    String name;

  }

  @Table("t_order_by_desc")
  static class DescOrderByModel {

    @Id
    Integer id;

    @OrderBy(Order.DESC)
    Integer age;

  }

  @Table("t_order_by_none")
  static class NoOrderByModel {

    @Id
    Integer id;

    String name;

  }

  @Table("t_order_by_precedence")
  @OrderByClause("age DESC")
  static class PrecedenceModel {

    @Id
    Integer id;

    @OrderBy(Order.ASC)
    String name;

  }

  @Table("t_order_by_meta")
  static class MetaOrderByModel {

    @Id
    Integer id;

    @MyOrderBy
    Integer score;

  }

  @OrderBy(Order.DESC)
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.FIELD, ElementType.METHOD })
  @interface MyOrderBy {

  }

}

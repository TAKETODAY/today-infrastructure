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

import java.sql.PreparedStatement;
import java.util.ArrayList;

import infra.jdbc.model.UserModel;
import infra.persistence.DefaultEntityMetadataFactory;
import infra.persistence.EntityMetadata;
import infra.persistence.platform.Platform;
import infra.persistence.sql.OrderSpec;
import infra.persistence.sql.OrderSpecSource;
import infra.persistence.sql.Restriction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class OrderSpecEntityQueryFactoryTests {

  private final EntityMetadata metadata = new DefaultEntityMetadataFactory().getEntityMetadata(UserModel.class);

  @Test
  void createsOrderedQueryWithoutRestrictions() throws Exception {
    EntityQueryFactories factories = new EntityQueryFactories(new DefaultEntityMetadataFactory());
    OrderSpec spec = OrderSpec.builder().asc("name").desc("id").build();

    assertThat(factories.getFactories()).hasAtLeastOneElementOfType(OrderSpecEntityQueryFactory.class);
    QueryStatement query = factories.createQuery(spec);
    assertThat(query.render(metadata).toStatementString(Platform.generic()))
            .endsWith("FROM t_user order by name ASC, id DESC")
            .doesNotContain(" WHERE ");
    PreparedStatement statement = mock(PreparedStatement.class);
    query.setParameter(metadata, statement);
    verifyNoInteractions(statement);
  }

  @Test
  void createsConditionWithOnlyOrdering() throws Exception {
    EntityQueryFactories factories = new EntityQueryFactories(new DefaultEntityMetadataFactory());
    OrderSpec spec = OrderSpec.plain("LENGTH(name) DESC");
    QueryCondition condition = factories.createCondition(spec);

    assertThat(condition.resolveOrderByClause(metadata)).isSameAs(spec);
    ArrayList<Restriction> restrictions = new ArrayList<>();
    condition.collectRestrictions(metadata, restrictions);
    assertThat(restrictions).isEmpty();
    PreparedStatement statement = mock(PreparedStatement.class);
    condition.setParameter(metadata, statement);
    verifyNoInteractions(statement);
  }

  @Test
  void ignoresOtherExamplesAndEmptySpecDoesNotOrder() {
    OrderSpecEntityQueryFactory factory = new OrderSpecEntityQueryFactory();
    assertThat(factory.createQuery(new Object())).isNull();
    assertThat(factory.createCondition(null)).isNull();
    assertThat(factory.createQuery(OrderSpec.empty()).render(metadata).toStatementString(Platform.generic()))
            .endsWith("FROM t_user");
  }

  @Test
  void acceptsBuilder() {
    OrderSpecEntityQueryFactory factory = new OrderSpecEntityQueryFactory();
    QueryStatement query = factory.createQuery(OrderSpec.builder().asc("name"));
    assertThat(query.render(metadata).toStatementString(Platform.generic()))
            .endsWith("order by name ASC");
  }

  @Test
  void acceptsOrderSpecSource() {
    OrderSpecEntityQueryFactory factory = new OrderSpecEntityQueryFactory();
    OrderSpec spec = OrderSpec.desc("id");
    QueryCondition condition = factory.createCondition((OrderSpecSource) () -> spec);
    assertThat(condition.resolveOrderByClause(metadata)).isSameAs(spec);
  }

}

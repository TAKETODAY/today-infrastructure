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
import java.util.LinkedHashMap;
import java.util.Map;

import infra.jdbc.model.UserModel;
import infra.persistence.DebugDescriptive;
import infra.persistence.DefaultEntityMetadataFactory;
import infra.persistence.EntityMetadata;
import infra.persistence.EntityProperty;
import infra.persistence.Identifier;
import infra.persistence.platform.Platform;
import infra.persistence.sql.Restriction;
import infra.persistence.sql.Restrictions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/4/10 17:39
 */
class MapEntityQueryFactoryTests {

  final DefaultEntityMetadataFactory metadataFactory = new DefaultEntityMetadataFactory();
  final EntityMetadata entityMetadata = metadataFactory.getEntityMetadata(UserModel.class);

  @Test
  void createCondition() {
    MapEntityQueryFactory factory = new MapEntityQueryFactory();
    QueryCondition condition = factory.createCondition(Map.of("name", "TODAY"));
    assertThat(condition).isNotNull();

    ArrayList<Restriction> restrictions = new ArrayList<>();
    condition.collectRestrictions(entityMetadata, restrictions);
    assertThat(restrictions).hasSize(1).contains(Restrictions.equal("name"));

    //
    assertThat(factory.createCondition(null)).isNull();
    assertThat(factory.createCondition("not a map")).isNull();
  }

  @Test
  void createQuery() {
    MapEntityQueryFactory factory = new MapEntityQueryFactory();
    QueryStatement queryStatement = factory.createQuery(Map.of("name", "TODAY"));
    assertThat(queryStatement).isNotNull();

    StatementSequence sequence = queryStatement.render(entityMetadata);
    assertThat(sequence.toStatementString(Platform.generic())).endsWith("FROM t_user WHERE name = ?");

    assertThat(factory.createQuery(null)).isNull();
    assertThat(factory.createQuery("not a map")).isNull();
  }

  @Test
  void bindsFromGivenParameterIndex() throws Exception {
    QueryCondition condition = new MapEntityQueryFactory().createCondition(Map.of("name", "TODAY"));
    assertThat(condition).isNotNull();
    PreparedStatement statement = mock(PreparedStatement.class);

    assertThat(condition.setParameter(entityMetadata, statement, 3)).isEqualTo(4);
    verify(statement).setString(3, "TODAY");
  }

  @Test
  void bindsPropertiesInIterationOrder() throws Exception {
    LinkedHashMap<String, Object> params = new LinkedHashMap<>();
    params.put("name", "TODAY");
    params.put("age", 18);

    QueryCondition condition = new MapEntityQueryFactory().createCondition(params);
    assertThat(condition).isNotNull();
    PreparedStatement statement = mock(PreparedStatement.class);

    assertThat(condition.setParameter(entityMetadata, statement, 1)).isEqualTo(3);
    verify(statement).setString(1, "TODAY");
    verify(statement).setInt(2, 18);
  }

  @Test
  void bindsRawObjectWhenValueTypeDoesNotMatchPropertyType() throws Exception {
    QueryCondition condition = new MapEntityQueryFactory().createCondition(Map.of("age", "18"));
    assertThat(condition).isNotNull();
    PreparedStatement statement = mock(PreparedStatement.class);

    assertThat(condition.setParameter(entityMetadata, statement, 1)).isEqualTo(2);
    verify(statement).setObject(1, "18");
  }

  @Test
  void bindsRawObjectWhenPropertyTypeIsPrimitive() throws Exception {
    EntityProperty property = mock(EntityProperty.class);
    doReturn(int.class).when(property).getType();
    EntityMetadata metadata = mock(EntityMetadata.class);
    given(metadata.findProperty("age")).willReturn(property);

    QueryCondition condition = new MapEntityQueryFactory().createCondition(Map.of("age", 18));
    assertThat(condition).isNotNull();
    PreparedStatement statement = mock(PreparedStatement.class);

    assertThat(condition.setParameter(metadata, statement, 1)).isEqualTo(2);
    verify(statement).setObject(1, 18);
  }

  @Test
  void usesMapKeyAsColumnWhenPropertyIsUnknown() throws Exception {
    MapEntityQueryFactory factory = new MapEntityQueryFactory();
    QueryCondition condition = factory.createCondition(Map.of("nickname", "TODAY"));
    assertThat(condition).isNotNull();

    ArrayList<Restriction> restrictions = new ArrayList<>();
    condition.collectRestrictions(entityMetadata, restrictions);
    assertThat(restrictions).containsExactly(Restrictions.equal("nickname"));

    QueryStatement query = factory.createQuery(Map.of("nickname", "TODAY"));
    assertThat(query).isNotNull();
    assertThat(query.render(entityMetadata).toStatementString(Platform.generic()))
            .endsWith("FROM t_user WHERE nickname = ?");

    PreparedStatement statement = mock(PreparedStatement.class);
    assertThat(condition.setParameter(entityMetadata, statement, 1)).isEqualTo(2);
    verify(statement).setObject(1, "TODAY");
  }

  @Test
  void resolvesPropertyFromRefMetadata() throws Exception {
    EntityProperty refProperty = mock(EntityProperty.class);
    given(refProperty.getColumnName()).willReturn(Identifier.parse("ref_name"));
    doReturn(String.class).when(refProperty).getType();

    EntityMetadata refMetadata = mock(EntityMetadata.class);
    given(refMetadata.findProperty("refField")).willReturn(refProperty);

    EntityMetadata metadata = mock(EntityMetadata.class);
    given(metadata.getRefMetadata()).willReturn(refMetadata);

    QueryCondition condition = new MapEntityQueryFactory().createCondition(Map.of("refField", "value"));
    assertThat(condition).isNotNull();

    ArrayList<Restriction> restrictions = new ArrayList<>();
    condition.collectRestrictions(metadata, restrictions);
    assertThat(restrictions).containsExactly(Restrictions.equal(Identifier.parse("ref_name")));

    PreparedStatement statement = mock(PreparedStatement.class);
    assertThat(condition.setParameter(metadata, statement, 2)).isEqualTo(3);
    verify(refProperty).setParameter(statement, 2, "value");
  }

  @Test
  void usesMapKeyWhenRefMetadataHasNoProperty() throws Exception {
    EntityMetadata refMetadata = mock(EntityMetadata.class);
    EntityMetadata metadata = mock(EntityMetadata.class);
    given(metadata.getRefMetadata()).willReturn(refMetadata);

    QueryCondition condition = new MapEntityQueryFactory().createCondition(Map.of("refField", "value"));
    assertThat(condition).isNotNull();

    ArrayList<Restriction> restrictions = new ArrayList<>();
    condition.collectRestrictions(metadata, restrictions);
    assertThat(restrictions).containsExactly(Restrictions.equal("refField"));

    PreparedStatement statement = mock(PreparedStatement.class);
    assertThat(condition.setParameter(metadata, statement, 1)).isEqualTo(2);
    verify(statement).setObject(1, "value");
  }

  @Test
  void describesMapParameters() {
    LinkedHashMap<String, Object> params = new LinkedHashMap<>();
    params.put("name", "TODAY");

    QueryCondition condition = new MapEntityQueryFactory().createCondition(params);
    assertThat(condition).isInstanceOf(DebugDescriptive.class);
    assertThat(((DebugDescriptive) condition).getDescription())
            .isEqualTo("Query with Map of params: " + params);
  }

}

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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import infra.jdbc.model.UserModel;
import infra.persistence.annotation.EntityRef;
import infra.persistence.annotation.WhereIsNull;
import infra.persistence.platform.GenericPlatform;
import infra.persistence.sql.Restriction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Verifies the {@link WhereIsNull @NullQuery} behaviour: a {@code null} value on a
 * property annotated with {@code @NullQuery} contributes an {@code IS NULL} /
 * {@code IS NOT NULL} predicate, while a {@code null} value on any other property
 * takes no part in the query.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
class WhereIsNullTests {

  private final DefaultEntityMetadataFactory metadataFactory = new DefaultEntityMetadataFactory();

  private final EntityMetadata userModelMetadata = metadataFactory.getEntityMetadata(UserModel.class);

  private final List<PropertyConditionStrategy> strategies = new DefaultEntityQueryFactory(metadataFactory).getStrategies();

  @Test
  void nullQueryRendersIsNull() {
    NullQueryForm example = new NullQueryForm();

    ExampleQuery query = new ExampleQuery(metadataFactory, example, strategies);
    ArrayList<Restriction> restrictions = new ArrayList<>();
    query.collectRestrictions(userModelMetadata, restrictions);

    assertThat(restrictions).hasSize(1);
    StringBuilder sql = new StringBuilder();
    Restriction.appendWhereClause(new GenericPlatform(), restrictions, sql);
    assertThat(sql.toString()).containsIgnoringCase("deleted_at IS NULL");
  }

  @Test
  void nullQueryNotRendersIsNotNull() {
    NotNullQueryForm example = new NotNullQueryForm();

    ExampleQuery query = new ExampleQuery(metadataFactory, example, strategies);
    ArrayList<Restriction> restrictions = new ArrayList<>();
    query.collectRestrictions(userModelMetadata, restrictions);

    assertThat(restrictions).hasSize(1);
    StringBuilder sql = new StringBuilder();
    Restriction.appendWhereClause(new GenericPlatform(), restrictions, sql);
    assertThat(sql.toString()).containsIgnoringCase("status IS NOT NULL");
  }

  @Test
  void nullQueryIsSkippedWhenValuePresent() {
    NullQueryForm example = new NullQueryForm();
    example.deletedAt = Instant.EPOCH;

    ExampleQuery query = new ExampleQuery(metadataFactory, example, strategies);
    ArrayList<Restriction> restrictions = new ArrayList<>();
    query.collectRestrictions(userModelMetadata, restrictions);

    assertThat(restrictions).hasSize(1);
    StringBuilder sql = new StringBuilder();
    Restriction.appendWhereClause(new GenericPlatform(), restrictions, sql);
    assertThat(sql.toString()).contains("deleted_at = ?");
  }

  @Test
  void nullQueryConditionConsumesNoParameter() throws Exception {
    NullQueryForm example = new NullQueryForm();

    ExampleQuery query = new ExampleQuery(metadataFactory, example, strategies);
    PreparedStatement statement = mock(PreparedStatement.class);

    query.setParameter(userModelMetadata, statement);

    verifyNoInteractions(statement);
  }

  @EntityRef(UserModel.class)
  static class NullQueryForm {

    @WhereIsNull
    public @Nullable Instant deletedAt;
  }

  @EntityRef(UserModel.class)
  static class NotNullQueryForm {

    @WhereIsNull(not = true)
    public @Nullable Integer status;

  }

}

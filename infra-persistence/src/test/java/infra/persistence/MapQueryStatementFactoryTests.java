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

import java.util.ArrayList;
import java.util.Map;

import infra.jdbc.model.UserModel;
import infra.persistence.platform.Platform;
import infra.persistence.sql.Restriction;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/4/10 17:39
 */
class MapQueryStatementFactoryTests {

  final DefaultEntityMetadataFactory metadataFactory = new DefaultEntityMetadataFactory();
  final EntityMetadata entityMetadata = metadataFactory.getEntityMetadata(UserModel.class);

  @Test
  void createCondition() {
    MapQueryStatementFactory factory = new MapQueryStatementFactory();
    ConditionStatement condition = factory.createCondition(Map.of("name", "TODAY"));
    assertThat(condition).isNotNull();

    ArrayList<Restriction> restrictions = new ArrayList<>();
    condition.renderWhereClause(entityMetadata, restrictions);
    assertThat(restrictions).hasSize(1).contains(Restriction.equal("name"));

    //
    assertThat(factory.createCondition(null)).isNull();
  }

  @Test
  void createQuery() {
    MapQueryStatementFactory factory = new MapQueryStatementFactory();
    QueryStatement queryStatement = factory.createQuery(Map.of("name", "TODAY"));
    assertThat(queryStatement).isNotNull();

    StatementSequence sequence = queryStatement.render(entityMetadata);
    assertThat(sequence.toStatementString(Platform.generic())).endsWith("FROM t_user WHERE `name` = ?");

    assertThat(factory.createQuery(null)).isNull();
  }

}
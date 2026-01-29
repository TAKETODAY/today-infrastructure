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

import java.util.Map;

import infra.jdbc.model.UserModel;
import infra.persistence.sql.OrderByClause;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/2/16 22:47
 */
class NoConditionsOrderByQueryTests {
  DefaultEntityMetadataFactory factory = new DefaultEntityMetadataFactory();

  @Test
  void render() {
    EntityMetadata entityMetadata = factory.createEntityMetadata(UserModel.class);
    var handler = new NoConditionsOrderByQuery(OrderByClause.forMap(Map.of("name", Order.ASC, "age", Order.DESC)));

    StatementSequence select = handler.render(entityMetadata);
    assertThat(select).extracting("orderByClause").isNotNull().asString().contains("`name` ASC").contains("`age` DESC");
  }

}
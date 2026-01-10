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
import java.util.List;

import infra.persistence.model.UserModel;
import infra.persistence.platform.MySQLPlatform;
import infra.persistence.sql.Restriction;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/5/9 17:26
 */
class WhereAnnotationTests {

  private final DefaultEntityMetadataFactory metadataFactory = new DefaultEntityMetadataFactory();

  private final EntityMetadata userModelMetadata = metadataFactory.getEntityMetadata(UserModel.class);

  @Test
  void test() {
    UserQuery query = new UserQuery();
    query.status = 1;
    query.status2 = 2;
    query.status3 = 3;

    ExampleQuery exampleQuery = new ExampleQuery(metadataFactory, query, List.of());
    StringBuilder sqlBuffer = new StringBuilder();
    exampleQuery.renderWhereClause(sqlBuffer);

    String statementString = exampleQuery.render(userModelMetadata)
            .toStatementString(new MySQLPlatform());

    System.out.println(sqlBuffer);
    System.out.println(statementString);

    assertThat(statementString).endsWith(sqlBuffer);
    assertThat(sqlBuffer.toString()).isEqualTo(" WHERE status > ? AND `status2` = ? OR `status3` <= ?");

    ArrayList<Restriction> restrictions = new ArrayList<>();
    exampleQuery.renderWhereClause(userModelMetadata, restrictions);
    assertThat(restrictions).hasSize(3);

  }

  @EntityRef(UserModel.class)
  static class UserQuery {

    @Where("status > ?")
    public int status;

    @Where
    public int status2;

    @OR
    @Where(operator = " <= ")
    public int status3;

  }

}

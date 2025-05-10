/*
 * Copyright 2017 - 2025 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
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

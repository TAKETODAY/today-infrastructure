/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.persistence;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Map;

import cn.taketoday.persistence.dialect.Platform;
import cn.taketoday.persistence.model.UserModel;
import cn.taketoday.persistence.sql.Restriction;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/4/10 17:39
 */
class MapQueryHandlerFactoryTests {

  final DefaultEntityMetadataFactory metadataFactory = new DefaultEntityMetadataFactory();
  final EntityMetadata entityMetadata = metadataFactory.getEntityMetadata(UserModel.class);

  @Test
  void createCondition() {
    MapQueryHandlerFactory factory = new MapQueryHandlerFactory();
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
    MapQueryHandlerFactory factory = new MapQueryHandlerFactory();
    QueryStatement queryStatement = factory.createQuery(Map.of("name", "TODAY"));
    assertThat(queryStatement).isNotNull();

    StatementSequence sequence = queryStatement.render(entityMetadata);
    assertThat(sequence.toStatementString(Platform.forClasspath())).endsWith("FROM t_user WHERE `name` = ?");

    assertThat(factory.createQuery(null)).isNull();
  }

}
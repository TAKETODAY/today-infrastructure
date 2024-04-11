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

import java.util.Map;

import cn.taketoday.persistence.model.UserModel;
import cn.taketoday.persistence.sql.OrderByClause;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2024/2/16 22:47
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
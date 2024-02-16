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

package cn.taketoday.jdbc.persistence;

import org.junit.jupiter.api.Test;

import java.util.Map;

import cn.taketoday.jdbc.persistence.dialect.MySQLPlatform;
import cn.taketoday.jdbc.persistence.model.UserModel;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2024/2/16 22:47
 */
class FindAllOrderByQueryHandlerTests {
  DefaultEntityMetadataFactory factory = new DefaultEntityMetadataFactory();

  @Test
  void render() {
    EntityMetadata entityMetadata = factory.createEntityMetadata(UserModel.class);
    var handler = new FindAllOrderByQueryHandler(Map.of("name", Order.ASC, "age", Order.DESC));

    Select select = new Select(new MySQLPlatform());
    handler.render(entityMetadata, select);

    assertThat(select.orderByClause).isNotNull().asString().contains("`name` ASC").contains("`age` DESC");
  }

}
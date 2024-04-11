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

import cn.taketoday.persistence.model.UserModel;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/16 22:56
 */
class TableNameGeneratorTests {

  @Test
  void forAnnotation() {
    TableNameGenerator generator = TableNameGenerator.forTableAnnotation();
    assertThat(generator.generateTableName(UserModel.class)).isEqualTo("t_user");
    assertThat(generator.generateTableName(UpdateUserName.class)).isEqualTo("t_user");
  }

  @EntityRef(UserModel.class)
  static class UpdateUserName {

  }

}
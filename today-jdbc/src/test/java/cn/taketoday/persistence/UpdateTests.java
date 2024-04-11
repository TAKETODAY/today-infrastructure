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

import cn.taketoday.persistence.dialect.Platform;
import cn.taketoday.persistence.sql.Update;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/9/9 22:29
 */
class UpdateTests {

  private final Platform platform = Platform.forClasspath();

  @Test
  void sql() {
    Update update = new Update("t_user");
    update.addAssignment("name");
    update.addRestriction("id");

    assertThat(update.toStatementString(platform)).isEqualTo("UPDATE t_user set `name`=? WHERE `id` = ?");

    update.addAssignment("name", ":name");
    assertThat(update.toStatementString(platform)).isEqualTo("UPDATE t_user set `name`=:name WHERE `id` = ?");
  }

}
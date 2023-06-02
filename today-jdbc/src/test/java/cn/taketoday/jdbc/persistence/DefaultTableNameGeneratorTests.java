/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.jdbc.persistence;

import org.junit.jupiter.api.Test;

import cn.taketoday.jdbc.persistence.model.UserModel;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/16 23:10
 */
class DefaultTableNameGeneratorTests {

  @Test
  void generateTableName() {
    DefaultTableNameGenerator generator = new DefaultTableNameGenerator();
    generator.setSuffixArrayToRemove("Model", "Entity");
    generator.setPrefixToAppend("t_");

    assertThat(generator.generateTableName(UserModel.class)).isEqualTo("t_user");
    generator.setSuffixToRemove(null);
    assertThat(generator.generateTableName(UserModel.class)).isEqualTo("t_user_model");

    generator.setPrefixToAppend(null);
    assertThat(generator.generateTableName(UserModel.class)).isEqualTo("user_model");

    generator.setCamelCaseToUnderscore(false);
    generator.setLowercase(false);

    assertThat(generator.generateTableName(UserModel.class)).isEqualTo("UserModel");

    // lowercase
    generator.setLowercase(true);
    generator.setCamelCaseToUnderscore(false);
    assertThat(generator.generateTableName(UserModel.class)).isEqualTo("usermodel");

    // suffixArrayToRemove
    generator.setSuffixToRemove("Model");
    assertThat(generator.generateTableName(UserModel.class)).isEqualTo("user");

  }

}
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

import javax.annotation.Nullable;

import cn.taketoday.persistence.model.UserModel;
import cn.taketoday.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/16 23:10
 */
class DefaultTableNameGeneratorTests {
  DefaultTableNameGenerator generator = new DefaultTableNameGenerator();

  @Test
  void generateTableName() {
    ReflectionTestUtils.setField(generator, "annotationGenerator", new TableNameGenerator() {
      @Nullable
      @Override
      public String generateTableName(Class<?> entityClass) {
        return null;
      }
    });

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

  @Test
  void annotationGenerator() {
    assertThat(generator.generateTableName(UserModel.class)).isEqualTo("t_user");
  }
}
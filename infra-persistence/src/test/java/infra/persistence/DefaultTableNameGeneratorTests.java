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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import infra.jdbc.model.UserModel;
import infra.test.util.ReflectionTestUtils;

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
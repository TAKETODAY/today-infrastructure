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

import infra.persistence.model.UserModel;

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
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

package infra.beans.factory.parsing;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractComponentDefinitionTests {

  @Test
  void defaultComponentViewsAreEmptyAndDescriptionDelegatesToName() {
    var definition = new AbstractComponentDefinition() {
      @Override
      public String getName() {
        return "component";
      }
    };

    assertThat(definition.getDescription()).isEqualTo("component");
    assertThat(definition.getBeanDefinitions()).isEmpty();
    assertThat(definition.getInnerBeanDefinitions()).isEmpty();
    assertThat(definition.getBeanReferences()).isEmpty();
    assertThat(definition.toString()).isEqualTo("component");
  }

}

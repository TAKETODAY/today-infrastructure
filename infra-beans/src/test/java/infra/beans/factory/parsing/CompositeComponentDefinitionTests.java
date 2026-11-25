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
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

class CompositeComponentDefinitionTests {

  @Test
  void retainsNameSourceAndNestedComponents() {
    var source = new Object();
    var first = mock(ComponentDefinition.class);
    var second = mock(ComponentDefinition.class);
    var definition = new CompositeComponentDefinition("composite", source);
    assertThat(definition.getName()).isEqualTo("composite");
    assertThat(definition.getSource()).isSameAs(source);
    assertThat(definition.getNestedComponents()).isEmpty();

    definition.addNestedComponent(first);
    definition.addNestedComponent(second);
    assertThat(definition.getNestedComponents()).containsExactly(first, second);
    assertThat(definition.toString()).isEqualTo("composite");
    assertThatIllegalArgumentException().isThrownBy(() -> definition.addNestedComponent(null));
  }

  @Test
  void nameIsRequired() {
    assertThatIllegalArgumentException().isThrownBy(() -> new CompositeComponentDefinition(null, null));
  }

}

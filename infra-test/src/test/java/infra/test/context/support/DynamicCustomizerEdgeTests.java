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

package infra.test.context.support;

import org.junit.jupiter.api.Test;

import java.util.Set;

import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.context.ConfigurableApplicationContext;
import infra.context.support.GenericApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class DynamicCustomizerEdgeTests {

  @Test
  void incompatibleBeanFactoryIsRejectedAndRegistrationIsIdempotent() {
    var customizer = new DynamicPropertiesContextCustomizer(Set.of());
    ConfigurableApplicationContext incompatible = mock(ConfigurableApplicationContext.class);
    given(incompatible.getBeanFactory()).willReturn(mock(ConfigurableBeanFactory.class));
    assertThatIllegalStateException().isThrownBy(() -> customizer.customizeContext(incompatible, null))
            .withMessage("BeanFactory must be a BeanDefinitionRegistry");
    try (GenericApplicationContext context = new GenericApplicationContext()) {
      customizer.customizeContext(context, null);
      customizer.customizeContext(context, null);
      assertThat(context.getBeanDefinitionCount()).isEqualTo(1);
    }
    assertThat(customizer).isEqualTo(customizer).isEqualTo(new DynamicPropertiesContextCustomizer(Set.of()))
            .hasSameHashCodeAs(new DynamicPropertiesContextCustomizer(Set.of()))
            .isNotEqualTo(null).isNotEqualTo(new Object());
  }

  @Test
  void dynamicPropertyMethodsMustAcceptExactlyOneRegistryArgument() throws Exception {
    for (String name : new String[] { "noArguments", "wrongType" }) {
      var method = name.equals("noArguments") ? Samples.class.getDeclaredMethod(name)
              : Samples.class.getDeclaredMethod(name, String.class);
      assertThatIllegalStateException().isThrownBy(() -> new DynamicPropertiesContextCustomizer(Set.of(method)))
              .withMessageContaining("single DynamicPropertyRegistry argument");
    }
  }

  static class Samples {
    static void noArguments() { }
    static void wrongType(String value) { }
  }

}

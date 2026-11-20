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

import infra.beans.factory.support.StandardBeanFactory;
import infra.beans.factory.support.RootBeanDefinition;
import infra.context.ConfigurableApplicationContext;
import infra.context.support.StaticApplicationContext;
import infra.core.type.AnnotationMetadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

class ImportsContextCustomizerAdditionalTests {

  @Test
  void customizingAnAbstractContextUsesItsBeanFactory() {
    try (StaticApplicationContext context = new StaticApplicationContext()) {
      ImportsContextCustomizer customizer = new ImportsContextCustomizer(getClass());
      customizer.customizeContext(context, null);
      assertThat(context.containsBeanDefinition(ImportsContextCustomizer.ImportsConfiguration.BEAN_NAME)).isTrue();
      assertThat(customizer).isEqualTo(customizer).isNotEqualTo(null).isNotEqualTo("other");
      assertThat(customizer.toString()).contains("key");
    }
  }

  @Test
  void rejectsContextsWithoutARegistry() {
    var customizer = new ImportsContextCustomizer(getClass());
    assertThatIllegalStateException().isThrownBy(() -> customizer.customizeContext(mock(ConfigurableApplicationContext.class), null))
            .withMessage("Could not locate BeanDefinitionRegistry");
  }

  @Test
  void selectorWithoutTestClassNamesAndCleanupOfAbsentDefinitionsAreSafe() {
    var factory = new StandardBeanFactory();
    factory.registerBeanDefinition(ImportsContextCustomizer.ImportsConfiguration.BEAN_NAME,
            new RootBeanDefinition(ImportsContextCustomizer.ImportsConfiguration.class));
    var selector = new ImportsContextCustomizer.ImportsSelector();
    selector.setBeanFactory(factory);
    assertThat(selector.selectImports(AnnotationMetadata.introspect(getClass()))).isEmpty();
    new ImportsContextCustomizer.ImportsCleanupPostProcessor(new String[] { "missing" })
            .postProcessBeanDefinitionRegistry(factory);
    assertThat(factory.getBeanDefinitionNames()).isEmpty();
  }

}

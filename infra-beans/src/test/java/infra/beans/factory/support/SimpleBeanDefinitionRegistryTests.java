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

package infra.beans.factory.support;

import org.junit.jupiter.api.Test;

import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.beans.factory.config.BeanDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

class SimpleBeanDefinitionRegistryTests {

  private final SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();

  @Test
  void tracksDefinitionsAndAliasesAcrossRegistrationAndRemoval() {
    var definition = new RootBeanDefinition(StringBuilder.class);
    this.registry.registerBeanDefinition("builder", definition);
    this.registry.registerAlias("builder", "alias");

    assertThat(this.registry.getBeanDefinitions()).containsOnlyKeys("builder");
    assertThat(this.registry.getBeanDefinitionNames()).containsExactly("builder");
    assertThat(this.registry.getBeanDefinitionCount()).isOne();
    assertThat(this.registry.getBeanDefinition("builder")).isSameAs(definition);
    assertThat(this.registry.isBeanNameInUse("builder")).isTrue();
    assertThat(this.registry.isBeanNameInUse("alias")).isTrue();
    assertThat(this.registry.containsBeanDefinition("alias")).isFalse();

    this.registry.removeAlias("alias");
    this.registry.removeBeanDefinition("builder");

    assertThat(this.registry.getBeanDefinitionCount()).isZero();
    assertThat(this.registry.isBeanNameInUse("builder")).isFalse();
    assertThat(this.registry.isBeanNameInUse("alias")).isFalse();
    assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
            .isThrownBy(() -> this.registry.getBeanDefinition("builder")).withMessageContaining("builder");
    assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
            .isThrownBy(() -> this.registry.removeBeanDefinition("builder")).withMessageContaining("builder");
  }

  @Test
  void distinguishesAssignableFromExactTypeLookup() {
    var definition = new RootBeanDefinition(StringBuilder.class);
    this.registry.registerBeanDefinition("builder", definition);
    this.registry.registerBeanDefinition("other", new RootBeanDefinition(Integer.class));
    this.registry.registerBeanDefinition("opaque", mock(BeanDefinition.class));

    assertThat(this.registry.getBeanDefinition(CharSequence.class)).isSameAs(definition);
    assertThat(this.registry.getBeanDefinition(Runnable.class)).isNull();
    assertThat(this.registry.containsBeanDefinition(CharSequence.class)).isTrue();
    assertThat(this.registry.containsBeanDefinition(CharSequence.class, true)).isFalse();
    assertThat(this.registry.containsBeanDefinition(StringBuilder.class, true)).isTrue();
    assertThat(this.registry.containsBeanDefinition(Runnable.class)).isFalse();
  }

  @Test
  void replacesDefinitionByDefaultAndExposesOverridePolicy() {
    var replacement = new RootBeanDefinition(StringBuffer.class);
    this.registry.registerBeanDefinition("builder", new RootBeanDefinition(StringBuilder.class));
    assertThat(this.registry.isBeanDefinitionOverridable("builder")).isTrue();

    this.registry.registerBeanDefinition("builder", replacement);
    assertThat(this.registry.getBeanDefinition("builder")).isSameAs(replacement);
    assertThat(this.registry.getBeanDefinitionCount()).isOne();

    this.registry.setAllowBeanDefinitionOverriding(false);
    assertThat(this.registry.isBeanDefinitionOverridable("builder")).isFalse();
  }

  @Test
  void rejectsBlankBeanName() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.registry.registerBeanDefinition(" ", new RootBeanDefinition(StringBuilder.class)))
            .withMessageContaining("beanName");
    assertThat(this.registry.getBeanDefinitions()).isEmpty();
  }

}

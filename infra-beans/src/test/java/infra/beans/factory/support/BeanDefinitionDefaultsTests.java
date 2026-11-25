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

import static org.assertj.core.api.Assertions.assertThat;

class BeanDefinitionDefaultsTests {

  @Test
  void leavesExplicitLazyInitUnchangedWhenDefaultIsUnset() {
    var defaults = new BeanDefinitionDefaults();
    var definition = new RootBeanDefinition(StringBuilder.class);
    definition.setLazyInit(true);

    definition.applyDefaults(defaults);

    assertThat(defaults.isLazyInit()).isFalse();
    assertThat(definition.isLazyInit()).isTrue();
    assertThat(definition.getAutowireMode()).isEqualTo(AbstractBeanDefinition.AUTOWIRE_NO);
    assertThat(definition.getDependencyCheck()).isEqualTo(AbstractBeanDefinition.DEPENDENCY_CHECK_NONE);
  }

  @Test
  void appliesWiringSettingsAndMakesDefaultLifecycleMethodsOptional() {
    var defaults = new BeanDefinitionDefaults();
    defaults.setLazyInit(true);
    defaults.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
    defaults.setDependencyCheck(AbstractBeanDefinition.DEPENDENCY_CHECK_OBJECTS);
    defaults.setInitMethodName("initialize");
    defaults.setDestroyMethodName("shutdown");
    var definition = new RootBeanDefinition(StringBuilder.class);

    definition.applyDefaults(defaults);

    assertThat(defaults.isLazyInit()).isTrue();
    assertThat(definition.isLazyInit()).isTrue();
    assertThat(definition.getAutowireMode()).isEqualTo(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
    assertThat(definition.getDependencyCheck()).isEqualTo(AbstractBeanDefinition.DEPENDENCY_CHECK_OBJECTS);
    assertThat(definition.getInitMethodName()).isEqualTo("initialize");
    assertThat(definition.getDestroyMethodName()).isEqualTo("shutdown");
    assertThat(definition.isEnforceInitMethod()).isFalse();
    assertThat(definition.isEnforceDestroyMethod()).isFalse();

    defaults.setLazyInit(false);
    defaults.setDestroyMethodName(" ");
    definition.applyDefaults(defaults);

    assertThat(defaults.isLazyInit()).isFalse();
    assertThat(definition.isLazyInit()).isFalse();
    assertThat(definition.getDestroyMethodName()).isNull();
  }

}

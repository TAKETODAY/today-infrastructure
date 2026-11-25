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

import infra.beans.factory.config.BeanDefinitionHolder;
import infra.beans.factory.config.RuntimeBeanReference;
import infra.beans.factory.support.GenericBeanDefinition;

import static org.assertj.core.api.Assertions.assertThat;

class BeanComponentDefinitionTests {

  @Test
  void exposesRegisteredDefinitionInnerBeansReferencesAndDescription() {
    var definition = new GenericBeanDefinition();
    var heldInner = new GenericBeanDefinition();
    var directInner = new GenericBeanDefinition();
    var nestedHolder = new BeanDefinitionHolder(heldInner, "nested");
    var directReference = new RuntimeBeanReference("dependency");
    definition.getPropertyValues().add("nestedHolder", nestedHolder);
    definition.getPropertyValues().add("inner", directInner);
    definition.getPropertyValues().add("reference", directReference);
    definition.getPropertyValues().add("literal", "not a bean reference");

    var component = new BeanComponentDefinition(definition, "sample", new String[] { "alias" });
    assertThat(component.getName()).isEqualTo("sample");
    assertThat(component.getDescription()).contains("sample", "alias");
    assertThat(component.getBeanDefinitions()).containsExactly(definition);
    assertThat(component.getInnerBeanDefinitions()).satisfiesExactly(
            inner -> assertThat(inner).isSameAs(heldInner),
            inner -> assertThat(inner).isSameAs(directInner));
    assertThat(component.getBeanReferences()).containsExactly(directReference);
    assertThat(component.toString()).isEqualTo(component.getDescription());
  }

  @Test
  void equalComponentsRequireTheSameComponentTypeAndHolderContents() {
    var definition = new GenericBeanDefinition();
    var first = new BeanComponentDefinition(definition, "sample");
    var second = new BeanComponentDefinition(definition, "sample");
    assertThat(first).isEqualTo(second);
    assertThat(first.hashCode()).isEqualTo(second.hashCode());
    assertThat(first.equals(first)).isTrue();
    assertThat(first.equals(new BeanDefinitionHolder(definition, "sample"))).isFalse();
    assertThat(first).isNotEqualTo(new BeanComponentDefinition(definition, "other"));
  }

}

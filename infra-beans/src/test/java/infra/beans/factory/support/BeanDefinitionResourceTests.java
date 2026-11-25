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

import java.io.FileNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class BeanDefinitionResourceTests {

  @Test
  void originatingDefinitionProvidesDescriptionButNoReadableResource() {
    var origin = new RootBeanDefinition(String.class);
    origin.setResourceDescription("original.xml");
    var decorated = new RootBeanDefinition(String.class);
    decorated.setOriginatingBeanDefinition(origin);

    assertThat(decorated.getOriginatingBeanDefinition()).isSameAs(origin);
    assertThat(decorated.getResourceDescription()).isEqualTo("BeanDefinition defined in original.xml");
    var resource = decorated.getResource();
    assertThat(resource).isNotNull();
    assertThat(resource.exists()).isFalse();
    assertThat(resource.isReadable()).isFalse();
    assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(resource::getInputStream)
            .withMessageContaining("original.xml");
  }

  @Test
  void equalityUsesTheWrappedDefinition() {
    var definition = new RootBeanDefinition(String.class);
    var resource = new BeanDefinitionResource(definition);
    var equivalent = new BeanDefinitionResource(new RootBeanDefinition(String.class));

    assertThat(resource).isEqualTo(resource).isEqualTo(equivalent).hasSameHashCodeAs(equivalent)
            .isNotEqualTo(new BeanDefinitionResource(new RootBeanDefinition(Integer.class)))
            .isNotEqualTo(definition);
  }

}

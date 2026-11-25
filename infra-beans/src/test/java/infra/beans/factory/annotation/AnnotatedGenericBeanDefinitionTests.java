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

package infra.beans.factory.annotation;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import infra.core.type.AnnotationMetadata;
import infra.core.type.MethodMetadata;
import infra.core.type.classreading.SimpleMetadataReaderFactory;

import static org.assertj.core.api.Assertions.assertThat;

class AnnotatedGenericBeanDefinitionTests {

  @Test
  void introspectsAnnotationsFromLoadedClass() {
    var definition = new AnnotatedGenericBeanDefinition(Sample.class);

    assertThat(definition.getBeanClass()).isEqualTo(Sample.class);
    assertThat(definition.getMetadata().hasAnnotation(Qualifier.class.getName())).isTrue();
    assertThat(definition.getFactoryMethodMetadata()).isNull();
  }

  @Test
  void retainsAlreadyLoadedClassFromReflectionMetadata() {
    AnnotationMetadata metadata = AnnotationMetadata.introspect(Sample.class);
    var definition = new AnnotatedGenericBeanDefinition(metadata);

    assertThat(definition.getBeanClass()).isEqualTo(Sample.class);
    assertThat(definition.getMetadata()).isSameAs(metadata);
  }

  @Test
  void recordsFactoryMethodFromClassFilesWithoutResolvingBeanClass() throws IOException {
    var readerFactory = new SimpleMetadataReaderFactory();
    AnnotationMetadata metadata = readerFactory.getMetadataReader(Sample.class.getName()).getAnnotationMetadata();
    MethodMetadata method = metadata.getAnnotatedMethods(Qualifier.class.getName()).iterator().next();
    var definition = new AnnotatedGenericBeanDefinition(metadata, method);

    assertThat(definition.hasBeanClass()).isFalse();
    assertThat(definition.getBeanClassName()).isEqualTo(Sample.class.getName());
    assertThat(definition.getMetadata()).isSameAs(metadata);
    assertThat(definition.getFactoryMethodName()).isEqualTo("create");
    assertThat(definition.getFactoryMethodMetadata()).isSameAs(method);
  }

  @Qualifier("sample")
  static class Sample {

    @Qualifier("factory")
    static String create() {
      return "sample";
    }

  }

}

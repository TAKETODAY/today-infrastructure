/*
 * Copyright 2012-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.context.properties.processor;

import org.junit.jupiter.api.Test;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

import infra.context.properties.sample.lombok.LombokDefaultValueProperties;
import infra.context.properties.sample.lombok.LombokDeprecatedProperties;
import infra.context.properties.sample.lombok.LombokDeprecatedSingleProperty;
import infra.context.properties.sample.lombok.LombokExplicitProperties;
import infra.context.properties.sample.lombok.LombokInnerClassProperties;
import infra.context.properties.sample.lombok.LombokSimpleDataProperties;
import infra.context.properties.sample.lombok.LombokSimpleProperties;
import infra.context.properties.sample.lombok.LombokSimpleValueProperties;
import infra.context.properties.sample.simple.SimpleProperties;
import infra.context.properties.sample.specific.InnerClassProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LombokPropertyDescriptor}.
 *
 * @author Stephane Nicoll
 */
class LombokPropertyDescriptorTests extends PropertyDescriptorTests {

  @Test
  void lombokSimpleProperty() {
    process(LombokSimpleProperties.class, (roundEnv, metadataEnv) -> {
      TypeElement ownerElement = roundEnv.getRootElement(LombokSimpleProperties.class);
      LombokPropertyDescriptor property = createPropertyDescriptor(ownerElement, "name");
      assertThat(property.getName()).isEqualTo("name");
      assertThat(property.getSource()).isSameAs(property.getField());
      assertThat(property.getField().getSimpleName()).hasToString("name");
      assertThat(property.isProperty(metadataEnv)).isTrue();
      assertThat(property.isNested(metadataEnv)).isFalse();
    });
  }

  @Test
  void lombokCollectionProperty() {
    process(LombokSimpleProperties.class, (roundEnv, metadataEnv) -> {
      TypeElement ownerElement = roundEnv.getRootElement(LombokSimpleProperties.class);
      LombokPropertyDescriptor property = createPropertyDescriptor(ownerElement, "items");
      assertThat(property.getName()).isEqualTo("items");
      assertThat(property.getSource()).isSameAs(property.getField());
      assertThat(property.getField().getSimpleName()).hasToString("items");
      assertThat(property.isProperty(metadataEnv)).isTrue();
      assertThat(property.isNested(metadataEnv)).isFalse();
    });
  }

  @Test
  void lombokNestedPropertySameClass() {
    process(LombokInnerClassProperties.class, (roundEnv, metadataEnv) -> {
      TypeElement ownerElement = roundEnv.getRootElement(LombokInnerClassProperties.class);
      LombokPropertyDescriptor property = createPropertyDescriptor(ownerElement, "first");
      assertThat(property.getName()).isEqualTo("first");
      assertThat(property.getSource()).isSameAs(property.getField());
      assertThat(property.getField().getSimpleName()).hasToString("first");
      assertThat(property.isProperty(metadataEnv)).isFalse();
      assertThat(property.isNested(metadataEnv)).isTrue();
    });
  }

  @Test
  void lombokNestedPropertyWithAnnotation() {
    process(LombokInnerClassProperties.class, (roundEnv, metadataEnv) -> {
      TypeElement ownerElement = roundEnv.getRootElement(LombokInnerClassProperties.class);
      LombokPropertyDescriptor property = createPropertyDescriptor(ownerElement, "third");
      assertThat(property.getName()).isEqualTo("third");
      assertThat(property.getSource()).isSameAs(property.getField());
      assertThat(property.getField().getSimpleName()).hasToString("third");
      assertThat(property.isProperty(metadataEnv)).isFalse();
      assertThat(property.isNested(metadataEnv)).isTrue();
    });
  }

  @Test
  void lombokSimplePropertyWithOnlyGetterOnClassShouldNotBeExposed() {
    process(LombokSimpleProperties.class, (roundEnv, metadataEnv) -> {
      TypeElement ownerElement = roundEnv.getRootElement(LombokSimpleProperties.class);
      LombokPropertyDescriptor property = createPropertyDescriptor(ownerElement, "ignored");
      assertThat(property.isProperty(metadataEnv)).isFalse();
      assertThat(property.isNested(metadataEnv)).isFalse();
    });
  }

  @Test
  void lombokSimplePropertyWithOnlyGetterOnDataClassShouldNotBeExposed() {
    process(LombokSimpleDataProperties.class, (roundEnv, metadataEnv) -> {
      TypeElement ownerElement = roundEnv.getRootElement(LombokSimpleDataProperties.class);
      LombokPropertyDescriptor property = createPropertyDescriptor(ownerElement, "ignored");
      assertThat(property.isProperty(metadataEnv)).isFalse();
      assertThat(property.isNested(metadataEnv)).isFalse();
    });
  }

  @Test
  void lombokSimplePropertyWithOnlyGetterOnValueClassShouldNotBeExposed() {
    process(LombokSimpleValueProperties.class, (roundEnv, metadataEnv) -> {
      TypeElement ownerElement = roundEnv.getRootElement(LombokSimpleValueProperties.class);
      LombokPropertyDescriptor property = createPropertyDescriptor(ownerElement, "ignored");
      assertThat(property.isProperty(metadataEnv)).isFalse();
      assertThat(property.isNested(metadataEnv)).isFalse();
    });
  }

  @Test
  void lombokSimplePropertyWithOnlyGetterOnFieldShouldNotBeExposed() {
    process(LombokExplicitProperties.class, (roundEnv, metadataEnv) -> {
      TypeElement ownerElement = roundEnv.getRootElement(LombokExplicitProperties.class);
      LombokPropertyDescriptor property = createPropertyDescriptor(ownerElement, "ignoredOnlyGetter");
      assertThat(property.isProperty(metadataEnv)).isFalse();
      assertThat(property.isNested(metadataEnv)).isFalse();
    });
  }

  @Test
  void lombokSimplePropertyWithOnlySetterOnFieldShouldNotBeExposed() {
    process(LombokExplicitProperties.class, (roundEnv, metadataEnv) -> {
      TypeElement ownerElement = roundEnv.getRootElement(LombokExplicitProperties.class);
      LombokPropertyDescriptor property = createPropertyDescriptor(ownerElement, "ignoredOnlySetter");
      assertThat(property.isProperty(metadataEnv)).isFalse();
      assertThat(property.isNested(metadataEnv)).isFalse();
    });
  }

  @Test
  void lombokMetadataSimpleProperty() {
    process(LombokSimpleProperties.class, (roundEnv, metadataEnv) -> {
      TypeElement ownerElement = roundEnv.getRootElement(LombokSimpleProperties.class);
      LombokPropertyDescriptor property = createPropertyDescriptor(ownerElement, "description");
      assertItemMetadata(metadataEnv, property).isProperty()
              .hasName("test.description")
              .hasType(String.class)
              .hasSourceType(LombokSimpleProperties.class)
              .hasNoDescription()
              .isNotDeprecated();
    });
  }

  @Test
  void lombokMetadataCollectionProperty() {
    process(LombokSimpleProperties.class, (roundEnv, metadataEnv) -> {
      TypeElement ownerElement = roundEnv.getRootElement(LombokSimpleProperties.class);
      LombokPropertyDescriptor property = createPropertyDescriptor(ownerElement, "items");
      assertItemMetadata(metadataEnv, property).isProperty()
              .hasName("test.items")
              .hasType("java.util.List<java.lang.String>")
              .hasSourceType(LombokSimpleProperties.class)
              .hasNoDescription()
              .isNotDeprecated();
    });
  }

  @Test
  void lombokMetadataNestedGroup() {
    process(LombokInnerClassProperties.class, (roundEnv, metadataEnv) -> {
      TypeElement ownerElement = roundEnv.getRootElement(LombokInnerClassProperties.class);
      VariableElement field = getField(ownerElement, "third");
      ExecutableElement getter = getMethod(ownerElement, "getThird");
      LombokPropertyDescriptor property = new LombokPropertyDescriptor(ownerElement, null, field, "third",
              field.asType(), getter, null);
      assertItemMetadata(metadataEnv, property).isGroup()
              .hasName("test.third")
              .hasType("infra.context.properties.sample.lombok.SimpleLombokPojo")
              .hasSourceType(LombokInnerClassProperties.class)
              .hasSourceMethod("getThird()")
              .hasNoDescription()
              .isNotDeprecated();
    });
  }

  @Test
  void lombokMetadataNestedGroupNoGetter() {
    process(LombokInnerClassProperties.class, (roundEnv, metadataEnv) -> {
      TypeElement ownerElement = roundEnv.getRootElement(LombokInnerClassProperties.class);
      LombokPropertyDescriptor property = createPropertyDescriptor(ownerElement, "first");
      assertItemMetadata(metadataEnv, property).isGroup()
              .hasName("test.first")
              .hasType("infra.context.properties.sample.lombok.LombokInnerClassProperties$Foo")
              .hasSourceType(LombokInnerClassProperties.class)
              .hasSourceMethod(null)
              .hasNoDescription()
              .isNotDeprecated();
    });
  }

  @Test
  void lombokMetadataNotACandidatePropertyShouldReturnNull() {
    process(LombokSimpleProperties.class, (roundEnv, metadataEnv) -> {
      TypeElement ownerElement = roundEnv.getRootElement(LombokSimpleProperties.class);
      LombokPropertyDescriptor property = createPropertyDescriptor(ownerElement, "ignored");
      assertThat(property.resolveItemMetadata("test", metadataEnv)).isNull();
    });
  }

  @Test
  @SuppressWarnings("deprecation")
  void lombokDeprecatedPropertyOnClass() {
    process(LombokDeprecatedProperties.class,
            (roundEnv, metadataEnv) -> {
              TypeElement ownerElement = roundEnv.getRootElement(
                      LombokDeprecatedProperties.class);
              LombokPropertyDescriptor property = createPropertyDescriptor(ownerElement, "name");
              assertItemMetadata(metadataEnv, property).isProperty().isDeprecatedWithNoInformation();
            });
  }

  @Test
  void lombokDeprecatedPropertyOnField() {
    process(LombokDeprecatedSingleProperty.class, (roundEnv, metadataEnv) -> {
      TypeElement ownerElement = roundEnv.getRootElement(LombokDeprecatedSingleProperty.class);
      LombokPropertyDescriptor property = createPropertyDescriptor(ownerElement, "name");
      assertItemMetadata(metadataEnv, property).isProperty().isDeprecatedWithNoInformation();
    });
  }

  @Test
  void lombokPropertyWithDescription() {
    process(LombokSimpleProperties.class, (roundEnv, metadataEnv) -> {
      TypeElement ownerElement = roundEnv.getRootElement(LombokSimpleProperties.class);
      LombokPropertyDescriptor property = createPropertyDescriptor(ownerElement, "name");
      assertItemMetadata(metadataEnv, property).isProperty().hasDescription("Name description.");
    });
  }

  @Test
  void lombokPropertyWithDefaultValue() {
    process(LombokDefaultValueProperties.class, (roundEnv, metadataEnv) -> {
      TypeElement ownerElement = roundEnv.getRootElement(LombokDefaultValueProperties.class);
      LombokPropertyDescriptor property = createPropertyDescriptor(ownerElement, "description");
      assertItemMetadata(metadataEnv, property).isProperty().hasDefaultValue("my description");
    });
  }

  @Test
  void lombokPropertyNotCandidate() {
    process(SimpleProperties.class, (roundEnv, metadataEnv) -> {
      TypeElement ownerElement = roundEnv.getRootElement(SimpleProperties.class);
      LombokPropertyDescriptor property = createPropertyDescriptor(ownerElement, "theName");
      assertThat(property.isProperty(metadataEnv)).isFalse();
      assertThat(property.isNested(metadataEnv)).isFalse();
    });
  }

  @Test
  void lombokNestedPropertyNotCandidate() {
    process(InnerClassProperties.class, (roundEnv, metadataEnv) -> {
      TypeElement ownerElement = roundEnv.getRootElement(InnerClassProperties.class);
      LombokPropertyDescriptor property = createPropertyDescriptor(ownerElement, "first");
      assertThat(property.isProperty(metadataEnv)).isFalse();
      assertThat(property.isNested(metadataEnv)).isFalse();
    });
  }

  protected LombokPropertyDescriptor createPropertyDescriptor(TypeElement ownerElement, String name) {
    VariableElement field = getField(ownerElement, name);
    ExecutableElement getter = getMethod(ownerElement, createAccessorMethodName("get", name));
    ExecutableElement setter = getMethod(ownerElement, createAccessorMethodName("set", name));
    return new LombokPropertyDescriptor(ownerElement, null, field, name, field.asType(), getter, setter);
  }

}

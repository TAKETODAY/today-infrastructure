/*
 * Copyright 2017 - 2023 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.context.properties.processor;

import org.junit.jupiter.api.Test;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

import cn.taketoday.context.properties.sample.simple.DeprecatedProperties;
import cn.taketoday.context.properties.sample.simple.DeprecatedSingleProperty;
import cn.taketoday.context.properties.sample.simple.SimpleCollectionProperties;
import cn.taketoday.context.properties.sample.simple.SimpleProperties;
import cn.taketoday.context.properties.sample.simple.SimpleTypeProperties;
import cn.taketoday.context.properties.sample.specific.InnerClassProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JavaBeanPropertyDescriptor}.
 *
 * @author Stephane Nicoll
 */
class JavaBeanPropertyDescriptorTests extends PropertyDescriptorTests {

  @Test
  void javaBeanSimpleProperty() {
    process(SimpleTypeProperties.class, (roundEnv, metadataEnv) -> {
      TypeElement ownerElement = roundEnv.getRootElement(SimpleTypeProperties.class);
      JavaBeanPropertyDescriptor property = createPropertyDescriptor(ownerElement, "myString");
      assertThat(property.getName()).isEqualTo("myString");
      assertThat(property.getSource()).isSameAs(property.getGetter());
      assertThat(property.getGetter().getSimpleName()).hasToString("getMyString");
      assertThat(property.getSetter().getSimpleName()).hasToString("setMyString");
      assertThat(property.isProperty(metadataEnv)).isTrue();
      assertThat(property.isNested(metadataEnv)).isFalse();
    });
  }

  @Test
  void javaBeanCollectionProperty() {
    process(SimpleCollectionProperties.class, (roundEnv, metadataEnv) -> {
      TypeElement ownerElement = roundEnv.getRootElement(SimpleCollectionProperties.class);
      JavaBeanPropertyDescriptor property = createPropertyDescriptor(ownerElement, "doubles");
      assertThat(property.getName()).isEqualTo("doubles");
      assertThat(property.getGetter().getSimpleName()).hasToString("getDoubles");
      assertThat(property.getSetter()).isNull();
      assertThat(property.isProperty(metadataEnv)).isTrue();
      assertThat(property.isNested(metadataEnv)).isFalse();
    });
  }

  @Test
  void javaBeanNestedPropertySameClass() {
    process(InnerClassProperties.class, (roundEnv, metadataEnv) -> {
      TypeElement ownerElement = roundEnv.getRootElement(InnerClassProperties.class);
      JavaBeanPropertyDescriptor property = createPropertyDescriptor(ownerElement, "first");
      assertThat(property.getName()).isEqualTo("first");
      assertThat(property.getGetter().getSimpleName()).hasToString("getFirst");
      assertThat(property.getSetter()).isNull();
      assertThat(property.isProperty(metadataEnv)).isFalse();
      assertThat(property.isNested(metadataEnv)).isTrue();
    });
  }

  @Test
  void javaBeanNestedPropertyWithAnnotation() {
    process(InnerClassProperties.class, (roundEnv, metadataEnv) -> {
      TypeElement ownerElement = roundEnv.getRootElement(InnerClassProperties.class);
      JavaBeanPropertyDescriptor property = createPropertyDescriptor(ownerElement, "third");
      assertThat(property.getName()).isEqualTo("third");
      assertThat(property.getGetter().getSimpleName()).hasToString("getThird");
      assertThat(property.getSetter()).isNull();
      assertThat(property.isProperty(metadataEnv)).isFalse();
      assertThat(property.isNested(metadataEnv)).isTrue();
    });
  }

  @Test
  void javaBeanSimplePropertyWithOnlyGetterShouldNotBeExposed() {
    process(SimpleProperties.class, (roundEnv, metadataEnv) -> {
      TypeElement ownerElement = roundEnv.getRootElement(SimpleProperties.class);
      ExecutableElement getter = getMethod(ownerElement, "getSize");
      VariableElement field = getField(ownerElement, "size");
      JavaBeanPropertyDescriptor property = new JavaBeanPropertyDescriptor(ownerElement, getter, getter, "size",
              field.asType(), field, null);
      assertThat(property.getName()).isEqualTo("size");
      assertThat(property.getSource()).isSameAs(property.getGetter());
      assertThat(property.getGetter().getSimpleName()).hasToString("getSize");
      assertThat(property.getSetter()).isNull();
      assertThat(property.isProperty(metadataEnv)).isFalse();
      assertThat(property.isNested(metadataEnv)).isFalse();
    });
  }

  @Test
  void javaBeanSimplePropertyWithOnlySetterShouldNotBeExposed() {
    process(SimpleProperties.class, (roundEnv, metadataEnv) -> {
      TypeElement ownerElement = roundEnv.getRootElement(SimpleProperties.class);
      VariableElement field = getField(ownerElement, "counter");
      JavaBeanPropertyDescriptor property = new JavaBeanPropertyDescriptor(ownerElement, null, null, "counter",
              field.asType(), field, getMethod(ownerElement, "setCounter"));
      assertThat(property.getName()).isEqualTo("counter");
      assertThat(property.getSource()).isSameAs(property.getGetter());
      assertThat(property.getGetter()).isNull();
      assertThat(property.getSetter().getSimpleName()).hasToString("setCounter");
      assertThat(property.isProperty(metadataEnv)).isFalse();
      assertThat(property.isNested(metadataEnv)).isFalse();
    });
  }

  @Test
  void javaBeanMetadataSimpleProperty() {
    process(SimpleTypeProperties.class, (roundEnv, metadataEnv) -> {
      TypeElement ownerElement = roundEnv.getRootElement(SimpleTypeProperties.class);
      JavaBeanPropertyDescriptor property = createPropertyDescriptor(ownerElement, "myString");
      assertItemMetadata(metadataEnv, property).isProperty()
              .hasName("test.my-string")
              .hasType(String.class)
              .hasSourceType(SimpleTypeProperties.class)
              .hasNoDescription()
              .isNotDeprecated();
    });
  }

  @Test
  void javaBeanMetadataCollectionProperty() {
    process(SimpleCollectionProperties.class, (roundEnv, metadataEnv) -> {
      TypeElement ownerElement = roundEnv.getRootElement(SimpleCollectionProperties.class);
      JavaBeanPropertyDescriptor property = createPropertyDescriptor(ownerElement, "doubles");
      assertItemMetadata(metadataEnv, property).isProperty()
              .hasName("test.doubles")
              .hasType("java.util.List<java.lang.Double>")
              .hasSourceType(SimpleCollectionProperties.class)
              .hasNoDescription()
              .isNotDeprecated();
    });
  }

  @Test
  void javaBeanMetadataNestedGroup() {
    process(InnerClassProperties.class, (roundEnv, metadataEnv) -> {
      TypeElement ownerElement = roundEnv.getRootElement(InnerClassProperties.class);
      JavaBeanPropertyDescriptor property = createPropertyDescriptor(ownerElement, "first");
      assertItemMetadata(metadataEnv, property).isGroup()
              .hasName("test.first")
              .hasType("cn.taketoday.context.properties.sample.specific.InnerClassProperties$Foo")
              .hasSourceType(InnerClassProperties.class)
              .hasSourceMethod("getFirst()")
              .hasNoDescription()
              .isNotDeprecated();
    });
  }

  @Test
  void javaBeanMetadataNotACandidatePropertyShouldReturnNull() {
    process(SimpleProperties.class, (roundEnv, metadataEnv) -> {
      TypeElement ownerElement = roundEnv.getRootElement(SimpleProperties.class);
      VariableElement field = getField(ownerElement, "counter");
      JavaBeanPropertyDescriptor property = new JavaBeanPropertyDescriptor(ownerElement, null, null, "counter",
              field.asType(), field, getMethod(ownerElement, "setCounter"));
      assertThat(property.resolveItemMetadata("test", metadataEnv)).isNull();
    });
  }

  @Test
  @SuppressWarnings("deprecation")
  void javaBeanDeprecatedPropertyOnClass() {
    process(DeprecatedProperties.class,
            (roundEnv, metadataEnv) -> {
              TypeElement ownerElement = roundEnv
                      .getRootElement(DeprecatedProperties.class);
              JavaBeanPropertyDescriptor property = createPropertyDescriptor(ownerElement, "name");
              assertItemMetadata(metadataEnv, property).isProperty().isDeprecatedWithNoInformation();
            });
  }

  @Test
  void javaBeanMetadataDeprecatedPropertyWithAnnotation() {
    process(DeprecatedSingleProperty.class, (roundEnv, metadataEnv) -> {
      TypeElement ownerElement = roundEnv.getRootElement(DeprecatedSingleProperty.class);
      JavaBeanPropertyDescriptor property = createPropertyDescriptor(ownerElement, "name");
      assertItemMetadata(metadataEnv, property).isProperty()
              .isDeprecatedWithReason("renamed")
              .isDeprecatedWithReplacement("singledeprecated.new-name");
    });
  }

  @Test
  void javaBeanDeprecatedPropertyOnGetter() {
    process(SimpleProperties.class, (roundEnv, metadataEnv) -> {
      TypeElement ownerElement = roundEnv.getRootElement(SimpleProperties.class);
      JavaBeanPropertyDescriptor property = createPropertyDescriptor(ownerElement, "flag", "isFlag", "setFlag");
      assertItemMetadata(metadataEnv, property).isProperty().isDeprecatedWithNoInformation();
    });
  }

  @Test
  void javaBeanDeprecatedPropertyOnSetter() {
    process(SimpleProperties.class, (roundEnv, metadataEnv) -> {
      TypeElement ownerElement = roundEnv.getRootElement(SimpleProperties.class);
      JavaBeanPropertyDescriptor property = createPropertyDescriptor(ownerElement, "theName");
      assertItemMetadata(metadataEnv, property).isProperty().isDeprecatedWithNoInformation();
    });
  }

  @Test
  void javaBeanPropertyWithDescription() {
    process(SimpleProperties.class, (roundEnv, metadataEnv) -> {
      TypeElement ownerElement = roundEnv.getRootElement(SimpleProperties.class);
      JavaBeanPropertyDescriptor property = createPropertyDescriptor(ownerElement, "theName");
      assertItemMetadata(metadataEnv, property).isProperty()
              .hasDescription("The name of this simple properties.");
    });
  }

  @Test
  void javaBeanPropertyWithDefaultValue() {
    process(SimpleProperties.class, (roundEnv, metadataEnv) -> {
      TypeElement ownerElement = roundEnv.getRootElement(SimpleProperties.class);
      JavaBeanPropertyDescriptor property = createPropertyDescriptor(ownerElement, "theName");
      assertItemMetadata(metadataEnv, property).isProperty().hasDefaultValue("boot");
    });
  }

  protected JavaBeanPropertyDescriptor createPropertyDescriptor(TypeElement ownerElement, String name) {
    return createPropertyDescriptor(ownerElement, name, createAccessorMethodName("get", name),
            createAccessorMethodName("set", name));
  }

  protected JavaBeanPropertyDescriptor createPropertyDescriptor(TypeElement ownerElement, String name,
          String getterName, String setterName) {
    ExecutableElement getter = getMethod(ownerElement, getterName);
    ExecutableElement setter = getMethod(ownerElement, setterName);
    VariableElement field = getField(ownerElement, name);
    return new JavaBeanPropertyDescriptor(ownerElement, null, getter, name, getter.getReturnType(), field, setter);
  }

}

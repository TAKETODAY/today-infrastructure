/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.context.properties.processor;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.function.BiConsumer;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;

import infra.context.properties.processor.test.RoundEnvironmentTester;
import infra.context.properties.processor.test.TestableAnnotationProcessor;
import infra.context.properties.sample.generic.AbstractGenericProperties;
import infra.context.properties.sample.generic.AbstractIntermediateGenericProperties;
import infra.context.properties.sample.generic.MixGenericNameProperties;
import infra.context.properties.sample.generic.SimpleGenericProperties;
import infra.context.properties.sample.generic.UnresolvedGenericProperties;
import infra.core.test.tools.SourceFile;
import infra.core.test.tools.TestCompiler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TypeUtils}.
 *
 * @author Stephane Nicoll
 * @author Scott Frederick
 */
class TypeUtilsTests {

  @Test
  void resolveTypeOnConcreteClass() {
    process(SimpleGenericProperties.class, (roundEnv, typeUtils) -> {
      TypeElement typeElement = roundEnv.getRootElement(SimpleGenericProperties.class);
      assertThat(getTypeOfField(typeUtils, typeElement, "name")).hasToString(String.class.getName());
      assertThat(getTypeOfField(typeUtils, typeElement, "mappings"))
              .hasToString(constructMapType(Integer.class, Duration.class));
    });
  }

  @Test
  void resolveTypeOnIntermediateClass() {
    process(AbstractIntermediateGenericProperties.class, (roundEnv, typeUtils) -> {
      TypeElement typeElement = roundEnv.getRootElement(AbstractIntermediateGenericProperties.class);
      assertThat(getTypeOfField(typeUtils, typeElement, "name")).hasToString(String.class.getName());
      assertThat(getTypeOfField(typeUtils, typeElement, "mappings"))
              .hasToString(constructMapType(Integer.class, Object.class));
    });
  }

  @Test
  void resolveTypeWithOnlyGenerics() {
    process(AbstractGenericProperties.class, (roundEnv, typeUtils) -> {
      TypeElement typeElement = roundEnv.getRootElement(AbstractGenericProperties.class);
      assertThat(getTypeOfField(typeUtils, typeElement, "name")).hasToString(Object.class.getName());
      assertThat(getTypeOfField(typeUtils, typeElement, "mappings"))
              .hasToString(constructMapType(Object.class, Object.class));
    });
  }

  @Test
  void resolveTypeWithUnresolvedGenericProperties() {
    process(UnresolvedGenericProperties.class, (roundEnv, typeUtils) -> {
      TypeElement typeElement = roundEnv.getRootElement(UnresolvedGenericProperties.class);
      assertThat(getTypeOfField(typeUtils, typeElement, "name")).hasToString(String.class.getName());
      assertThat(getTypeOfField(typeUtils, typeElement, "mappings"))
              .hasToString(constructMapType(Number.class, Object.class));
    });
  }

  @Test
  void resolvedTypeMixGenericNamePropertiesProperties() {
    process(MixGenericNameProperties.class, (roundEnv, typeUtils) -> {
      TypeElement typeElement = roundEnv.getRootElement(MixGenericNameProperties.class);
      assertThat(getTypeOfField(typeUtils, typeElement, "name")).hasToString(String.class.getName());
      assertThat(getTypeOfField(typeUtils, typeElement, "mappings"))
              .hasToString(constructMapType(Number.class, Object.class));
    });
  }

  private void process(Class<?> target, BiConsumer<RoundEnvironmentTester, TypeUtils> consumer) {
    TestableAnnotationProcessor<TypeUtils> processor = new TestableAnnotationProcessor<>(consumer, TypeUtils::new);
    TestCompiler compiler = TestCompiler.forSystem()
            .withProcessors(processor)
            .withSources(SourceFile.forTestClass(target));
    compiler.compile((compiled) -> {
    });
  }

  private String constructMapType(Class<?> keyType, Class<?> valueType) {
    return "%s<%s,%s>".formatted(Map.class.getName(), keyType.getName(), valueType.getName());
  }

  private String getTypeOfField(TypeUtils typeUtils, TypeElement typeElement, String name) {
    TypeMirror field = findField(typeUtils, typeElement, name);
    if (field == null) {
      throw new IllegalStateException("Unable to find field '" + name + "' in " + typeElement);
    }
    return typeUtils.getType(typeElement, field);
  }

  private TypeMirror findField(TypeUtils typeUtils, TypeElement typeElement, String name) {
    for (VariableElement variableElement : ElementFilter.fieldsIn(typeElement.getEnclosedElements())) {
      if (variableElement.getSimpleName().contentEquals(name)) {
        return variableElement.asType();
      }
    }
    TypeMirror superclass = typeElement.getSuperclass();
    if (superclass != null && !superclass.toString().equals(Object.class.getName())) {
      return findField(typeUtils, (TypeElement) typeUtils.asElement(superclass), name);
    }
    return null;
  }

}

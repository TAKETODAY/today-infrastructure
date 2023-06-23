/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.framework.test.mock.mockito;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.core.annotation.MergedAnnotations.SearchStrategy;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * Parser to create {@link MockDefinition} and {@link SpyDefinition} instances from
 * {@link MockBean @MockBean} and {@link SpyBean @SpyBean} annotations declared on or in a
 * class.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class DefinitionsParser {

  private final Set<Definition> definitions;

  private final Map<Definition, Field> definitionFields;

  DefinitionsParser() {
    this(Collections.emptySet());
  }

  DefinitionsParser(Collection<? extends Definition> existing) {
    this.definitions = new LinkedHashSet<>();
    this.definitionFields = new LinkedHashMap<>();
    if (existing != null) {
      this.definitions.addAll(existing);
    }
  }

  void parse(Class<?> source) {
    parseElement(source, null);
    ReflectionUtils.doWithFields(source, (element) -> parseElement(element, source));
  }

  private void parseElement(AnnotatedElement element, Class<?> source) {
    MergedAnnotations annotations = MergedAnnotations.from(element, SearchStrategy.SUPERCLASS);
    annotations.stream(MockBean.class).map(MergedAnnotation::synthesize)
            .forEach((annotation) -> parseMockBeanAnnotation(annotation, element, source));
    annotations.stream(SpyBean.class).map(MergedAnnotation::synthesize)
            .forEach((annotation) -> parseSpyBeanAnnotation(annotation, element, source));
  }

  private void parseMockBeanAnnotation(MockBean annotation, AnnotatedElement element, Class<?> source) {
    Set<ResolvableType> typesToMock = getOrDeduceTypes(element, annotation.value(), source);
    Assert.state(!typesToMock.isEmpty(), () -> "Unable to deduce type to mock from " + element);
    if (StringUtils.isNotEmpty(annotation.name())) {
      Assert.state(typesToMock.size() == 1, "The name attribute can only be used when mocking a single class");
    }
    for (ResolvableType typeToMock : typesToMock) {
      MockDefinition definition = new MockDefinition(annotation.name(), typeToMock, annotation.extraInterfaces(),
              annotation.answer(), annotation.serializable(), annotation.reset(),
              QualifierDefinition.forElement(element));
      addDefinition(element, definition, "mock");
    }
  }

  private void parseSpyBeanAnnotation(SpyBean annotation, AnnotatedElement element, Class<?> source) {
    Set<ResolvableType> typesToSpy = getOrDeduceTypes(element, annotation.value(), source);
    Assert.state(!typesToSpy.isEmpty(), () -> "Unable to deduce type to spy from " + element);
    if (StringUtils.isNotEmpty(annotation.name())) {
      Assert.state(typesToSpy.size() == 1, "The name attribute can only be used when spying a single class");
    }
    for (ResolvableType typeToSpy : typesToSpy) {
      SpyDefinition definition = new SpyDefinition(annotation.name(), typeToSpy, annotation.reset(),
              annotation.proxyTargetAware(), QualifierDefinition.forElement(element));
      addDefinition(element, definition, "spy");
    }
  }

  private void addDefinition(AnnotatedElement element, Definition definition, String type) {
    boolean isNewDefinition = this.definitions.add(definition);
    Assert.state(isNewDefinition, () -> "Duplicate " + type + " definition " + definition);
    if (element instanceof Field) {
      Field field = (Field) element;
      this.definitionFields.put(definition, field);
    }
  }

  private Set<ResolvableType> getOrDeduceTypes(AnnotatedElement element, Class<?>[] value, Class<?> source) {
    Set<ResolvableType> types = new LinkedHashSet<>();
    for (Class<?> clazz : value) {
      types.add(ResolvableType.forClass(clazz));
    }
    if (types.isEmpty() && element instanceof Field) {
      Field field = (Field) element;
      types.add((field.getGenericType() instanceof TypeVariable) ? ResolvableType.forField(field, source)
                                                                 : ResolvableType.forField(field));
    }
    return types;
  }

  Set<Definition> getDefinitions() {
    return Collections.unmodifiableSet(this.definitions);
  }

  Field getField(Definition definition) {
    return this.definitionFields.get(definition);
  }

}

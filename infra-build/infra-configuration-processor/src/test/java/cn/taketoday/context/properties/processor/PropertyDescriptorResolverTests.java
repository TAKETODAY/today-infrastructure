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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.lang.model.element.TypeElement;

import cn.taketoday.context.properties.processor.metadata.ItemMetadata;
import cn.taketoday.context.properties.processor.test.RoundEnvironmentTester;
import cn.taketoday.context.properties.processor.test.TestableAnnotationProcessor;
import cn.taketoday.context.properties.sample.immutable.ImmutableClassConstructorBindingProperties;
import cn.taketoday.context.properties.sample.immutable.ImmutableDeducedConstructorBindingProperties;
import cn.taketoday.context.properties.sample.immutable.ImmutableMultiConstructorProperties;
import cn.taketoday.context.properties.sample.immutable.ImmutableNameAnnotationProperties;
import cn.taketoday.context.properties.sample.immutable.ImmutableSimpleProperties;
import cn.taketoday.context.properties.sample.lombok.LombokExplicitProperties;
import cn.taketoday.context.properties.sample.lombok.LombokSimpleDataProperties;
import cn.taketoday.context.properties.sample.lombok.LombokSimpleProperties;
import cn.taketoday.context.properties.sample.lombok.LombokSimpleValueProperties;
import cn.taketoday.context.properties.sample.simple.AutowiredProperties;
import cn.taketoday.context.properties.sample.simple.HierarchicalProperties;
import cn.taketoday.context.properties.sample.simple.HierarchicalPropertiesGrandparent;
import cn.taketoday.context.properties.sample.simple.HierarchicalPropertiesParent;
import cn.taketoday.context.properties.sample.simple.SimpleProperties;
import cn.taketoday.context.properties.sample.specific.TwoConstructorsExample;
import cn.taketoday.core.test.tools.SourceFile;
import cn.taketoday.core.test.tools.TestCompiler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PropertyDescriptorResolver}.
 *
 * @author Stephane Nicoll
 * @author Scott Frederick
 */
class PropertyDescriptorResolverTests {

  @Test
  void propertiesWithJavaBeanProperties() {
    process(SimpleProperties.class,
            propertyNames((stream) -> assertThat(stream).containsExactly("theName", "flag", "comparator")));
  }

  @Test
  void propertiesWithJavaBeanHierarchicalProperties() {
    process(HierarchicalProperties.class,
            Arrays.asList(HierarchicalPropertiesParent.class, HierarchicalPropertiesGrandparent.class),
            (type, metadataEnv) -> {
              PropertyDescriptorResolver resolver = new PropertyDescriptorResolver(metadataEnv);
              assertThat(resolver.resolve(type, null).map(PropertyDescriptor::getName)).containsExactly("third",
                      "second", "first");
              assertThat(resolver.resolve(type, null)
                      .map((descriptor) -> descriptor.getGetter().getEnclosingElement().getSimpleName().toString()))
                      .containsExactly("HierarchicalProperties", "HierarchicalPropertiesParent",
                              "HierarchicalPropertiesParent");
              assertThat(resolver.resolve(type, null)
                      .map((descriptor) -> descriptor.resolveItemMetadata("test", metadataEnv))
                      .map(ItemMetadata::getDefaultValue)).containsExactly("three", "two", "one");
            });
  }

  @Test
  void propertiesWithLombokGetterSetterAtClassLevel() {
    process(LombokSimpleProperties.class, propertyNames(
            (stream) -> assertThat(stream).containsExactly("name", "description", "counter", "number", "items")));
  }

  @Test
  void propertiesWithLombokGetterSetterAtFieldLevel() {
    process(LombokExplicitProperties.class, propertyNames(
            (stream) -> assertThat(stream).containsExactly("name", "description", "counter", "number", "items")));
  }

  @Test
  void propertiesWithLombokDataClass() {
    process(LombokSimpleDataProperties.class, propertyNames(
            (stream) -> assertThat(stream).containsExactly("name", "description", "counter", "number", "items")));
  }

  @Test
  void propertiesWithLombokValueClass() {
    process(LombokSimpleValueProperties.class, propertyNames(
            (stream) -> assertThat(stream).containsExactly("name", "description", "counter", "number", "items")));
  }

  @Test
  void propertiesWithDeducedConstructorBinding() {
    process(ImmutableDeducedConstructorBindingProperties.class,
            propertyNames((stream) -> assertThat(stream).containsExactly("theName", "flag")));
    process(ImmutableDeducedConstructorBindingProperties.class,
            properties((stream) -> assertThat(stream).isNotEmpty()
                    .allMatch((predicate) -> predicate instanceof ConstructorParameterPropertyDescriptor)));
  }

  @Test
  void propertiesWithConstructorWithConstructorBinding() {
    process(ImmutableSimpleProperties.class, propertyNames(
            (stream) -> assertThat(stream).containsExactly("theName", "flag", "comparator", "counter")));
    process(ImmutableSimpleProperties.class, properties((stream) -> assertThat(stream).isNotEmpty()
            .allMatch((predicate) -> predicate instanceof ConstructorParameterPropertyDescriptor)));
  }

  @Test
  void propertiesWithConstructorAndClassConstructorBinding() {
    process(ImmutableClassConstructorBindingProperties.class,
            propertyNames((stream) -> assertThat(stream).containsExactly("name", "description")));
    process(ImmutableClassConstructorBindingProperties.class, properties((stream) -> assertThat(stream).isNotEmpty()
            .allMatch((predicate) -> predicate instanceof ConstructorParameterPropertyDescriptor)));
  }

  @Test
  void propertiesWithAutowiredConstructor() {
    process(AutowiredProperties.class, propertyNames((stream) -> assertThat(stream).containsExactly("theName")));
    process(AutowiredProperties.class, properties((stream) -> assertThat(stream).isNotEmpty()
            .allMatch((predicate) -> predicate instanceof JavaBeanPropertyDescriptor)));
  }

  @Test
  void propertiesWithMultiConstructor() {
    process(ImmutableMultiConstructorProperties.class,
            propertyNames((stream) -> assertThat(stream).containsExactly("name", "description")));
    process(ImmutableMultiConstructorProperties.class, properties((stream) -> assertThat(stream).isNotEmpty()
            .allMatch((predicate) -> predicate instanceof ConstructorParameterPropertyDescriptor)));
  }

  @Test
  void propertiesWithMultiConstructorNoDirective() {
    process(TwoConstructorsExample.class, propertyNames((stream) -> assertThat(stream).containsExactly("name")));
    process(TwoConstructorsExample.class,
            properties((stream) -> assertThat(stream).element(0).isInstanceOf(JavaBeanPropertyDescriptor.class)));
  }

  @Test
  void propertiesWithNameAnnotationParameter() {
    process(ImmutableNameAnnotationProperties.class,
            propertyNames((stream) -> assertThat(stream).containsExactly("import")));
  }

  private BiConsumer<TypeElement, MetadataGenerationEnvironment> properties(
          Consumer<Stream<PropertyDescriptor<?>>> stream) {
    return (element, metadataEnv) -> {
      PropertyDescriptorResolver resolver = new PropertyDescriptorResolver(metadataEnv);
      stream.accept(resolver.resolve(element, null));
    };
  }

  private BiConsumer<TypeElement, MetadataGenerationEnvironment> propertyNames(Consumer<Stream<String>> stream) {
    return properties((result) -> stream.accept(result.map(PropertyDescriptor::getName)));
  }

  private void process(Class<?> target, BiConsumer<TypeElement, MetadataGenerationEnvironment> consumer) {
    process(target, Collections.emptyList(), consumer);
  }

  private void process(Class<?> target, Collection<Class<?>> additionalClasses,
          BiConsumer<TypeElement, MetadataGenerationEnvironment> consumer) {
    BiConsumer<RoundEnvironmentTester, MetadataGenerationEnvironment> internalConsumer = (roundEnv,
            metadataEnv) -> {
      TypeElement element = roundEnv.getRootElement(target);
      consumer.accept(element, metadataEnv);
    };
    TestableAnnotationProcessor<MetadataGenerationEnvironment> processor = new TestableAnnotationProcessor<>(
            internalConsumer, new MetadataGenerationEnvironmentFactory());
    SourceFile targetSource = SourceFile.forTestClass(target);
    List<SourceFile> additionalSource = additionalClasses.stream().map(SourceFile::forTestClass).toList();
    TestCompiler compiler = TestCompiler.forSystem()
            .withProcessors(processor)
            .withSources(targetSource)
            .withSources(additionalSource);
    compiler.compile((compiled) -> {
    });
  }

}

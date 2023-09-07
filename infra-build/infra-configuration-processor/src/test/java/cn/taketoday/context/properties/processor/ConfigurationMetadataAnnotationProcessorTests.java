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

import cn.taketoday.context.properties.processor.metadata.ConfigurationMetadata;
import cn.taketoday.context.properties.processor.metadata.ItemMetadata;
import cn.taketoday.context.properties.processor.metadata.Metadata;
import cn.taketoday.context.properties.sample.record.RecordWithGetter;
import cn.taketoday.context.properties.sample.recursive.RecursiveProperties;
import cn.taketoday.context.properties.sample.simple.ClassWithNestedProperties;
import cn.taketoday.context.properties.sample.simple.DeprecatedFieldSingleProperty;
import cn.taketoday.context.properties.sample.simple.DeprecatedProperties;
import cn.taketoday.context.properties.sample.simple.DeprecatedRecord;
import cn.taketoday.context.properties.sample.simple.DeprecatedSingleProperty;
import cn.taketoday.context.properties.sample.simple.DescriptionProperties;
import cn.taketoday.context.properties.sample.simple.HierarchicalProperties;
import cn.taketoday.context.properties.sample.simple.HierarchicalPropertiesGrandparent;
import cn.taketoday.context.properties.sample.simple.HierarchicalPropertiesParent;
import cn.taketoday.context.properties.sample.simple.InnerClassWithPrivateConstructor;
import cn.taketoday.context.properties.sample.simple.NotAnnotated;
import cn.taketoday.context.properties.sample.simple.SimpleArrayProperties;
import cn.taketoday.context.properties.sample.simple.SimpleCollectionProperties;
import cn.taketoday.context.properties.sample.simple.SimplePrefixValueProperties;
import cn.taketoday.context.properties.sample.simple.SimpleProperties;
import cn.taketoday.context.properties.sample.simple.SimpleTypeProperties;
import cn.taketoday.context.properties.sample.specific.AnnotatedGetter;
import cn.taketoday.context.properties.sample.specific.BoxingPojo;
import cn.taketoday.context.properties.sample.specific.BuilderPojo;
import cn.taketoday.context.properties.sample.specific.DeprecatedLessPreciseTypePojo;
import cn.taketoday.context.properties.sample.specific.DeprecatedUnrelatedMethodPojo;
import cn.taketoday.context.properties.sample.specific.DoubleRegistrationProperties;
import cn.taketoday.context.properties.sample.specific.EmptyDefaultValueProperties;
import cn.taketoday.context.properties.sample.specific.ExcludedTypesPojo;
import cn.taketoday.context.properties.sample.specific.InnerClassAnnotatedGetterConfig;
import cn.taketoday.context.properties.sample.specific.InnerClassHierarchicalProperties;
import cn.taketoday.context.properties.sample.specific.InnerClassProperties;
import cn.taketoday.context.properties.sample.specific.InnerClassRootConfig;
import cn.taketoday.context.properties.sample.specific.InvalidAccessorProperties;
import cn.taketoday.context.properties.sample.specific.InvalidDefaultValueCharacterProperties;
import cn.taketoday.context.properties.sample.specific.InvalidDefaultValueFloatingPointProperties;
import cn.taketoday.context.properties.sample.specific.InvalidDefaultValueNumberProperties;
import cn.taketoday.context.properties.sample.specific.InvalidDoubleRegistrationProperties;
import cn.taketoday.context.properties.sample.specific.SimplePojo;
import cn.taketoday.context.properties.sample.specific.StaticAccessor;
import cn.taketoday.core.test.tools.CompilationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link ConfigurationMetadataAnnotationProcessor}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Kris De Volder
 * @author Jonas Keßler
 * @author Pavel Anisimov
 * @author Scott Frederick
 * @author Moritz Halbritter
 */
class ConfigurationMetadataAnnotationProcessorTests extends AbstractMetadataGenerationTests {

  @Test
  void supportedAnnotations() {
    assertThat(new ConfigurationMetadataAnnotationProcessor().getSupportedAnnotationTypes())
            .containsExactlyInAnyOrder("cn.taketoday.context.annotation.config.AutoConfiguration",
                    "cn.taketoday.context.properties.ConfigurationProperties",
                    "cn.taketoday.context.annotation.config.DisableDIAutoConfiguration",
                    "cn.taketoday.context.annotation.Configuration");
  }

  @Test
  void notAnnotated() {
    ConfigurationMetadata metadata = compile(NotAnnotated.class);
    assertThat(metadata).isNull();
  }

  @Test
  void simpleProperties() {
    ConfigurationMetadata metadata = compile(SimpleProperties.class);
    assertThat(metadata).has(Metadata.withGroup("simple").fromSource(SimpleProperties.class));
    assertThat(metadata).has(Metadata.withProperty("simple.the-name", String.class)
            .fromSource(SimpleProperties.class)
            .withDescription("The name of this simple properties.")
            .withDefaultValue("boot")
            .withDeprecation());
    assertThat(metadata).has(Metadata.withProperty("simple.flag", Boolean.class)
            .withDefaultValue(false)
            .fromSource(SimpleProperties.class)
            .withDescription("A simple flag.")
            .withDeprecation());
    assertThat(metadata).has(Metadata.withProperty("simple.comparator"));
    assertThat(metadata).doesNotHave(Metadata.withProperty("simple.counter"));
    assertThat(metadata).doesNotHave(Metadata.withProperty("simple.size"));
  }

  @Test
  void simplePrefixValueProperties() {
    ConfigurationMetadata metadata = compile(SimplePrefixValueProperties.class);
    assertThat(metadata).has(Metadata.withGroup("simple").fromSource(SimplePrefixValueProperties.class));
    assertThat(metadata)
            .has(Metadata.withProperty("simple.name", String.class).fromSource(SimplePrefixValueProperties.class));
  }

  @Test
  void simpleTypeProperties() {
    ConfigurationMetadata metadata = compile(SimpleTypeProperties.class);
    assertThat(metadata).has(Metadata.withGroup("simple.type").fromSource(SimpleTypeProperties.class));
    assertThat(metadata).has(Metadata.withProperty("simple.type.my-string", String.class));
    assertThat(metadata).has(Metadata.withProperty("simple.type.my-byte", Byte.class));
    assertThat(metadata)
            .has(Metadata.withProperty("simple.type.my-primitive-byte", Byte.class).withDefaultValue(0));
    assertThat(metadata).has(Metadata.withProperty("simple.type.my-char", Character.class));
    assertThat(metadata).has(Metadata.withProperty("simple.type.my-primitive-char", Character.class));
    assertThat(metadata).has(Metadata.withProperty("simple.type.my-boolean", Boolean.class));
    assertThat(metadata)
            .has(Metadata.withProperty("simple.type.my-primitive-boolean", Boolean.class).withDefaultValue(false));
    assertThat(metadata).has(Metadata.withProperty("simple.type.my-short", Short.class));
    assertThat(metadata)
            .has(Metadata.withProperty("simple.type.my-primitive-short", Short.class).withDefaultValue(0));
    assertThat(metadata).has(Metadata.withProperty("simple.type.my-integer", Integer.class));
    assertThat(metadata)
            .has(Metadata.withProperty("simple.type.my-primitive-integer", Integer.class).withDefaultValue(0));
    assertThat(metadata).has(Metadata.withProperty("simple.type.my-long", Long.class));
    assertThat(metadata)
            .has(Metadata.withProperty("simple.type.my-primitive-long", Long.class).withDefaultValue(0));
    assertThat(metadata).has(Metadata.withProperty("simple.type.my-double", Double.class));
    assertThat(metadata).has(Metadata.withProperty("simple.type.my-primitive-double", Double.class));
    assertThat(metadata).has(Metadata.withProperty("simple.type.my-float", Float.class));
    assertThat(metadata).has(Metadata.withProperty("simple.type.my-primitive-float", Float.class));
    assertThat(metadata.getItems()).hasSize(18);
  }

  @Test
  void hierarchicalProperties() {
    ConfigurationMetadata metadata = compile(HierarchicalProperties.class, HierarchicalPropertiesParent.class,
            HierarchicalPropertiesGrandparent.class);
    assertThat(metadata).has(Metadata.withGroup("hierarchical").fromSource(HierarchicalProperties.class));
    assertThat(metadata).has(Metadata.withProperty("hierarchical.first", String.class)
            .withDefaultValue("one")
            .fromSource(HierarchicalProperties.class));
    assertThat(metadata).has(Metadata.withProperty("hierarchical.second", String.class)
            .withDefaultValue("two")
            .fromSource(HierarchicalProperties.class));
    assertThat(metadata).has(Metadata.withProperty("hierarchical.third", String.class)
            .withDefaultValue("three")
            .fromSource(HierarchicalProperties.class));
  }

  @Test
  void descriptionProperties() {
    ConfigurationMetadata metadata = compile(DescriptionProperties.class);
    assertThat(metadata).has(Metadata.withGroup("description").fromSource(DescriptionProperties.class));
    assertThat(metadata).has(Metadata.withProperty("description.simple", String.class)
            .fromSource(DescriptionProperties.class)
            .withDescription("A simple description."));
    assertThat(metadata).has(Metadata.withProperty("description.multi-line", String.class)
            .fromSource(DescriptionProperties.class)
            .withDescription(
                    "This is a lengthy description that spans across multiple lines to showcase that the line separators are cleaned automatically."));
  }

  @Test
  @SuppressWarnings("deprecation")
  void deprecatedProperties() {
    Class<?> type = DeprecatedProperties.class;
    ConfigurationMetadata metadata = compile(type);
    assertThat(metadata).has(Metadata.withGroup("deprecated").fromSource(type));
    assertThat(metadata)
            .has(Metadata.withProperty("deprecated.name", String.class).fromSource(type).withDeprecation());
    assertThat(metadata)
            .has(Metadata.withProperty("deprecated.description", String.class).fromSource(type).withDeprecation());
  }

  @Test
  void singleDeprecatedProperty() {
    Class<?> type = DeprecatedSingleProperty.class;
    ConfigurationMetadata metadata = compile(type);
    assertThat(metadata).has(Metadata.withGroup("singledeprecated").fromSource(type));
    assertThat(metadata).has(Metadata.withProperty("singledeprecated.new-name", String.class).fromSource(type));
    assertThat(metadata).has(Metadata.withProperty("singledeprecated.name", String.class)
            .fromSource(type)
            .withDeprecation("renamed", "singledeprecated.new-name", "1.2.3"));
  }

  @Test
  void singleDeprecatedFieldProperty() {
    Class<?> type = DeprecatedFieldSingleProperty.class;
    ConfigurationMetadata metadata = compile(type);
    assertThat(metadata).has(Metadata.withGroup("singlefielddeprecated").fromSource(type));
    assertThat(metadata)
            .has(Metadata.withProperty("singlefielddeprecated.name", String.class).fromSource(type).withDeprecation());
  }

  @Test
  void deprecatedOnUnrelatedSetter() {
    Class<?> type = DeprecatedUnrelatedMethodPojo.class;
    ConfigurationMetadata metadata = compile(type);
    assertThat(metadata).has(Metadata.withGroup("not.deprecated").fromSource(type));
    assertThat(metadata)
            .has(Metadata.withProperty("not.deprecated.counter", Integer.class).withNoDeprecation().fromSource(type));
    assertThat(metadata).has(Metadata.withProperty("not.deprecated.flag", Boolean.class)
            .withDefaultValue(false)
            .withNoDeprecation()
            .fromSource(type));
  }

  @Test
  void deprecatedWithLessPreciseType() {
    Class<?> type = DeprecatedLessPreciseTypePojo.class;
    ConfigurationMetadata metadata = compile(type);
    assertThat(metadata).has(Metadata.withGroup("not.deprecated").fromSource(type));
    assertThat(metadata).has(Metadata.withProperty("not.deprecated.flag", Boolean.class)
            .withDefaultValue(false)
            .withNoDeprecation()
            .fromSource(type));
  }

  @Test
  void deprecatedPropertyOnRecord() {
    Class<?> type = DeprecatedRecord.class;
    ConfigurationMetadata metadata = compile(type);
    assertThat(metadata).has(Metadata.withGroup("deprecated-record").fromSource(type));
    assertThat(metadata).has(Metadata.withProperty("deprecated-record.alpha", String.class)
            .fromSource(type)
            .withDeprecation("some-reason", null, null));
    assertThat(metadata).has(Metadata.withProperty("deprecated-record.bravo", String.class).fromSource(type));
  }

  @Test
  void typBoxing() {
    Class<?> type = BoxingPojo.class;
    ConfigurationMetadata metadata = compile(type);
    assertThat(metadata).has(Metadata.withGroup("boxing").fromSource(type));
    assertThat(metadata)
            .has(Metadata.withProperty("boxing.flag", Boolean.class).withDefaultValue(false).fromSource(type));
    assertThat(metadata).has(Metadata.withProperty("boxing.another-flag", Boolean.class).fromSource(type));
    assertThat(metadata).has(Metadata.withProperty("boxing.counter", Integer.class).fromSource(type));
  }

  @Test
  void parseCollectionConfig() {
    ConfigurationMetadata metadata = compile(SimpleCollectionProperties.class);
    // getter and setter
    assertThat(metadata).has(Metadata.withProperty("collection.integers-to-names",
            "java.util.Map<java.lang.Integer,java.lang.String>"));
    assertThat(metadata).has(Metadata.withProperty("collection.longs", "java.util.Collection<java.lang.Long>"));
    assertThat(metadata).has(Metadata.withProperty("collection.floats", "java.util.List<java.lang.Float>"));
    // getter only
    assertThat(metadata).has(Metadata.withProperty("collection.names-to-integers",
            "java.util.Map<java.lang.String,java.lang.Integer>"));
    assertThat(metadata).has(Metadata.withProperty("collection.bytes", "java.util.Collection<java.lang.Byte>"));
    assertThat(metadata).has(Metadata.withProperty("collection.doubles", "java.util.List<java.lang.Double>"));
    assertThat(metadata).has(Metadata.withProperty("collection.names-to-holders",
            "java.util.Map<java.lang.String,cn.taketoday.context.properties.sample.simple.SimpleCollectionProperties$Holder<java.lang.String>>"));
  }

  @Test
  void parseArrayConfig() {
    ConfigurationMetadata metadata = compile(SimpleArrayProperties.class);
    assertThat(metadata).has(Metadata.withGroup("array").ofType(SimpleArrayProperties.class));
    assertThat(metadata).has(Metadata.withProperty("array.primitive", "java.lang.Integer[]"));
    assertThat(metadata).has(Metadata.withProperty("array.simple", "java.lang.String[]"));
    assertThat(metadata).has(Metadata.withProperty("array.inner",
            "cn.taketoday.context.properties.sample.simple.SimpleArrayProperties$Holder[]"));
    assertThat(metadata)
            .has(Metadata.withProperty("array.name-to-integer", "java.util.Map<java.lang.String,java.lang.Integer>[]"));
    assertThat(metadata.getItems()).hasSize(5);
  }

  @Test
  void annotatedGetter() {
    ConfigurationMetadata metadata = compile(AnnotatedGetter.class);
    assertThat(metadata).has(Metadata.withGroup("specific").fromSource(AnnotatedGetter.class));
    assertThat(metadata)
            .has(Metadata.withProperty("specific.name", String.class).fromSource(AnnotatedGetter.class));
  }

  @Test
  void staticAccessor() {
    ConfigurationMetadata metadata = compile(StaticAccessor.class);
    assertThat(metadata).has(Metadata.withGroup("specific").fromSource(StaticAccessor.class));
    assertThat(metadata).has(Metadata.withProperty("specific.counter", Integer.class)
            .fromSource(StaticAccessor.class)
            .withDefaultValue(42));
    assertThat(metadata)
            .doesNotHave(Metadata.withProperty("specific.name", String.class).fromSource(StaticAccessor.class));
    assertThat(metadata.getItems()).hasSize(2);
  }

  @Test
  void innerClassRootConfig() {
    ConfigurationMetadata metadata = compile(InnerClassRootConfig.class);
    assertThat(metadata).has(Metadata.withProperty("config.name"));
  }

  @Test
  void innerClassProperties() {
    ConfigurationMetadata metadata = compile(InnerClassProperties.class);
    assertThat(metadata).has(Metadata.withGroup("config").fromSource(InnerClassProperties.class));
    assertThat(metadata).has(Metadata.withGroup("config.first")
            .ofType(InnerClassProperties.Foo.class)
            .fromSource(InnerClassProperties.class));
    assertThat(metadata).has(Metadata.withProperty("config.first.name"));
    assertThat(metadata).has(Metadata.withProperty("config.first.bar.name"));
    assertThat(metadata).has(Metadata.withGroup("config.the-second", InnerClassProperties.Foo.class)
            .fromSource(InnerClassProperties.class));
    assertThat(metadata).has(Metadata.withProperty("config.the-second.name"));
    assertThat(metadata).has(Metadata.withProperty("config.the-second.bar.name"));
    assertThat(metadata)
            .has(Metadata.withGroup("config.third").ofType(SimplePojo.class).fromSource(InnerClassProperties.class));
    assertThat(metadata).has(Metadata.withProperty("config.third.value"));
    assertThat(metadata).has(Metadata.withProperty("config.fourth"));
    assertThat(metadata).isNotEqualTo(Metadata.withGroup("config.fourth"));
  }

  @Test
  void innerClassPropertiesHierarchical() {
    ConfigurationMetadata metadata = compile(InnerClassHierarchicalProperties.class);
    assertThat(metadata).has(Metadata.withGroup("config.foo").ofType(InnerClassHierarchicalProperties.Foo.class));
    assertThat(metadata)
            .has(Metadata.withGroup("config.foo.bar").ofType(InnerClassHierarchicalProperties.Bar.class));
    assertThat(metadata)
            .has(Metadata.withGroup("config.foo.bar.baz").ofType(InnerClassHierarchicalProperties.Foo.Baz.class));
    assertThat(metadata).has(Metadata.withProperty("config.foo.bar.baz.blah"));
    assertThat(metadata).has(Metadata.withProperty("config.foo.bar.bling"));
  }

  @Test
  void innerClassAnnotatedGetterConfig() {
    ConfigurationMetadata metadata = compile(InnerClassAnnotatedGetterConfig.class);
    assertThat(metadata).has(Metadata.withProperty("specific.value"));
    assertThat(metadata).has(Metadata.withProperty("foo.name"));
    assertThat(metadata).isNotEqualTo(Metadata.withProperty("specific.foo"));
  }

  @Test
  void nestedClassChildProperties() {
    ConfigurationMetadata metadata = compile(ClassWithNestedProperties.class);
    assertThat(metadata)
            .has(Metadata.withGroup("nestedChildProps").fromSource(ClassWithNestedProperties.NestedChildClass.class));
    assertThat(metadata).has(Metadata.withProperty("nestedChildProps.child-class-property", Integer.class)
            .fromSource(ClassWithNestedProperties.NestedChildClass.class)
            .withDefaultValue(20));
    assertThat(metadata).has(Metadata.withProperty("nestedChildProps.parent-class-property", Integer.class)
            .fromSource(ClassWithNestedProperties.NestedChildClass.class)
            .withDefaultValue(10));
  }

  @Test
  void builderPojo() {
    ConfigurationMetadata metadata = compile(BuilderPojo.class);
    assertThat(metadata).has(Metadata.withProperty("builder.name"));
  }

  @Test
  void excludedTypesPojo() {
    ConfigurationMetadata metadata = compile(ExcludedTypesPojo.class);
    assertThat(metadata).has(Metadata.withProperty("excluded.name"));
    assertThat(metadata).isNotEqualTo(Metadata.withProperty("excluded.class-loader"));
    assertThat(metadata).isNotEqualTo(Metadata.withProperty("excluded.data-source"));
    assertThat(metadata).isNotEqualTo(Metadata.withProperty("excluded.print-writer"));
    assertThat(metadata).isNotEqualTo(Metadata.withProperty("excluded.writer"));
    assertThat(metadata).isNotEqualTo(Metadata.withProperty("excluded.writer-array"));
  }

  @Test
  void invalidAccessor() {
    ConfigurationMetadata metadata = compile(InvalidAccessorProperties.class);
    assertThat(metadata).has(Metadata.withGroup("config"));
    assertThat(metadata.getItems()).hasSize(1);
  }

  @Test
  void doubleRegistration() {
    ConfigurationMetadata metadata = compile(DoubleRegistrationProperties.class);
    assertThat(metadata).has(Metadata.withGroup("one"));
    assertThat(metadata).has(Metadata.withGroup("two"));
    assertThat(metadata).has(Metadata.withProperty("one.value"));
    assertThat(metadata).has(Metadata.withProperty("two.value"));
    assertThat(metadata.getItems()).hasSize(4);
  }

  @Test
  void invalidDoubleRegistration() {
    assertThatExceptionOfType(CompilationException.class)
            .isThrownBy(() -> compile(InvalidDoubleRegistrationProperties.class))
            .withMessageContaining("Unable to compile source");
  }

  @Test
  void constructorParameterPropertyWithInvalidDefaultValueOnNumber() {
    assertThatExceptionOfType(CompilationException.class)
            .isThrownBy(() -> compile(InvalidDefaultValueNumberProperties.class))
            .withMessageContaining("Unable to compile source");
  }

  @Test
  void constructorParameterPropertyWithInvalidDefaultValueOnFloatingPoint() {
    assertThatExceptionOfType(CompilationException.class)
            .isThrownBy(() -> compile(InvalidDefaultValueFloatingPointProperties.class))
            .withMessageContaining("Unable to compile source");
  }

  @Test
  void constructorParameterPropertyWithInvalidDefaultValueOnCharacter() {
    assertThatExceptionOfType(CompilationException.class)
            .isThrownBy(() -> compile(InvalidDefaultValueCharacterProperties.class))
            .withMessageContaining("Unable to compile source");
  }

  @Test
  void constructorParameterPropertyWithEmptyDefaultValueOnProperty() {
    ConfigurationMetadata metadata = compile(EmptyDefaultValueProperties.class);
    assertThat(metadata).has(Metadata.withProperty("test.name"));
    ItemMetadata nameMetadata = metadata.getItems()
            .stream()
            .filter((item) -> item.getName().equals("test.name"))
            .findFirst()
            .get();
    assertThat(nameMetadata.getDefaultValue()).isNull();
  }

  @Test
  void recursivePropertiesDoNotCauseAStackOverflow() {
    compile(RecursiveProperties.class);
  }

  @Test
  void recordProperties() {
    String source = """
            @cn.taketoday.context.properties.sample.ConfigurationProperties("implicit")
            public record ExampleRecord(String someString, Integer someInteger) {
            }
            """;
    ConfigurationMetadata metadata = compile(source);
    assertThat(metadata).has(Metadata.withProperty("implicit.some-string"));
    assertThat(metadata).has(Metadata.withProperty("implicit.some-integer"));
  }

  @Test
  void recordPropertiesWithDefaultValues() {
    String source = """
            @cn.taketoday.context.properties.sample.ConfigurationProperties("record.defaults")
            public record ExampleRecord(
            	@cn.taketoday.context.properties.sample.DefaultValue("An1s9n") String someString,
            	@cn.taketoday.context.properties.sample.DefaultValue("594") Integer someInteger) {
            }
            """;
    ConfigurationMetadata metadata = compile(source);
    assertThat(metadata)
            .has(Metadata.withProperty("record.defaults.some-string", String.class).withDefaultValue("An1s9n"));
    assertThat(metadata)
            .has(Metadata.withProperty("record.defaults.some-integer", Integer.class).withDefaultValue(594));
  }

  @Test
  void multiConstructorRecordProperties() {
    String source = """
            @cn.taketoday.context.properties.sample.ConfigurationProperties("multi")
            public record ExampleRecord(String someString, Integer someInteger) {
            	@cn.taketoday.context.properties.sample.ConstructorBinding
            	public ExampleRecord(String someString) {
            		this(someString, 42);
            	}
            	public ExampleRecord(Integer someInteger) {
            		this("someString", someInteger);
            	}
            }
            """;
    ConfigurationMetadata metadata = compile(source);
    assertThat(metadata).has(Metadata.withProperty("multi.some-string"));
    assertThat(metadata).doesNotHave(Metadata.withProperty("multi.some-integer"));
  }

  @Test
  void innerClassWithPrivateConstructor() {
    ConfigurationMetadata metadata = compile(InnerClassWithPrivateConstructor.class);
    assertThat(metadata).has(Metadata.withProperty("config.nested.name"));
    assertThat(metadata).doesNotHave(Metadata.withProperty("config.nested.ignored"));
  }

  @Test
  void recordWithGetter() {
    ConfigurationMetadata metadata = compile(RecordWithGetter.class);
    assertThat(metadata).has(Metadata.withProperty("record-with-getter.alpha"));
    assertThat(metadata).doesNotHave(Metadata.withProperty("record-with-getter.bravo"));
  }

}

/*
 * Copyright 2002-present the original author or authors.
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

package infra.core.annotation;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link AnnotationAttributes}.
 *
 * @author Chris Beams
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 4.0
 */
class AnnotationAttributesTests {

  private AnnotationAttributes attributes = new AnnotationAttributes();

  @Test
  void typeSafeAttributeAccess() {
    AnnotationAttributes nestedAttributes = new AnnotationAttributes();
    nestedAttributes.put("value", 10);
    nestedAttributes.put("name", "algernon");

    attributes.put("name", "dave");
    attributes.put("names", new String[] { "dave", "frank", "hal" });
    attributes.put("bool1", true);
    attributes.put("bool2", false);
    attributes.put("color", Color.RED);
    attributes.put("class", Integer.class);
    attributes.put("classes", new Class<?>[] { Number.class, Short.class, Integer.class });
    attributes.put("number", 42);
    attributes.put("anno", nestedAttributes);
    attributes.put("annoArray", new AnnotationAttributes[] { nestedAttributes });

    assertThat(attributes.getString("name")).isEqualTo("dave");
    assertThat(attributes.getStringArray("names")).isEqualTo(new String[] { "dave", "frank", "hal" });
    assertThat(attributes.getBoolean("bool1")).isEqualTo(true);
    assertThat(attributes.getBoolean("bool2")).isEqualTo(false);
    assertThat(attributes.<Color>getEnum("color")).isEqualTo(Color.RED);
    assertThat(attributes.getClass("class").equals(Integer.class)).isTrue();
    assertThat(attributes.getClassArray("classes")).isEqualTo(new Class<?>[] { Number.class, Short.class, Integer.class });
    assertThat(attributes.<Integer>getNumber("number")).isEqualTo(42);
    assertThat(attributes.getAnnotation("anno").<Integer>getNumber("value")).isEqualTo(10);
    assertThat(attributes.getAnnotationArray("annoArray")[0].getString("name")).isEqualTo("algernon");

  }

  @Test
  void unresolvableClassWithClassNotFoundException() throws Exception {
    attributes.put("unresolvableClass", new ClassNotFoundException("myclass"));
    assertThatIllegalArgumentException()
            .isThrownBy(() -> attributes.getClass("unresolvableClass"))
            .withMessageContaining("myclass")
            .withCauseInstanceOf(ClassNotFoundException.class);
  }

  @Test
  void unresolvableClassWithLinkageError() throws Exception {
    attributes.put("unresolvableClass", new LinkageError("myclass"));
    assertThatIllegalArgumentException()
            .isThrownBy(() -> attributes.getClass("unresolvableClass"))
            .withMessageContaining("myclass")
            .withCauseInstanceOf(LinkageError.class);
  }

  @Test
  void singleElementToSingleElementArrayConversionSupport() throws Exception {
    Filter filter = FilteredClass.class.getAnnotation(Filter.class);

    AnnotationAttributes nestedAttributes = new AnnotationAttributes();
    nestedAttributes.put("name", "Dilbert");

    // Store single elements
    attributes.put("names", "Dogbert");
    attributes.put("classes", Number.class);
    attributes.put("nestedAttributes", nestedAttributes);
    attributes.put("filters", filter);

    // Get back arrays of single elements
    assertThat(attributes.getStringArray("names")).isEqualTo(new String[] { "Dogbert" });
    assertThat(attributes.getClassArray("classes")).isEqualTo(new Class<?>[] { Number.class });

    AnnotationAttributes[] array = attributes.getAnnotationArray("nestedAttributes");
    assertThat(array).isNotNull();
    assertThat(array.length).isEqualTo(1);
    assertThat(array[0].getString("name")).isEqualTo("Dilbert");

    Filter[] filters = attributes.getAnnotationArray("filters", Filter.class);
    assertThat(filters).isNotNull();
    assertThat(filters.length).isEqualTo(1);
    assertThat(filters[0].pattern()).isEqualTo("foo");
  }

  @Test
  void nestedAnnotations() throws Exception {
    Filter filter = FilteredClass.class.getAnnotation(Filter.class);

    attributes.put("filter", filter);
    attributes.put("filters", new Filter[] { filter, filter });

    Filter retrievedFilter = attributes.getAnnotation("filter", Filter.class);
    assertThat(retrievedFilter).isEqualTo(filter);
    assertThat(retrievedFilter.pattern()).isEqualTo("foo");

    Filter[] retrievedFilters = attributes.getAnnotationArray("filters", Filter.class);
    assertThat(retrievedFilters).isNotNull();
    assertThat(retrievedFilters.length).isEqualTo(2);
    assertThat(retrievedFilters[1].pattern()).isEqualTo("foo");
  }

  @Test
  void getEnumWithNullAttributeName() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> attributes.getEnum(null))
            .withMessageContaining("must not be null or empty");
  }

  @Test
  void getEnumWithEmptyAttributeName() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> attributes.getEnum(""))
            .withMessageContaining("must not be null or empty");
  }

  @Test
  void getEnumWithUnknownAttributeName() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> attributes.getEnum("bogus"))
            .withMessageContaining("Attribute 'bogus' not found");
  }

  @Test
  void getEnumWithTypeMismatch() {
    attributes.put("color", "RED");
    assertThatIllegalArgumentException()
            .isThrownBy(() -> attributes.getEnum("color"))
            .withMessageContaining("Attribute 'color' is of type String, but Enum was expected");
  }

  @Test
  void getAliasedStringWithImplicitAliases() {
    String value = "metaverse";
    List<String> aliases = Arrays.asList("value", "location1", "location2", "location3", "xmlFile", "groovyScript");

    attributes = new AnnotationAttributes(AnnotationUtilsTests.ImplicitAliasesContextConfig.class);
    attributes.put("value", value);
    AnnotationUtils.postProcessAnnotationAttributes(null, attributes, false);
    aliases.stream().forEach(alias -> assertThat(attributes.getString(alias)).isEqualTo(value));

    attributes = new AnnotationAttributes(AnnotationUtilsTests.ImplicitAliasesContextConfig.class);
    attributes.put("location1", value);
    AnnotationUtils.postProcessAnnotationAttributes(null, attributes, false);
    aliases.stream().forEach(alias -> assertThat(attributes.getString(alias)).isEqualTo(value));

    attributes = new AnnotationAttributes(AnnotationUtilsTests.ImplicitAliasesContextConfig.class);
    attributes.put("value", value);
    attributes.put("location1", value);
    attributes.put("xmlFile", value);
    attributes.put("groovyScript", value);
    AnnotationUtils.postProcessAnnotationAttributes(null, attributes, false);
    aliases.stream().forEach(alias -> assertThat(attributes.getString(alias)).isEqualTo(value));
  }

  @Test
  void getAliasedStringArrayWithImplicitAliases() {
    String[] value = new String[] { "test.xml" };
    List<String> aliases = Arrays.asList("value", "location1", "location2", "location3", "xmlFile", "groovyScript");

    attributes = new AnnotationAttributes(AnnotationUtilsTests.ImplicitAliasesContextConfig.class);
    attributes.put("location1", value);
    AnnotationUtils.postProcessAnnotationAttributes(null, attributes, false);
    aliases.stream().forEach(alias -> assertThat(attributes.getStringArray(alias)).isEqualTo(value));

    attributes = new AnnotationAttributes(AnnotationUtilsTests.ImplicitAliasesContextConfig.class);
    attributes.put("value", value);
    AnnotationUtils.postProcessAnnotationAttributes(null, attributes, false);
    aliases.stream().forEach(alias -> assertThat(attributes.getStringArray(alias)).isEqualTo(value));

    attributes = new AnnotationAttributes(AnnotationUtilsTests.ImplicitAliasesContextConfig.class);
    attributes.put("location1", value);
    attributes.put("value", value);
    AnnotationUtils.postProcessAnnotationAttributes(null, attributes, false);
    aliases.stream().forEach(alias -> assertThat(attributes.getStringArray(alias)).isEqualTo(value));

    attributes = new AnnotationAttributes(AnnotationUtilsTests.ImplicitAliasesContextConfig.class);
    attributes.put("location1", value);
    AnnotationUtils.registerDefaultValues(attributes);
    AnnotationUtils.postProcessAnnotationAttributes(null, attributes, false);
    aliases.stream().forEach(alias -> assertThat(attributes.getStringArray(alias)).isEqualTo(value));

    attributes = new AnnotationAttributes(AnnotationUtilsTests.ImplicitAliasesContextConfig.class);
    attributes.put("value", value);
    AnnotationUtils.registerDefaultValues(attributes);
    AnnotationUtils.postProcessAnnotationAttributes(null, attributes, false);
    aliases.stream().forEach(alias -> assertThat(attributes.getStringArray(alias)).isEqualTo(value));

    attributes = new AnnotationAttributes(AnnotationUtilsTests.ImplicitAliasesContextConfig.class);
    AnnotationUtils.registerDefaultValues(attributes);
    AnnotationUtils.postProcessAnnotationAttributes(null, attributes, false);
    aliases.stream().forEach(alias -> assertThat(attributes.getStringArray(alias)).isEqualTo(new String[] { "" }));
  }

  @Test
  void fromMapWithNullMap() {
    AnnotationAttributes result = AnnotationAttributes.fromMap(null);
    assertThat(result).isNull();
  }

  @Test
  void fromMapWithExistingAnnotationAttributes() {
    AnnotationAttributes original = new AnnotationAttributes();
    original.put("key", "value");

    AnnotationAttributes result = AnnotationAttributes.fromMap(original);
    assertThat(result).isSameAs(original);
  }

  @Test
  void fromMapWithRegularMap() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("key1", "value1");
    map.put("key2", 42);

    AnnotationAttributes result = AnnotationAttributes.fromMap(map);
    assertThat(result).isNotNull();
    assertThat(result.getString("key1")).isEqualTo("value1");
    assertThat((Integer) result.getNumber("key2")).isEqualTo(42);
  }

  @Test
  void constructorWithInitialCapacity() {
    AnnotationAttributes attributes = new AnnotationAttributes(16);
    attributes.put("key", "value");

    assertThat(attributes.getString("key")).isEqualTo("value");
  }

  @Test
  void constructorWithMap() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("key1", "value1");
    map.put("key2", 123);

    AnnotationAttributes attributes = new AnnotationAttributes(map);
    assertThat(attributes.getString("key1")).isEqualTo("value1");
    assertThat((Integer) attributes.getNumber("key2")).isEqualTo(123);
  }

  @Test
  void constructorWithAnnotationAttributes() {
    AnnotationAttributes original = new AnnotationAttributes();
    original.put("key1", "value1");
    original.put("key2", true);

    AnnotationAttributes copy = new AnnotationAttributes(original);
    assertThat(copy.getString("key1")).isEqualTo("value1");
    assertThat(copy.getBoolean("key2")).isEqualTo(true);
    assertThat(copy.annotationType()).isEqualTo(original.annotationType());
    assertThat(copy.displayName).isEqualTo(original.displayName);
    assertThat(copy.validated).isEqualTo(original.validated);
  }

  @Test
  void constructorWithStringAnnotationType() {
    AnnotationAttributes attributes = new AnnotationAttributes("java.lang.Deprecated", null);
    assertThat(attributes.annotationType()).isNull();
    assertThat(attributes.displayName).isEqualTo("java.lang.Deprecated");
    assertThat(attributes.validated).isFalse();
  }

  @Test
  void constructorWithStringAnnotationTypeAndClassLoader() {
    AnnotationAttributes attributes = new AnnotationAttributes("java.lang.Deprecated",
            Thread.currentThread().getContextClassLoader());
    assertThat(attributes.annotationType()).isEqualTo(Deprecated.class);
    assertThat(attributes.displayName).isEqualTo("java.lang.Deprecated");
    assertThat(attributes.validated).isFalse();
  }

  @Test
  void constructorWithAnnotationTypeAndValidated() {
    AnnotationAttributes attributes = new AnnotationAttributes(Deprecated.class, true);
    assertThat(attributes.annotationType()).isEqualTo(Deprecated.class);
    assertThat(attributes.displayName).isEqualTo("java.lang.Deprecated");
    assertThat(attributes.validated).isTrue();
  }

  @Test
  void getAnnotationType() {
    AnnotationAttributes attributes = new AnnotationAttributes(Deprecated.class);
    assertThat(attributes.annotationType()).isEqualTo(Deprecated.class);
  }

  @Test
  void getAnnotationTypeWhenUnknown() {
    AnnotationAttributes attributes = new AnnotationAttributes();
    assertThat(attributes.annotationType()).isNull();
  }

  @Test
  void getStringWithValidAttribute() {
    AnnotationAttributes attributes = new AnnotationAttributes();
    attributes.put("name", "test");

    assertThat(attributes.getString("name")).isEqualTo("test");
  }

  @Test
  void getStringWithInvalidAttributeType() {
    AnnotationAttributes attributes = new AnnotationAttributes();
    attributes.put("name", 123);

    assertThatIllegalArgumentException()
            .isThrownBy(() -> attributes.getString("name"))
            .withMessageContaining("Attribute 'name' is of type Integer, but String was expected");
  }

  @Test
  void getStringWithMissingAttribute() {
    AnnotationAttributes attributes = new AnnotationAttributes();

    assertThatIllegalArgumentException()
            .isThrownBy(() -> attributes.getString("missing"))
            .withMessageContaining("Attribute 'missing' not found");
  }

  @Test
  void getBooleanWithValidAttribute() {
    AnnotationAttributes attributes = new AnnotationAttributes();
    attributes.put("flag", true);

    assertThat(attributes.getBoolean("flag")).isTrue();
  }

  @Test
  void getNumberWithValidAttribute() {
    AnnotationAttributes attributes = new AnnotationAttributes();
    attributes.put("count", 42);

    assertThat((Integer) attributes.getNumber("count")).isEqualTo(42);
  }

  @Test
  void getClassWithValidAttribute() {
    AnnotationAttributes attributes = new AnnotationAttributes();
    attributes.put("clazz", String.class);

    assertThat(attributes.getClass("clazz")).isEqualTo(String.class);
  }

  @Test
  void getClassArrayWithSingleClass() {
    AnnotationAttributes attributes = new AnnotationAttributes();
    attributes.put("classes", String.class);

    Class<?>[] result = attributes.getClassArray("classes");
    assertThat(result).hasSize(1);
    assertThat(result[0]).isEqualTo(String.class);
  }

  @Test
  void getClassArrayWithClassArray() {
    AnnotationAttributes attributes = new AnnotationAttributes();
    attributes.put("classes", new Class<?>[] { String.class, Integer.class });

    Class<?>[] result = attributes.getClassArray("classes");
    assertThat(result).hasSize(2);
    assertThat(result[0]).isEqualTo(String.class);
    assertThat(result[1]).isEqualTo(Integer.class);
  }

  @Test
  void getAnnotationWithValidAttribute() {
    AnnotationAttributes attributes = new AnnotationAttributes();
    AnnotationAttributes nested = new AnnotationAttributes();
    nested.put("value", "nested");
    attributes.put("nestedAnnotation", nested);

    AnnotationAttributes result = attributes.getAnnotation("nestedAnnotation");
    assertThat(result.getString("value")).isEqualTo("nested");
  }

  @Test
  void getAnnotationArrayWithSingleAnnotation() {
    AnnotationAttributes attributes = new AnnotationAttributes();
    AnnotationAttributes nested = new AnnotationAttributes();
    nested.put("value", "nested");
    attributes.put("annotations", nested);

    AnnotationAttributes[] result = attributes.getAnnotationArray("annotations");
    assertThat(result).hasSize(1);
    assertThat(result[0].getString("value")).isEqualTo("nested");
  }

  @Test
  void getAnnotationArrayWithAnnotationArray() {
    AnnotationAttributes attributes = new AnnotationAttributes();
    AnnotationAttributes nested1 = new AnnotationAttributes();
    nested1.put("value", "nested1");
    AnnotationAttributes nested2 = new AnnotationAttributes();
    nested2.put("value", "nested2");
    attributes.put("annotations", new AnnotationAttributes[] { nested1, nested2 });

    AnnotationAttributes[] result = attributes.getAnnotationArray("annotations");
    assertThat(result).hasSize(2);
    assertThat(result[0].getString("value")).isEqualTo("nested1");
    assertThat(result[1].getString("value")).isEqualTo("nested2");
  }

  @Test
  void toStringMethod() {
    AnnotationAttributes attributes = new AnnotationAttributes();
    attributes.put("name", "test");
    attributes.put("count", 42);

    String result = attributes.toString();
    assertThat(result).contains("name=test");
    assertThat(result).contains("count=42");
  }

  @Test
  void toStringWithArrayValue() {
    AnnotationAttributes attributes = new AnnotationAttributes();
    attributes.put("names", new String[] { "a", "b" });

    String result = attributes.toString();
    assertThat(result).contains("names=[a, b]");
  }

  @Test
  void toStringWithCircularReference() {
    AnnotationAttributes attributes = new AnnotationAttributes();
    attributes.put("self", attributes);

    String result = attributes.toString();
    assertThat(result).contains("self=(this Map)");
  }

  enum Color {

    RED, WHITE, BLUE
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface Filter {

    @AliasFor(attribute = "classes")
    Class<?>[] value() default {};

    @AliasFor(attribute = "value")
    Class<?>[] classes() default {};

    String pattern();
  }

  @Filter(pattern = "foo")
  static class FilteredClass {
  }

}

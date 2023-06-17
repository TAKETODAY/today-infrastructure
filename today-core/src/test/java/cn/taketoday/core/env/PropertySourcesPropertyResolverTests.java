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

package cn.taketoday.core.env;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import cn.taketoday.core.conversion.ConverterNotFoundException;
import cn.taketoday.core.testfixture.env.MockPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Chris Beams
 */
class PropertySourcesPropertyResolverTests {

  private Properties testProperties;

  private PropertySources propertySources;

  private ConfigurablePropertyResolver propertyResolver;

  @BeforeEach
  void setUp() {
    propertySources = new PropertySources();
    propertyResolver = new PropertySourcesPropertyResolver(propertySources);
    testProperties = new Properties();
    propertySources.addFirst(new PropertiesPropertySource("testProperties", testProperties));
  }

  @Test
  void containsProperty() {
    assertThat(propertyResolver.containsProperty("foo")).isFalse();
    testProperties.put("foo", "bar");
    assertThat(propertyResolver.containsProperty("foo")).isTrue();
  }

  @Test
  void getProperty() {
    assertThat(propertyResolver.getProperty("foo")).isNull();
    testProperties.put("foo", "bar");
    assertThat(propertyResolver.getProperty("foo")).isEqualTo("bar");
  }

  @Test
  void getProperty_withDefaultValue() {
    assertThat(propertyResolver.getProperty("foo", "myDefault")).isEqualTo("myDefault");
    testProperties.put("foo", "bar");
    assertThat(propertyResolver.getProperty("foo")).isEqualTo("bar");
  }

  @Test
  void getProperty_propertySourceSearchOrderIsFIFO() {
    PropertySources sources = new PropertySources();
    PropertyResolver resolver = new PropertySourcesPropertyResolver(sources);
    sources.addFirst(new MockPropertySource("ps1").withProperty("pName", "ps1Value"));
    assertThat(resolver.getProperty("pName")).isEqualTo("ps1Value");
    sources.addFirst(new MockPropertySource("ps2").withProperty("pName", "ps2Value"));
    assertThat(resolver.getProperty("pName")).isEqualTo("ps2Value");
    sources.addFirst(new MockPropertySource("ps3").withProperty("pName", "ps3Value"));
    assertThat(resolver.getProperty("pName")).isEqualTo("ps3Value");
  }

  @Test
  void getProperty_withExplicitNullValue() {
    // java.util.Properties does not allow null values (because Hashtable does not)
    Map<String, Object> nullableProperties = new HashMap<>();
    propertySources.addLast(new MapPropertySource("nullableProperties", nullableProperties));
    nullableProperties.put("foo", null);
    assertThat(propertyResolver.getProperty("foo")).isNull();
  }

  @Test
  void getProperty_withTargetType_andDefaultValue() {
    assertThat(propertyResolver.getProperty("foo", Integer.class, 42)).isEqualTo(42);
    testProperties.put("foo", 13);
    assertThat(propertyResolver.getProperty("foo", Integer.class, 42)).isEqualTo(13);
  }

  @Test
  void getProperty_withStringArrayConversion() {
    testProperties.put("foo", "bar,baz");
    assertThat(propertyResolver.getProperty("foo", String[].class)).isEqualTo(new String[] { "bar", "baz" });
  }

  @Test
  void getProperty_withNonConvertibleTargetType() {
    testProperties.put("foo", "bar");

    class TestType { }

    assertThatExceptionOfType(ConverterNotFoundException.class).isThrownBy(() ->
            propertyResolver.getProperty("foo", TestType.class));
  }

  @Test
  void getProperty_doesNotCache_replaceExistingKeyPostConstruction() {
    String key = "foo";
    String value1 = "bar";
    String value2 = "biz";

    HashMap<String, Object> map = new HashMap<>();
    map.put(key, value1); // before construction
    PropertySources propertySources = new PropertySources();
    propertySources.addFirst(new MapPropertySource("testProperties", map));
    PropertyResolver propertyResolver = new PropertySourcesPropertyResolver(propertySources);
    assertThat(propertyResolver.getProperty(key)).isEqualTo(value1);
    map.put(key, value2); // after construction and first resolution
    assertThat(propertyResolver.getProperty(key)).isEqualTo(value2);
  }

  @Test
  void getProperty_doesNotCache_addNewKeyPostConstruction() {
    HashMap<String, Object> map = new HashMap<>();
    PropertySources propertySources = new PropertySources();
    propertySources.addFirst(new MapPropertySource("testProperties", map));
    PropertyResolver propertyResolver = new PropertySourcesPropertyResolver(propertySources);
    assertThat(propertyResolver.getProperty("foo")).isNull();
    map.put("foo", "42");
    assertThat(propertyResolver.getProperty("foo")).isEqualTo("42");
  }

  @Test
  void getPropertySources_replacePropertySource() {
    propertySources = new PropertySources();
    propertyResolver = new PropertySourcesPropertyResolver(propertySources);
    propertySources.addLast(new MockPropertySource("local").withProperty("foo", "localValue"));
    propertySources.addLast(new MockPropertySource("system").withProperty("foo", "systemValue"));

    // 'local' was added first so has precedence
    assertThat(propertyResolver.getProperty("foo")).isEqualTo("localValue");

    // replace 'local' with new property source
    propertySources.replace("local", new MockPropertySource("new").withProperty("foo", "newValue"));

    // 'system' now has precedence
    assertThat(propertyResolver.getProperty("foo")).isEqualTo("newValue");

    assertThat(propertySources).hasSize(2);
  }

  @Test
  void getRequiredProperty() {
    testProperties.put("exists", "xyz");
    assertThat(propertyResolver.getRequiredProperty("exists")).isEqualTo("xyz");

    assertThatIllegalStateException().isThrownBy(() ->
            propertyResolver.getRequiredProperty("bogus"));
  }

  @Test
  void getRequiredProperty_withStringArrayConversion() {
    testProperties.put("exists", "abc,123");
    assertThat(propertyResolver.getRequiredProperty("exists", String[].class)).isEqualTo(new String[] { "abc", "123" });

    assertThatIllegalStateException().isThrownBy(() ->
            propertyResolver.getRequiredProperty("bogus", String[].class));
  }

  @Test
  void resolvePlaceholders() {
    PropertySources propertySources = new PropertySources();
    propertySources.addFirst(new MockPropertySource().withProperty("key", "value"));
    PropertyResolver resolver = new PropertySourcesPropertyResolver(propertySources);
    assertThat(resolver.resolvePlaceholders("Replace this ${key}")).isEqualTo("Replace this value");
  }

  @Test
  void resolvePlaceholders_withUnresolvable() {
    PropertySources propertySources = new PropertySources();
    propertySources.addFirst(new MockPropertySource().withProperty("key", "value"));
    PropertyResolver resolver = new PropertySourcesPropertyResolver(propertySources);
    assertThat(resolver.resolvePlaceholders("Replace this ${key} plus ${unknown}"))
            .isEqualTo("Replace this value plus ${unknown}");
  }

  @Test
  void resolvePlaceholders_withDefaultValue() {
    PropertySources propertySources = new PropertySources();
    propertySources.addFirst(new MockPropertySource().withProperty("key", "value"));
    PropertyResolver resolver = new PropertySourcesPropertyResolver(propertySources);
    assertThat(resolver.resolvePlaceholders("Replace this ${key} plus ${unknown:defaultValue}"))
            .isEqualTo("Replace this value plus defaultValue");
  }

  @Test
  void resolvePlaceholders_withNullInput() {
    assertThatIllegalArgumentException().isThrownBy(() ->
            new PropertySourcesPropertyResolver(new PropertySources()).resolvePlaceholders(null));
  }

  @Test
  void resolveRequiredPlaceholders() {
    PropertySources propertySources = new PropertySources();
    propertySources.addFirst(new MockPropertySource().withProperty("key", "value"));
    PropertyResolver resolver = new PropertySourcesPropertyResolver(propertySources);
    assertThat(resolver.resolveRequiredPlaceholders("Replace this ${key}")).isEqualTo("Replace this value");
  }

  @Test
  void resolveRequiredPlaceholders_withUnresolvable() {
    PropertySources propertySources = new PropertySources();
    propertySources.addFirst(new MockPropertySource().withProperty("key", "value"));
    PropertyResolver resolver = new PropertySourcesPropertyResolver(propertySources);
    assertThatIllegalArgumentException().isThrownBy(() ->
            resolver.resolveRequiredPlaceholders("Replace this ${key} plus ${unknown}"));
  }

  @Test
  void resolveRequiredPlaceholders_withDefaultValue() {
    PropertySources propertySources = new PropertySources();
    propertySources.addFirst(new MockPropertySource().withProperty("key", "value"));
    PropertyResolver resolver = new PropertySourcesPropertyResolver(propertySources);
    assertThat(resolver.resolveRequiredPlaceholders("Replace this ${key} plus ${unknown:defaultValue}"))
            .isEqualTo("Replace this value plus defaultValue");
  }

  @Test
  void resolveRequiredPlaceholders_withNullInput() {
    assertThatIllegalArgumentException().isThrownBy(() ->
            new PropertySourcesPropertyResolver(new PropertySources()).resolveRequiredPlaceholders(null));
  }

  @Test
  void setRequiredProperties_andValidateRequiredProperties() {
    // no properties have been marked as required -> validation should pass
    propertyResolver.validateRequiredProperties();

    // mark which properties are required
    propertyResolver.setRequiredProperties("foo", "bar");

    // neither foo nor bar properties are present -> validating should throw
    assertThatExceptionOfType(MissingRequiredPropertiesException.class).isThrownBy(
                    propertyResolver::validateRequiredProperties)
            .withMessage("The following properties were declared as required " +
                    "but could not be resolved: [foo, bar]");

    // add foo property -> validation should fail only on missing 'bar' property
    testProperties.put("foo", "fooValue");
    assertThatExceptionOfType(MissingRequiredPropertiesException.class).isThrownBy(
                    propertyResolver::validateRequiredProperties)
            .withMessage("The following properties were declared as required " +
                    "but could not be resolved: [bar]");

    // add bar property -> validation should pass, even with an empty string value
    testProperties.put("bar", "");
    propertyResolver.validateRequiredProperties();
  }

  @Test
  void resolveNestedPropertyPlaceholders() {
    PropertySources ps = new PropertySources();
    ps.addFirst(new MockPropertySource()
            .withProperty("p1", "v1")
            .withProperty("p2", "v2")
            .withProperty("p3", "${p1}:${p2}")              // nested placeholders
            .withProperty("p4", "${p3}")                    // deeply nested placeholders
            .withProperty("p5", "${p1}:${p2}:${bogus}")     // unresolvable placeholder
            .withProperty("p6", "${p1}:${p2}:${bogus:def}") // unresolvable w/ default
            .withProperty("pL", "${pR}")                    // cyclic reference left
            .withProperty("pR", "${pL}")                    // cyclic reference right
    );
    ConfigurablePropertyResolver pr = new PropertySourcesPropertyResolver(ps);
    assertThat(pr.getProperty("p1")).isEqualTo("v1");
    assertThat(pr.getProperty("p2")).isEqualTo("v2");
    assertThat(pr.getProperty("p3")).isEqualTo("v1:v2");
    assertThat(pr.getProperty("p4")).isEqualTo("v1:v2");
    assertThatIllegalArgumentException().isThrownBy(() ->
                    pr.getProperty("p5"))
            .withMessageContaining("Could not resolve placeholder 'bogus' in value \"${p1}:${p2}:${bogus}\"");
    assertThat(pr.getProperty("p6")).isEqualTo("v1:v2:def");
    assertThatIllegalArgumentException().isThrownBy(() ->
                    pr.getProperty("pL"))
            .withMessageContaining("Circular");
  }

  @Test
  void ignoreUnresolvableNestedPlaceholdersIsConfigurable() {
    PropertySources ps = new PropertySources();
    ps.addFirst(new MockPropertySource()
            .withProperty("p1", "v1")
            .withProperty("p2", "v2")
            .withProperty("p3", "${p1}:${p2}:${bogus:def}") // unresolvable w/ default
            .withProperty("p4", "${p1}:${p2}:${bogus}")     // unresolvable placeholder
    );
    ConfigurablePropertyResolver pr = new PropertySourcesPropertyResolver(ps);
    assertThat(pr.getProperty("p1")).isEqualTo("v1");
    assertThat(pr.getProperty("p2")).isEqualTo("v2");
    assertThat(pr.getProperty("p3")).isEqualTo("v1:v2:def");

    // placeholders nested within the value of "p4" are unresolvable and cause an
    // exception by default
    assertThatIllegalArgumentException().isThrownBy(() ->
                    pr.getProperty("p4"))
            .withMessageContaining("Could not resolve placeholder 'bogus' in value \"${p1}:${p2}:${bogus}\"");

    // relax the treatment of unresolvable nested placeholders
    pr.setIgnoreUnresolvableNestedPlaceholders(true);
    // and observe they now pass through unresolved
    assertThat(pr.getProperty("p4")).isEqualTo("v1:v2:${bogus}");

    // resolve[Nested]Placeholders methods behave as usual regardless the value of
    // ignoreUnresolvableNestedPlaceholders
    assertThat(pr.resolvePlaceholders("${p1}:${p2}:${bogus}")).isEqualTo("v1:v2:${bogus}");
    assertThatIllegalArgumentException().isThrownBy(() ->
                    pr.resolveRequiredPlaceholders("${p1}:${p2}:${bogus}"))
            .withMessageContaining("Could not resolve placeholder 'bogus' in value \"${p1}:${p2}:${bogus}\"");
  }

}

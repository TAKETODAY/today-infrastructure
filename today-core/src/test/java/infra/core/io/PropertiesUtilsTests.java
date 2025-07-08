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

package infra.core.io;

import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import infra.util.DefaultPropertiesPersister;
import infra.util.ResourceUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Harry Yang 2021/10/9 10:12
 */
class PropertiesUtilsTests {

  @Test
  void parseNullStringReturnsEmptyProperties() {
    Properties props = PropertiesUtils.parse(null);
    assertThat(props).isEmpty();
  }

  @Test
  void parseEmptyStringReturnsEmptyProperties() {
    Properties props = PropertiesUtils.parse("");
    assertThat(props).isEmpty();
  }

  @Test
  void parseSingleKeyValuePair() {
    Properties props = PropertiesUtils.parse("key=value");
    assertThat(props)
            .hasSize(1)
            .containsEntry("key", "value");
  }

  @Test
  void parseMultipleKeyValuePairs() {
    Properties props = PropertiesUtils.parse("key1=value1\nkey2=value2");
    assertThat(props)
            .hasSize(2)
            .containsEntry("key1", "value1")
            .containsEntry("key2", "value2");
  }

  @Test
  void parsePropertiesWithSpecialCharacters() {
    Properties props = PropertiesUtils.parse("key.with.dots=value\nkey\\ with\\ spaces=value");
    assertThat(props)
            .containsEntry("key.with.dots", "value")
            .containsEntry("key with spaces", "value");
  }

  @Test
  void parsePropertiesWithEmptyValues() {
    Properties props = PropertiesUtils.parse("key1=\nkey2=");
    assertThat(props)
            .containsEntry("key1", "")
            .containsEntry("key2", "");
  }

  @Test
  void parsePropertiesWithComments() {
    Properties props = PropertiesUtils.parse("#comment\n!comment\nkey=value");
    assertThat(props)
            .hasSize(1)
            .containsEntry("key", "value");
  }

  @Test
  void loadEmptyPropertiesFile() throws IOException {
    Resource resource = ResourceUtils.getResource("classpath:empty.properties");
    Properties props = PropertiesUtils.loadProperties(resource);
    assertThat(props).isEmpty();
  }

  @Test
  void loadNonExistentPropertiesFileThrowsException() {
    assertThatExceptionOfType(FileNotFoundException.class)
            .isThrownBy(() -> PropertiesUtils.loadProperties("nonexistent.properties"));
  }

  @Test
  void loadPropertiesFromXml() throws IOException {
    Properties props = PropertiesUtils.loadProperties("test.xml");
    assertThat(props).isNotEmpty();
  }

  @Test
  void loadPropertiesWithEncodedResource() throws IOException {
    EncodedResource resource = new EncodedResource(
            ResourceUtils.getResource("test.properties"),
            StandardCharsets.UTF_8);
    Properties props = PropertiesUtils.loadProperties(resource);
    assertThat(props).isNotEmpty();
  }

  @Test
  void parsePropertiesWithBackslashContinuation() {
    Properties props = PropertiesUtils.parse("long.key=first line \\\nsecond line");
    assertThat(props.getProperty("long.key")).isEqualTo("first line second line");
  }

  @Test
  void parsePropertiesWithUnicodeEscapes() {
    Properties props = PropertiesUtils.parse("unicode.key=\\u0048\\u0065\\u006C\\u006C\\u006F");
    assertThat(props.getProperty("unicode.key")).isEqualTo("Hello");
  }

  @Test
  void loadAllPropertiesWithNullClassLoader() throws IOException {
    Properties props = PropertiesUtils.loadAllProperties("test.properties", null);
    assertThat(props).isNotEmpty();
  }

  @Test
  void fillPropertiesWithNullProperties() {
    Resource resource = ResourceUtils.getResource("test.properties");
    assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> PropertiesUtils.fillProperties(null, resource));
  }

  @Test
  void fillPropertiesWithNullResource() {
    Properties props = new Properties();
    assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> PropertiesUtils.fillProperties(props, null));
  }

  @Test
  void loadAllPropertiesWithNullResourceName() {
    assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> PropertiesUtils.loadAllProperties(null));
  }

  @Test
  void loadPropertiesWithInvalidXmlContent() {
    assertThatExceptionOfType(IOException.class)
            .isThrownBy(() -> PropertiesUtils.loadProperties("invalid.xml"));
  }

  @Test
  void parsePropertiesWithWhitespace() {
    Properties props = PropertiesUtils.parse("  key  =  value with spaces  \n key2=  value2 with spaces ");
    assertThat(props)
            .containsEntry("key", "value with spaces  ")
            .containsEntry("key2", "value2 with spaces ");
  }

  @Test
  void loadPropertiesWithOrderedEntries() throws IOException {
    Properties props = PropertiesUtils.loadProperties("infra/core/io/ordered.properties");
    assertThat(props)
            .containsEntry("a", "value1")
            .containsEntry("b", "value2")
            .containsEntry("c", "value3");
  }

  @Test
  void loadPropertiesFromExampleXml() throws IOException {
    Properties props = PropertiesUtils.loadProperties("infra/core/io/example.xml");
    assertThat(props).isNotEmpty();
  }

  @Test
  void loadPropertiesFromInvalidFileExtension() {
    assertThatExceptionOfType(IOException.class)
            .isThrownBy(() -> PropertiesUtils.loadProperties("infra/core/io/file.invalid"));
  }

  @Test
  void fillPropertiesFromNullEncodedResource() {
    Properties props = new Properties();
    assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> PropertiesUtils.fillProperties(
                    props, null, DefaultPropertiesPersister.INSTANCE));
  }

  @Test
  void parsePropertiesPreservesTrailingSpacesInValues() {
    Properties props = PropertiesUtils.parse("key=value   \nkey2=  value2  ");
    assertThat(props)
            .containsEntry("key", "value   ")
            .containsEntry("key2", "value2  ");
  }

  @Test
  void parsePropertiesWithColonSeparator() {
    Properties props = PropertiesUtils.parse("key:value\nkey2: value2");
    assertThat(props)
            .containsEntry("key", "value")
            .containsEntry("key2", "value2");
  }

  @Test
  void parsePropertiesWithMixedSeparators() {
    Properties props = PropertiesUtils.parse("key1=value1\nkey2:value2\nkey3 = value3");
    assertThat(props)
            .containsEntry("key1", "value1")
            .containsEntry("key2", "value2")
            .containsEntry("key3", "value3");
  }

  @Test
  void parsePropertiesWithWindowsLineEndings() {
    Properties props = PropertiesUtils.parse("key1=value1\r\nkey2=value2\r\n");
    assertThat(props)
            .containsEntry("key1", "value1")
            .containsEntry("key2", "value2");
  }

  @Test
  void loadAllPropertiesWithMultipleResourcesInDifferentClassLoaders() throws IOException {
    ClassLoader firstLoader = new URLClassLoader(new URL[] {});
    ClassLoader secondLoader = new URLClassLoader(new URL[] {});

    Properties firstProps = new Properties();
    PropertiesUtils.loadAllProperties(firstProps, "test.properties", StandardCharsets.ISO_8859_1, firstLoader);
    Properties secondProps = PropertiesUtils.loadAllProperties("test.properties", secondLoader);

    assertThat(firstProps).isNotEmpty();
    assertThat(secondProps).isNotEmpty();
  }

  @Test
  void parsePropertiesIgnoresLeadingWhitespaceBeforeComment() {
    Properties props = PropertiesUtils.parse("  #comment\n  !comment2\nkey=value");
    assertThat(props)
            .hasSize(1)
            .containsEntry("key", "value");
  }

  @Test
  void parsePropertiesWithEqualsInValue() {
    Properties props = PropertiesUtils.parse("key=value=withEquals\nkey2=value:withColon");
    assertThat(props)
            .containsEntry("key", "value=withEquals")
            .containsEntry("key2", "value:withColon");
  }

  @Test
  void parsePropertiesWithBackslashCharacters() {
    Properties props = PropertiesUtils.parse("path=C:\\\\Program Files\\\\App\nkey=value\\\\test");
    assertThat(props)
            .containsEntry("path", "C:\\Program Files\\App")
            .containsEntry("key", "value\\test");
  }

  @Test
  void loadAllPropertiesWithNestedResourceName() throws IOException {
    Properties props = PropertiesUtils.loadAllProperties("infra/core/io/nested/test.properties");
    assertThat(props).isEmpty();

    props = new Properties();
    PropertiesUtils.loadAllProperties(props, "infra/core/io/nested/test.properties");
    assertThat(props).isEmpty();
  }

  @Test
  void fillPropertiesWithNonExistentFile() {
    Properties props = new Properties();
    Resource nonExistentResource = ResourceUtils.getResource("nonexistent.properties");
    assertThatExceptionOfType(FileNotFoundException.class)
            .isThrownBy(() -> PropertiesUtils.fillProperties(props, nonExistentResource));
  }

}

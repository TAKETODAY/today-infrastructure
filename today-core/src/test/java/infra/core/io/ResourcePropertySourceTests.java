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

package infra.core.io;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/22 14:35
 */
class ResourcePropertySourceTests {

  @Test
  void createFromEncodedResourceLoadsProperties() throws IOException {
    Properties props = new Properties();
    props.setProperty("key", "value");

    Resource mockResource = mock(Resource.class);

    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    props.store(stream, "");

    when(mockResource.getInputStream()).thenReturn(new ByteArrayInputStream(stream.toByteArray()));

    EncodedResource encodedResource = new EncodedResource(mockResource);
    ResourcePropertySource source = new ResourcePropertySource("test", encodedResource);

    assertThat(source.getProperty("key")).isEqualTo("value");
  }

  @Test
  void createFromResourceLocationLoadsProperties() throws IOException {
    ResourcePropertySource source = new ResourcePropertySource("test.properties");
    assertThat(source.getProperty("foo")).isEqualTo("bar");
  }

  @Test
  void withNameCreatesNewInstanceWithNewName() throws IOException {
    ResourcePropertySource source = new ResourcePropertySource("test.properties");
    ResourcePropertySource renamed = source.withName("newName");

    assertThat(renamed).isNotSameAs(source);
    assertThat(renamed.getName()).isEqualTo("newName");
    assertThat(renamed.getProperty("foo")).isEqualTo(source.getProperty("foo"));
  }

  @Test
  void withResourceNameRestoresOriginalName() throws IOException {
    ResourcePropertySource source = new ResourcePropertySource("custom", "test.properties");
    ResourcePropertySource restored = source.withResourceName();

    assertThat(restored.getName()).endsWith("test.properties]");
    assertThat(restored.getProperty("foo")).isEqualTo(source.getProperty("foo"));
  }

  @Test
  void createWithCustomClassLoaderLoadsProperties() throws IOException {
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    ResourcePropertySource source = new ResourcePropertySource("test", "test.properties", classLoader);
    assertThat(source.getProperty("foo")).isEqualTo("bar");
  }

  @Test
  void createFromEmptyResourceUsesClassName() throws IOException {
    Resource emptyResource = mock(Resource.class);
    when(emptyResource.toString()).thenReturn("");
    when(emptyResource.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

    ResourcePropertySource source = new ResourcePropertySource(emptyResource);
    assertThat(source.getName()).contains(emptyResource.getClass().getSimpleName());
  }

  @Test
  void createFromInvalidLocationThrowsException() {
    assertThatIOException()
            .isThrownBy(() -> new ResourcePropertySource("invalid.properties"));
  }

  @Test
  void createFromXmlResourceLoadsXmlProperties() throws IOException {
    ResourcePropertySource source = new ResourcePropertySource("test.xml");
    assertThat(source.getProperty("xmlKey")).isEqualTo("xmlValue");
  }

  @Test
  void emptyResourceNameUsesResourceToString() throws IOException {
    Resource mockResource = mock(Resource.class);
    when(mockResource.toString()).thenReturn("");
    when(mockResource.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

    ResourcePropertySource source = new ResourcePropertySource(mockResource);
    assertThat(source.getName()).contains("@");
  }

  @Test
  void multipleSameResourcesShareProperties() throws IOException {
    ResourcePropertySource source1 = new ResourcePropertySource("test.properties");
    ResourcePropertySource source2 = new ResourcePropertySource("test.properties");
    assertThat(source1.getProperty("foo")).isEqualTo(source2.getProperty("foo"));
  }

  @Test
  void withNamePreservesOriginalResourceName() throws IOException {
    ResourcePropertySource source = new ResourcePropertySource("original", "test.properties");
    ResourcePropertySource renamed = source.withName("newName");
    ResourcePropertySource restored = renamed.withResourceName();

    assertThat(restored.getName()).isEqualTo("class path resource [test.properties]");
  }

  @Test
  void withSameNameReturnsSameInstance() throws IOException {
    ResourcePropertySource source = new ResourcePropertySource("name", "test.properties");
    ResourcePropertySource same = source.withName("name");
    assertThat(same).isSameAs(source);
  }

  @Test
  void withResourceNameOnResourceNameSourceReturnsSameInstance() throws IOException {
    ResourcePropertySource source = new ResourcePropertySource("test.properties");
    ResourcePropertySource same = source.withResourceName();
    assertThat(same).isSameAs(source);
  }

  @Test
  void createFromResourceWithNonEmptyToString() throws IOException {
    Resource mockResource = mock(Resource.class);
    when(mockResource.toString()).thenReturn("MyResource");
    when(mockResource.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

    ResourcePropertySource source = new ResourcePropertySource(mockResource);
    assertThat(source.getName()).isEqualTo("MyResource");
  }

  @Test
  void propertySourceWithChineseCharactersLoads() throws IOException {
    Properties props = new Properties();
    props.setProperty("中文", "值");

    Resource mockResource = mock(Resource.class);
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    props.store(stream, "");
    when(mockResource.getInputStream()).thenReturn(new ByteArrayInputStream(stream.toByteArray()));

    ResourcePropertySource source = new ResourcePropertySource(mockResource);
    assertThat(source.getProperty("中文")).isEqualTo("值");
  }

  @Test
  void propertySourceWithSpecialCharactersInName() throws IOException {
    ResourcePropertySource source = new ResourcePropertySource("name!@#$%^&*()", "test.properties");
    assertThat(source.getName()).isEqualTo("name!@#$%^&*()");
  }

  @Test
  void duplicatePropertiesUsesLastValue() throws IOException {
    Properties props = new Properties();
    props.setProperty("key", "value1");
    props.setProperty("key", "value2");

    Resource mockResource = mock(Resource.class);
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    props.store(stream, "");
    when(mockResource.getInputStream()).thenReturn(new ByteArrayInputStream(stream.toByteArray()));

    ResourcePropertySource source = new ResourcePropertySource(mockResource);
    assertThat(source.getProperty("key")).isEqualTo("value2");
  }

  @Test
  void resourceWithLongNamePreservesFullName() throws IOException {
    String longName = "a".repeat(1000);
    ResourcePropertySource source = new ResourcePropertySource(longName, "test.properties");
    assertThat(source.getName()).isEqualTo(longName);
  }

  @Test
  void emptyPropertiesFileCreatesEmptySource() throws IOException {
    Resource mockResource = mock(Resource.class);
    when(mockResource.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

    ResourcePropertySource source = new ResourcePropertySource(mockResource);
    assertThat(source.getSource()).isEmpty();
  }

  @Test
  void resourceNameWithBackslashesIsPreserved() throws IOException {
    String nameWithBackslashes = "path\\to\\properties";
    ResourcePropertySource source = new ResourcePropertySource(nameWithBackslashes, "test.properties");
    assertThat(source.getName()).isEqualTo(nameWithBackslashes);
  }
}
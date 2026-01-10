/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.core.io;

import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import infra.util.PropertiesPersister;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/22 15:11
 */
class PropertiesLoaderSupportTests {

  @Test
  void localPropertiesOverrideFilePropertiesWhenLocalOverrideEnabled() throws IOException {
    TestPropertiesLoader loader = new TestPropertiesLoader();
    Properties local = new Properties();
    local.setProperty("key", "local-value");
    loader.setProperties(local);
    loader.setLocation(new PathResource("src/test/resources/infra/core/io/example.properties"));
    loader.setLocalOverride(true);

    Properties result = loader.mergeProperties();

    assertThat(result.getProperty("key")).isEqualTo("local-value");
  }

  @Test
  void filePropertiesOverrideLocalPropertiesWhenLocalOverrideDisabled() throws IOException {
    TestPropertiesLoader loader = new TestPropertiesLoader();
    Properties local = new Properties();
    local.setProperty("key", "local-value");
    loader.setProperties(local);
    loader.setLocation(new PathResource("src/test/resources/infra/core/io/example.properties"));

    Properties result = loader.mergeProperties();

    assertThat(result.getProperty("key")).isEqualTo("file-value");
  }

  @Test
  void multipleLocalPropertiesAreMergedInOrder() throws IOException {
    TestPropertiesLoader loader = new TestPropertiesLoader();
    Properties first = new Properties();
    first.setProperty("key1", "value1");
    Properties second = new Properties();
    second.setProperty("key2", "value2");
    loader.setPropertiesArray(first, second);

    Properties result = loader.mergeProperties();

    assertThat(result)
            .containsEntry("key1", "value1")
            .containsEntry("key2", "value2");
  }

  @Test
  void multipleLocationsAreMergedWithLatestWinning() throws IOException {
    TestPropertiesLoader loader = new TestPropertiesLoader();
    loader.setLocations(
            new PathResource("src/test/resources/infra/core/io/first.properties"),
            new PathResource("src/test/resources/infra/core/io/second.properties")
    );

    Properties result = loader.mergeProperties();

    assertThat(result.getProperty("common.key")).isEqualTo("second-value");
  }

  @Test
  void missingResourceThrowsExceptionWhenNotIgnored() {
    TestPropertiesLoader loader = new TestPropertiesLoader();
    loader.setLocation(new PathResource("missing.properties"));

    assertThatExceptionOfType(FileNotFoundException.class)
            .isThrownBy(loader::mergeProperties);
  }

  @Test
  void missingResourceIsSkippedWhenIgnored() throws IOException {
    TestPropertiesLoader loader = new TestPropertiesLoader();
    loader.setLocation(new PathResource("missing.properties"));
    loader.setIgnoreResourceNotFound(true);

    Properties result = loader.mergeProperties();

    assertThat(result).isEmpty();
  }

  @Test
  void propertiesAreLoadedWithSpecifiedEncoding() throws IOException {
    TestPropertiesLoader loader = new TestPropertiesLoader();
    loader.setLocation(new PathResource("src/test/resources/infra/core/io/utf8.properties"));
    loader.setFileEncoding("UTF-8");

    Properties result = loader.mergeProperties();

    assertThat(result.getProperty("utf8.key")).isEqualTo("测试");
  }

  @Test
  void customPropertiesPersisterIsUsedToLoadProperties() throws IOException {
    TestPropertiesLoader loader = new TestPropertiesLoader();
    PropertiesPersister persister = mock(PropertiesPersister.class);
    loader.setPropertiesPersister(persister);
    loader.setLocation(new PathResource("src/test/resources/infra/core/io/example.properties"));

    loader.mergeProperties();

    verify(persister).load(any(Properties.class), any(InputStream.class));
  }

  @Test
  void loadPropertiesFromAllTestFiles() throws IOException {
    TestPropertiesLoader loader = new TestPropertiesLoader();
    loader.setLocations(
            new PathResource("src/test/resources/infra/core/io/first.properties"),
            new PathResource("src/test/resources/infra/core/io/second.properties"),
            new PathResource("src/test/resources/infra/core/io/utf8.properties")
    );

    loader.setFileEncoding("UTF-8");

    Properties result = loader.mergeProperties();

    assertThat(result)
            .containsEntry("common.key", "second-value")
            .containsEntry("first.key", "first-only")
            .containsEntry("second.key", "second-only")
            .containsEntry("utf8.key", "测试");
  }

  private static class TestPropertiesLoader extends PropertiesLoaderSupport {
  }
}
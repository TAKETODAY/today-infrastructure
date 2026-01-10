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

package infra.util;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/22 19:27
 */
class DefaultPropertiesPersisterTests {


  @Test
  void propertiesLoadedFromInputStream() throws IOException {
    Properties props = new Properties();
    var content = "key=value";
    DefaultPropertiesPersister.INSTANCE.load(props, new ByteArrayInputStream(content.getBytes()));
    assertThat(props).containsEntry("key", "value");
  }

  @Test
  void propertiesLoadedFromReader() throws IOException {
    Properties props = new Properties();
    var content = "key=value";
    DefaultPropertiesPersister.INSTANCE.load(props, new StringReader(content));
    assertThat(props).containsEntry("key", "value");
  }

  @Test
  void propertiesStoredToOutputStream() throws IOException {
    Properties props = new Properties();
    props.setProperty("key", "value");
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    DefaultPropertiesPersister.INSTANCE.store(props, out, "header");

    String stored = out.toString();
    assertThat(stored).contains("header");
    assertThat(stored).contains("key=value");
  }

  @Test
  void propertiesStoredToWriter() throws IOException {
    Properties props = new Properties();
    props.setProperty("key", "value");
    StringWriter writer = new StringWriter();

    DefaultPropertiesPersister.INSTANCE.store(props, writer, "header");

    assertThat(writer.toString()).contains("header");
    assertThat(writer.toString()).contains("key=value");
  }

  @Test
  void propertiesLoadedFromXmlInputStream() throws IOException {
    Properties props = new Properties();
    var xml = """
           <?xml version="1.0" encoding="UTF-8"?>
           <!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">
           <properties>
             <entry key="key">value</entry>
           </properties>
           """;

    DefaultPropertiesPersister.INSTANCE.loadFromXml(props, new ByteArrayInputStream(xml.getBytes()));
    assertThat(props).containsEntry("key", "value");
  }

  @Test
  void propertiesStoredToXmlOutputStream() throws IOException {
    Properties props = new Properties();
    props.setProperty("key", "value");
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    DefaultPropertiesPersister.INSTANCE.storeToXml(props, out, "header");

    String xml = out.toString();
    assertThat(xml).contains("<?xml");
    assertThat(xml).contains("header");
    assertThat(xml).contains("<entry key=\"key\">value</entry>");
  }

  @Test
  void propertiesStoredToXmlOutputStreamWithEncoding() throws IOException {
    Properties props = new Properties();
    props.setProperty("key", "value");
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    DefaultPropertiesPersister.INSTANCE.storeToXml(props, out, "header", "UTF-8");

    String xml = out.toString();
    assertThat(xml).contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    assertThat(xml).contains("header");
    assertThat(xml).contains("<entry key=\"key\">value</entry>");
  }

  @Test
  void loadFailsWithNullInputStream() {
    Properties props = new Properties();
    assertThatNullPointerException()
            .isThrownBy(() -> DefaultPropertiesPersister.INSTANCE.load(props, (InputStream)null));
  }

  @Test
  void loadFailsWithNullReader() {
    Properties props = new Properties();
    assertThatNullPointerException()
            .isThrownBy(() -> DefaultPropertiesPersister.INSTANCE.load(props, (Reader) null));
  }

  @Test
  void loadFailsWithNullProperties() {
    assertThatNullPointerException()
            .isThrownBy(() -> DefaultPropertiesPersister.INSTANCE.load(null, new StringReader("")));
  }

  @Test
  void storeFailsWithNullOutputStream() {
    Properties props = new Properties();
    assertThatNullPointerException()
            .isThrownBy(() -> DefaultPropertiesPersister.INSTANCE.store(props, (OutputStream)null, "header"));
  }

  @Test
  void storeFailsWithNullWriter() {
    Properties props = new Properties();
    assertThatNullPointerException()
            .isThrownBy(() -> DefaultPropertiesPersister.INSTANCE.store(props, (Writer) null, "header"));
  }

  @Test
  void storeFailsWithNullProperties() {
    assertThatNullPointerException()
            .isThrownBy(() -> DefaultPropertiesPersister.INSTANCE.store(null, new StringWriter(), "header"));
  }

}
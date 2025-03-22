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

import static org.assertj.core.api.Assertions.*;

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
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

package cn.taketoday.framework.env;

import org.junit.jupiter.api.Test;

import java.util.List;

import cn.taketoday.core.env.PropertySource;
import cn.taketoday.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PropertiesPropertySourceLoader}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class PropertiesPropertySourceLoaderTests {

  private final PropertiesPropertySourceLoader loader = new PropertiesPropertySourceLoader();

  @Test
  void getFileExtensions() {
    assertThat(this.loader.getFileExtensions()).isEqualTo(new String[] { "properties", "xml" });
  }

  @Test
  void loadProperties() throws Exception {
    List<PropertySource<?>> loaded = this.loader.load("test.properties",
            new ClassPathResource("test-properties.properties", getClass()));
    PropertySource<?> source = loaded.get(0);
    assertThat(source.getProperty("test")).isEqualTo("properties");
  }

  @Test
  void loadMultiDocumentPropertiesWithSeparatorAtTheBeginningOfFile() throws Exception {
    List<PropertySource<?>> loaded = this.loader.load("test.properties",
            new ClassPathResource("multi-document-properties-2.properties", getClass()));
    assertThat(loaded).hasSize(2);
    PropertySource<?> source1 = loaded.get(0);
    PropertySource<?> source2 = loaded.get(1);
    assertThat(source1.getProperty("blah")).isEqualTo("hello world");
    assertThat(source2.getProperty("foo")).isEqualTo("bar");
  }

  @Test
  void loadMultiDocumentProperties() throws Exception {
    List<PropertySource<?>> loaded = this.loader.load("test.properties",
            new ClassPathResource("multi-document-properties.properties", getClass()));
    assertThat(loaded).hasSize(2);
    PropertySource<?> source1 = loaded.get(0);
    PropertySource<?> source2 = loaded.get(1);
    assertThat(source1.getProperty("blah")).isEqualTo("hello world");
    assertThat(source2.getProperty("foo")).isEqualTo("bar");
  }

  @Test
  void loadMultiDocumentPropertiesWithEmptyDocument() throws Exception {
    List<PropertySource<?>> loaded = this.loader.load("test.properties",
            new ClassPathResource("multi-document-properties-empty.properties", getClass()));
    assertThat(loaded).hasSize(2);
    PropertySource<?> source1 = loaded.get(0);
    PropertySource<?> source2 = loaded.get(1);
    assertThat(source1.getProperty("blah")).isEqualTo("hello world");
    assertThat(source2.getProperty("foo")).isEqualTo("bar");
  }

  @Test
  void loadXml() throws Exception {
    List<PropertySource<?>> loaded = this.loader.load("test.xml",
            new ClassPathResource("test-xml.xml", getClass()));
    PropertySource<?> source = loaded.get(0);
    assertThat(source.getProperty("test")).isEqualTo("xml");
  }

}

/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.core;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.constructor.ConstructorException;
import org.yaml.snakeyaml.parser.ParserException;
import org.yaml.snakeyaml.scanner.ScannerException;

import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.core.io.ByteArrayResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.entry;

/**
 * @author TODAY 2021/9/30 23:41
 */
class YamlProcessorTests {

  private final YamlProcessor processor = new YamlProcessor();

  @Test
  void arrayConvertedToIndexedBeanReference() {
    setYaml("foo: bar\nbar: [1,2,3]");
    this.processor.process((properties, map) -> {
      assertThat(properties).hasSize(4);
      assertThat(properties.get("foo")).isEqualTo("bar");
      assertThat(properties.getProperty("foo")).isEqualTo("bar");
      assertThat(properties.get("bar[0]")).isEqualTo(1);
      assertThat(properties.getProperty("bar[0]")).isEqualTo("1");
      assertThat(properties.get("bar[1]")).isEqualTo(2);
      assertThat(properties.getProperty("bar[1]")).isEqualTo("2");
      assertThat(properties.get("bar[2]")).isEqualTo(3);
      assertThat(properties.getProperty("bar[2]")).isEqualTo("3");
    });
  }

  @Test
  void stringResource() {
    setYaml("foo # a document that is a literal");
    this.processor.process((properties, map) -> assertThat(map.get("document")).isEqualTo("foo"));
  }

  @Test
  void badDocumentStart() {
    setYaml("foo # a document\nbar: baz");
    assertThatExceptionOfType(ParserException.class)
            .isThrownBy(() -> this.processor.process((properties, map) -> { }))
            .withMessageContaining("line 2, column 1");
  }

  @Test
  void badResource() {
    setYaml("foo: bar\ncd\nspam:\n  foo: baz");
    assertThatExceptionOfType(ScannerException.class)
            .isThrownBy(() -> this.processor.process((properties, map) -> { }))
            .withMessageContaining("line 3, column 1");
  }

  @Test
  void mapConvertedToIndexedBeanReference() {
    setYaml("foo: bar\nbar:\n spam: bucket");
    this.processor.process((properties, map) -> {
      assertThat(properties.get("bar.spam")).isEqualTo("bucket");
      assertThat(properties).hasSize(2);
    });
  }

  @Test
  void integerKeyBehaves() {
    setYaml("foo: bar\n1: bar");
    this.processor.process((properties, map) -> {
      assertThat(properties.get("[1]")).isEqualTo("bar");
      assertThat(properties).hasSize(2);
    });
  }

  @Test
  void integerDeepKeyBehaves() {
    setYaml("foo:\n  1: bar");
    this.processor.process((properties, map) -> {
      assertThat(properties.get("foo[1]")).isEqualTo("bar");
      assertThat(properties).hasSize(1);
    });
  }

  @Test
  @SuppressWarnings("unchecked")
  void flattenedMapIsSameAsPropertiesButOrdered() {
    setYaml("cat: dog\nfoo: bar\nbar:\n spam: bucket");
    this.processor.process((properties, map) -> {
      Map<String, Object> flattenedMap = processor.getFlattenedMap(map);
      assertThat(flattenedMap).isInstanceOf(LinkedHashMap.class);

      assertThat(properties).hasSize(3);
      assertThat(flattenedMap).hasSize(3);

      assertThat(properties.get("bar.spam")).isEqualTo("bucket");
      assertThat(flattenedMap.get("bar.spam")).isEqualTo("bucket");

      Map<String, Object> bar = (Map<String, Object>) map.get("bar");
      assertThat(bar.get("spam")).isEqualTo("bucket");

      List<Object> keysFromProperties = new ArrayList<>(properties.keySet());
      List<String> keysFromFlattenedMap = new ArrayList<>(flattenedMap.keySet());
      assertThat(keysFromProperties).containsExactlyInAnyOrderElementsOf(keysFromFlattenedMap);
      // Keys in the Properties object are sorted.
      assertThat(keysFromProperties).containsExactly("bar.spam", "cat", "foo");
      // But the flattened map retains the order from the input.
      assertThat(keysFromFlattenedMap).containsExactly("cat", "foo", "bar.spam");
    });
  }

  @Test
  @SuppressWarnings("unchecked")
  void standardTypesSupportedByDefault() throws Exception {
    setYaml("value: !!set\n  ? first\n  ? second");
    this.processor.process((properties, map) -> {
      assertThat(properties).containsExactly(entry("value[0]", "first"), entry("value[1]", "second"));
      assertThat(map.get("value")).isInstanceOf(Set.class);
      Set<String> set = (Set<String>) map.get("value");
      assertThat(set).containsExactly("first", "second");
    });
  }

  @Test
  void customTypeNotSupportedByDefault() throws Exception {
    URL url = new URL("https://localhost:9000/");
    setYaml("value: !!java.net.URL [\"" + url + "\"]");
    assertThatExceptionOfType(ConstructorException.class)
            .isThrownBy(() -> this.processor.process((properties, map) -> { }))
            .withMessageContaining("Unsupported type encountered in YAML document: java.net.URL");
  }

  @Test
  void customTypesSupportedDueToExplicitConfiguration() throws Exception {
    this.processor.setSupportedTypes(URL.class, String.class);

    URL url = new URL("https://localhost:9000/");
    setYaml("value: !!java.net.URL [!!java.lang.String [\"" + url + "\"]]");

    this.processor.process((properties, map) -> {
      assertThat(properties).containsExactly(entry("value", url));
      assertThat(map).containsExactly(entry("value", url));
    });
  }

  @Test
  void customTypeNotSupportedDueToExplicitConfiguration() {
    this.processor.setSupportedTypes(List.class);

    setYaml("value: !!java.net.URL [\"https://localhost:9000/\"]");

    assertThatExceptionOfType(ConstructorException.class)
            .isThrownBy(() -> this.processor.process((properties, map) -> { }))
            .withMessageContaining("Unsupported type encountered in YAML document: java.net.URL");
  }

  private void setYaml(String yaml) {
    this.processor.setResources(new ByteArrayResource(yaml.getBytes()));
  }

}

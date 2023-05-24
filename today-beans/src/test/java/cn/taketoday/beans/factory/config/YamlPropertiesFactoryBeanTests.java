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

package cn.taketoday.beans.factory.config;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.DuplicateKeyException;
import org.yaml.snakeyaml.scanner.ScannerException;

import java.util.Map;
import java.util.Properties;

import cn.taketoday.core.YamlProcessor.MatchStatus;
import cn.taketoday.core.YamlProcessor.ResolutionMethod;
import cn.taketoday.core.io.ByteArrayResource;
import cn.taketoday.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/5/24 14:45
 */
class YamlPropertiesFactoryBeanTests {

  @Test
  void loadResource() {
    YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
    factory.setResources(new ByteArrayResource("foo: bar\nspam:\n  foo: baz".getBytes()));
    Properties properties = factory.getObject();
    assertThat(properties.getProperty("foo")).isEqualTo("bar");
    assertThat(properties.getProperty("spam.foo")).isEqualTo("baz");
  }

  @Test
  void badResource() {
    YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
    factory.setResources(new ByteArrayResource("foo: bar\ncd\nspam:\n  foo: baz".getBytes()));
    assertThatExceptionOfType(ScannerException.class)
            .isThrownBy(factory::getObject)
            .withMessageContaining("line 3, column 1");
  }

  @Test
  void loadResourcesWithOverride() {
    YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
    factory.setResources(
            new ByteArrayResource("foo: bar\nspam:\n  foo: baz".getBytes()),
            new ByteArrayResource("foo:\n  bar: spam".getBytes()));
    Properties properties = factory.getObject();
    assertThat(properties.getProperty("foo")).isEqualTo("bar");
    assertThat(properties.getProperty("spam.foo")).isEqualTo("baz");
    assertThat(properties.getProperty("foo.bar")).isEqualTo("spam");
  }

  @Test
  void loadResourcesWithInternalOverride() {
    YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
    factory.setResources(new ByteArrayResource(
            "foo: bar\nspam:\n  foo: baz\nfoo: bucket".getBytes()));
    assertThatExceptionOfType(DuplicateKeyException.class).isThrownBy(factory::getObject);
  }

  @Test
  void loadResourcesWithNestedInternalOverride() {
    YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
    factory.setResources(new ByteArrayResource(
            "foo:\n  bar: spam\n  foo: baz\nbreak: it\nfoo: bucket".getBytes()));
    assertThatExceptionOfType(DuplicateKeyException.class).isThrownBy(factory::getObject);
  }

  @Test
  void loadResourceWithMultipleDocuments() {
    YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
    factory.setResources(new ByteArrayResource(
            "foo: bar\nspam: baz\n---\nfoo: bag".getBytes()));
    Properties properties = factory.getObject();
    assertThat(properties.getProperty("foo")).isEqualTo("bag");
    assertThat(properties.getProperty("spam")).isEqualTo("baz");
  }

  @Test
  void loadResourceWithSelectedDocuments() {
    YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
    factory.setResources(new ByteArrayResource(
            "foo: bar\nspam: baz\n---\nfoo: bag\nspam: bad".getBytes()));
    factory.setDocumentMatchers(properties -> ("bag".equals(properties.getProperty("foo")) ?
                                               MatchStatus.FOUND : MatchStatus.NOT_FOUND));
    Properties properties = factory.getObject();
    assertThat(properties.getProperty("foo")).isEqualTo("bag");
    assertThat(properties.getProperty("spam")).isEqualTo("bad");
  }

  @Test
  void loadResourceWithDefaultMatch() {
    YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
    factory.setMatchDefault(true);
    factory.setResources(new ByteArrayResource(
            "one: two\n---\nfoo: bar\nspam: baz\n---\nfoo: bag\nspam: bad".getBytes()));
    factory.setDocumentMatchers(properties -> {
      if (!properties.containsKey("foo")) {
        return MatchStatus.ABSTAIN;
      }
      return ("bag".equals(properties.getProperty("foo")) ?
              MatchStatus.FOUND : MatchStatus.NOT_FOUND);
    });
    Properties properties = factory.getObject();
    assertThat(properties.getProperty("foo")).isEqualTo("bag");
    assertThat(properties.getProperty("spam")).isEqualTo("bad");
    assertThat(properties.getProperty("one")).isEqualTo("two");
  }

  @Test
  void loadResourceWithoutDefaultMatch() {
    YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
    factory.setMatchDefault(false);
    factory.setResources(new ByteArrayResource(
            "one: two\n---\nfoo: bar\nspam: baz\n---\nfoo: bag\nspam: bad".getBytes()));
    factory.setDocumentMatchers(properties -> {
      if (!properties.containsKey("foo")) {
        return MatchStatus.ABSTAIN;
      }
      return ("bag".equals(properties.getProperty("foo")) ?
              MatchStatus.FOUND : MatchStatus.NOT_FOUND);
    });
    Properties properties = factory.getObject();
    assertThat(properties.getProperty("foo")).isEqualTo("bag");
    assertThat(properties.getProperty("spam")).isEqualTo("bad");
    assertThat(properties.getProperty("one")).isNull();
  }

  @Test
  void loadResourceWithDefaultMatchSkippingMissedMatch() {
    YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
    factory.setMatchDefault(true);
    factory.setResources(new ByteArrayResource(
            "one: two\n---\nfoo: bag\nspam: bad\n---\nfoo: bar\nspam: baz".getBytes()));
    factory.setDocumentMatchers(properties -> {
      if (!properties.containsKey("foo")) {
        return MatchStatus.ABSTAIN;
      }
      return ("bag".equals(properties.getProperty("foo")) ?
              MatchStatus.FOUND : MatchStatus.NOT_FOUND);
    });
    Properties properties = factory.getObject();
    assertThat(properties.getProperty("foo")).isEqualTo("bag");
    assertThat(properties.getProperty("spam")).isEqualTo("bad");
    assertThat(properties.getProperty("one")).isEqualTo("two");
  }

  @Test
  void loadNonExistentResource() {
    YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
    factory.setResolutionMethod(ResolutionMethod.OVERRIDE_AND_IGNORE);
    factory.setResources(new ClassPathResource("no-such-file.yml"));
    Properties properties = factory.getObject();
    assertThat(properties).isEmpty();
  }

  @Test
  void loadNull() {
    YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
    factory.setResources(new ByteArrayResource("foo: bar\nspam:".getBytes()));
    Properties properties = factory.getObject();
    assertThat(properties.getProperty("foo")).isEqualTo("bar");
    assertThat(properties.getProperty("spam")).isEmpty();
  }

  @Test
  void loadEmptyArrayValue() {
    YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
    factory.setResources(new ByteArrayResource("a: alpha\ntest: []".getBytes()));
    Properties properties = factory.getObject();
    assertThat(properties.getProperty("a")).isEqualTo("alpha");
    assertThat(properties.getProperty("test")).isEmpty();
  }

  @Test
  void loadArrayOfString() {
    YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
    factory.setResources(new ByteArrayResource("foo:\n- bar\n- baz".getBytes()));
    Properties properties = factory.getObject();
    assertThat(properties.getProperty("foo[0]")).isEqualTo("bar");
    assertThat(properties.getProperty("foo[1]")).isEqualTo("baz");
    assertThat(properties.get("foo")).isNull();
  }

  @Test
  void loadArrayOfInteger() {
    YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
    factory.setResources(new ByteArrayResource("foo:\n- 1\n- 2".getBytes()));
    Properties properties = factory.getObject();
    assertThat(properties.getProperty("foo[0]")).isEqualTo("1");
    assertThat(properties.getProperty("foo[1]")).isEqualTo("2");
    assertThat(properties.get("foo")).isNull();
  }

  @Test
  void loadArrayOfObject() {
    YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
    factory.setResources(new ByteArrayResource(
            "foo:\n- bar:\n    spam: crap\n- baz\n- one: two\n  three: four".getBytes()
    ));
    Properties properties = factory.getObject();
    assertThat(properties.getProperty("foo[0].bar.spam")).isEqualTo("crap");
    assertThat(properties.getProperty("foo[1]")).isEqualTo("baz");
    assertThat(properties.getProperty("foo[2].one")).isEqualTo("two");
    assertThat(properties.getProperty("foo[2].three")).isEqualTo("four");
    assertThat(properties.get("foo")).isNull();
  }

  @Test
  @SuppressWarnings("unchecked")
  void yaml() {
    Yaml yaml = new Yaml();
    Map<String, ?> map = yaml.loadAs("foo: bar\nspam:\n  foo: baz", Map.class);
    assertThat(map.get("foo")).isEqualTo("bar");
    assertThat(((Map<String, Object>) map.get("spam")).get("foo")).isEqualTo("baz");
  }

}
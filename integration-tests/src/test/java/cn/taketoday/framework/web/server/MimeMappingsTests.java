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

package cn.taketoday.framework.web.server;

import org.apache.catalina.startup.Tomcat;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.PropertiesUtils;
import cn.taketoday.framework.web.server.MimeMappings.DefaultMimeMappings;
import cn.taketoday.framework.web.server.MimeMappings.Mapping;
import cn.taketoday.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link MimeMappings}.
 *
 * @author Phillip Webb
 */
class MimeMappingsTests {

  @Test
  void defaultsCannotBeModified() {
    assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> MimeMappings.DEFAULT.add("foo", "foo/bar"));
  }

  @Test
  void createFromExisting() {
    MimeMappings mappings = new MimeMappings();
    mappings.add("foo", "bar");
    MimeMappings clone = new MimeMappings(mappings);
    mappings.add("baz", "bar");
    assertThat(clone.get("foo")).isEqualTo("bar");
    assertThat(clone.get("baz")).isNull();
  }

  @Test
  void createFromMap() {
    Map<String, String> mappings = new HashMap<>();
    mappings.put("foo", "bar");
    MimeMappings clone = new MimeMappings(mappings);
    mappings.put("baz", "bar");
    assertThat(clone.get("foo")).isEqualTo("bar");
    assertThat(clone.get("baz")).isNull();
  }

  @Test
  void iterate() {
    MimeMappings mappings = new MimeMappings();
    mappings.add("foo", "bar");
    mappings.add("baz", "boo");
    List<Mapping> mappingList = new ArrayList<>();
    for (Mapping mapping : mappings) {
      mappingList.add(mapping);
    }
    assertThat(mappingList.get(0).getExtension()).isEqualTo("foo");
    assertThat(mappingList.get(0).getMimeType()).isEqualTo("bar");
    assertThat(mappingList.get(1).getExtension()).isEqualTo("baz");
    assertThat(mappingList.get(1).getMimeType()).isEqualTo("boo");
  }

  @Test
  void getAll() {
    MimeMappings mappings = new MimeMappings();
    mappings.add("foo", "bar");
    mappings.add("baz", "boo");
    List<Mapping> mappingList = new ArrayList<>(mappings.getAll());
    assertThat(mappingList.get(0).getExtension()).isEqualTo("foo");
    assertThat(mappingList.get(0).getMimeType()).isEqualTo("bar");
    assertThat(mappingList.get(1).getExtension()).isEqualTo("baz");
    assertThat(mappingList.get(1).getMimeType()).isEqualTo("boo");
  }

  @Test
  void addNew() {
    MimeMappings mappings = new MimeMappings();
    assertThat(mappings.add("foo", "bar")).isNull();
  }

  @Test
  void addReplacesExisting() {
    MimeMappings mappings = new MimeMappings();
    mappings.add("foo", "bar");
    assertThat(mappings.add("foo", "baz")).isEqualTo("bar");
  }

  @Test
  void remove() {
    MimeMappings mappings = new MimeMappings();
    mappings.add("foo", "bar");
    assertThat(mappings.remove("foo")).isEqualTo("bar");
    assertThat(mappings.remove("foo")).isNull();
  }

  @Test
  void get() {
    MimeMappings mappings = new MimeMappings();
    mappings.add("foo", "bar");
    assertThat(mappings.get("foo")).isEqualTo("bar");
  }

  @Test
  void getMissing() {
    MimeMappings mappings = new MimeMappings();
    assertThat(mappings.get("foo")).isNull();
  }

  @Test
  void makeUnmodifiable() {
    MimeMappings mappings = new MimeMappings();
    mappings.add("foo", "bar");
    MimeMappings unmodifiable = MimeMappings.unmodifiableMappings(mappings);
    try {
      unmodifiable.remove("foo");
    }
    catch (UnsupportedOperationException ex) {
      // Expected
    }
    mappings.remove("foo");
    assertThat(unmodifiable.get("foo")).isNull();
  }

  @Test
  void mimeTypesInDefaultMappingsAreCorrectlyStructured() {
    String regName = "[A-Za-z0-9!#$&.+\\-^_]{1,127}";
    Pattern pattern = Pattern.compile("^" + regName + "\\/" + regName + "$");
    assertThat(MimeMappings.DEFAULT).allSatisfy((mapping) -> assertThat(mapping.getMimeType()).matches(pattern));
  }

  @Test
  void getCommonTypeOnDefaultMimeMappingsDoesNotLoadMappings() {
    DefaultMimeMappings mappings = new DefaultMimeMappings();
    assertThat(mappings.get("json")).isEqualTo("application/json");
    assertThat((Object) mappings).extracting("loaded").isNull();
  }

  @Test
  void getExoticTypeOnDefaultMimeMappingsLoadsMappings() {
    DefaultMimeMappings mappings = new DefaultMimeMappings();
    assertThat(mappings.get("123")).isEqualTo("application/vnd.lotus-1-2-3");
    assertThat((Object) mappings).extracting("loaded").isNotNull();
  }

  @Test
  void iterateOnDefaultMimeMappingsLoadsMappings() {
    DefaultMimeMappings mappings = new DefaultMimeMappings();
    assertThat(mappings).isNotEmpty();
    assertThat((Object) mappings).extracting("loaded").isNotNull();
  }

  @Test
  void commonMappingsAreSubsetOfAllMappings() {
    MimeMappings defaultMappings = new DefaultMimeMappings();
    MimeMappings commonMappings = (MimeMappings) ReflectionTestUtils.getField(DefaultMimeMappings.class, "COMMON");
    for (Mapping commonMapping : commonMappings) {
      assertThat(defaultMappings.get(commonMapping.getExtension())).isEqualTo(commonMapping.getMimeType());
    }
  }

  @Test
  void lazyCopyWhenNotMutatedDelegates() {
    DefaultMimeMappings mappings = new DefaultMimeMappings();
    MimeMappings lazyCopy = MimeMappings.lazyCopy(mappings);
    assertThat(lazyCopy.get("json")).isEqualTo("application/json");
    assertThat((Object) mappings).extracting("loaded").isNull();
  }

  @Test
  void lazyCopyWhenMutatedCreatesCopy() {
    DefaultMimeMappings mappings = new DefaultMimeMappings();
    MimeMappings lazyCopy = MimeMappings.lazyCopy(mappings);
    lazyCopy.add("json", "other/json");
    assertThat(lazyCopy.get("json")).isEqualTo("other/json");
    assertThat((Object) mappings).extracting("loaded").isNotNull();
  }

  @Test
  void lazyCopyWhenMutatedCreatesCopyOnlyOnce() {
    MimeMappings mappings = new MimeMappings();
    mappings.add("json", "one/json");
    MimeMappings lazyCopy = MimeMappings.lazyCopy(mappings);
    lazyCopy.add("first", "copy/yes");
    assertThat(lazyCopy.get("json")).isEqualTo("one/json");
    mappings.add("json", "two/json");
    lazyCopy.add("second", "copy/no");
    assertThat(lazyCopy.get("json")).isEqualTo("one/json");
  }

  @Test
  void mimeMappingsMatchesTomcatDefaults() throws IOException {
    Properties ourDefaultMimeMappings = PropertiesUtils
            .loadProperties(new ClassPathResource("cn/taketoday/framework/web/server/mime-mappings.properties", getClass()));
    Properties tomcatDefaultMimeMappings = PropertiesUtils
            .loadProperties(new ClassPathResource("MimeTypeMappings.properties", Tomcat.class));
    assertThat(ourDefaultMimeMappings).containsExactlyInAnyOrderEntriesOf(tomcatDefaultMimeMappings);
  }

}

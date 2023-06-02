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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.core.env.EnumerablePropertySource;
import cn.taketoday.core.env.PropertySource;
import cn.taketoday.core.io.ByteArrayResource;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/5/2 21:44
 */
class YamlPropertySourceLoaderTests {

  private final YamlPropertySourceLoader loader = new YamlPropertySourceLoader();

  @Test
  void load() throws Exception {
    ByteArrayResource resource = new ByteArrayResource("foo:\n  bar: spam".getBytes());
    PropertySource<?> source = this.loader.load("resource", resource).get(0);
    assertThat(source).isNotNull();
    assertThat(source.getProperty("foo.bar")).isEqualTo("spam");
  }

  @Test
  void orderedItems() throws Exception {
    StringBuilder yaml = new StringBuilder();
    List<String> expected = new ArrayList<>();
    for (char c = 'a'; c <= 'z'; c++) {
      yaml.append(c).append(": value").append(c).append("\n");
      expected.add(String.valueOf(c));
    }
    ByteArrayResource resource = new ByteArrayResource(yaml.toString().getBytes());
    EnumerablePropertySource<?> source = (EnumerablePropertySource<?>) this.loader.load("resource", resource)
            .get(0);
    assertThat(source).isNotNull();
    assertThat(source.getPropertyNames()).isEqualTo(StringUtils.toStringArray(expected));
  }

  @Test
  void mergeItems() throws Exception {
    StringBuilder yaml = new StringBuilder();
    yaml.append("foo:\n  bar: spam\n");
    yaml.append("---\n");
    yaml.append("foo:\n  baz: wham\n");
    ByteArrayResource resource = new ByteArrayResource(yaml.toString().getBytes());
    List<PropertySource<?>> loaded = this.loader.load("resource", resource);
    assertThat(loaded).hasSize(2);
    assertThat(loaded.get(0).getProperty("foo.bar")).isEqualTo("spam");
    assertThat(loaded.get(1).getProperty("foo.baz")).isEqualTo("wham");
  }

  @Test
  @Disabled
  void timestampLikeItemsDoNotBecomeDates() throws Exception {
    ByteArrayResource resource = new ByteArrayResource("foo: 2015-01-28".getBytes());
    PropertySource<?> source = this.loader.load("resource", resource).get(0);
    assertThat(source).isNotNull();
    assertThat(source.getProperty("foo")).isEqualTo("2015-01-28");
  }

  @Test
  void loadOriginAware() throws Exception {
    Resource resource = new ClassPathResource("test-yaml.yml", getClass());
    List<PropertySource<?>> loaded = this.loader.load("resource", resource);
    for (PropertySource<?> source : loaded) {
      EnumerablePropertySource<?> enumerableSource = (EnumerablePropertySource<?>) source;
      for (String name : enumerableSource.getPropertyNames()) {
        System.out.println(name + " = " + enumerableSource.getProperty(name));
      }
    }
  }

}
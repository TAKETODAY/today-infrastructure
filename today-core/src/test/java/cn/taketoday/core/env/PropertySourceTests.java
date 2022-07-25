/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.core.env;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link PropertySource} implementations.
 *
 * @author Chris Beams
 */
class PropertySourceTests {

  @Test
  @SuppressWarnings("serial")
  void equals() {
    Map<String, Object> map1 = new HashMap<String, Object>() {{
      put("a", "b");
    }};
    Map<String, Object> map2 = new HashMap<String, Object>() {{
      put("c", "d");
    }};
    Properties props1 = new Properties() {{
      setProperty("a", "b");
    }};
    Properties props2 = new Properties() {{
      setProperty("c", "d");
    }};

    MapPropertySource mps = new MapPropertySource("mps", map1);
    assertThat(mps).isEqualTo(mps);

    assertThat(new MapPropertySource("x", map1).equals(new MapPropertySource("x", map1))).isTrue();
    assertThat(new MapPropertySource("x", map1).equals(new MapPropertySource("x", map2))).isTrue();
    assertThat(new MapPropertySource("x", map1).equals(new PropertiesPropertySource("x", props1))).isTrue();
    assertThat(new MapPropertySource("x", map1).equals(new PropertiesPropertySource("x", props2))).isTrue();

    assertThat(new MapPropertySource("x", map1).equals(new Object())).isFalse();
    assertThat(new MapPropertySource("x", map1).equals("x")).isFalse();
    assertThat(new MapPropertySource("x", map1).equals(new MapPropertySource("y", map1))).isFalse();
    assertThat(new MapPropertySource("x", map1).equals(new MapPropertySource("y", map2))).isFalse();
    assertThat(new MapPropertySource("x", map1).equals(new PropertiesPropertySource("y", props1))).isFalse();
    assertThat(new MapPropertySource("x", map1).equals(new PropertiesPropertySource("y", props2))).isFalse();
  }

  @Test
  @SuppressWarnings("serial")
  void collectionsOperations() {
    Map<String, Object> map1 = new HashMap<String, Object>() {{
      put("a", "b");
    }};
    Map<String, Object> map2 = new HashMap<String, Object>() {{
      put("c", "d");
    }};

    PropertySource<?> ps1 = new MapPropertySource("ps1", map1);
    ps1.getSource();
    List<PropertySource<?>> propertySources = new ArrayList<>();
    assertThat(propertySources.add(ps1)).isEqualTo(true);
    assertThat(propertySources.contains(ps1)).isTrue();
    assertThat(propertySources.contains(PropertySource.named("ps1"))).isTrue();

    PropertySource<?> ps1replacement = new MapPropertySource("ps1", map2); // notice - different map
    assertThat(propertySources.add(ps1replacement)).isTrue(); // true because linkedlist allows duplicates
    assertThat(propertySources).hasSize(2);
    assertThat(propertySources.remove(PropertySource.named("ps1"))).isTrue();
    assertThat(propertySources).hasSize(1);
    assertThat(propertySources.remove(PropertySource.named("ps1"))).isTrue();
    assertThat(propertySources).hasSize(0);

    PropertySource<?> ps2 = new MapPropertySource("ps2", map2);
    propertySources.add(ps1);
    propertySources.add(ps2);
    assertThat(propertySources.indexOf(PropertySource.named("ps1"))).isEqualTo(0);
    assertThat(propertySources.indexOf(PropertySource.named("ps2"))).isEqualTo(1);
    propertySources.clear();
  }

}

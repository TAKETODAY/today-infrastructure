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

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CompositePropertySource}.
 *
 * @author Phillip Webb
 */
class CompositePropertySourceTests {

  @Test
  void addFirst() {
    PropertySource<?> p1 = new MapPropertySource("p1", Collections.emptyMap());
    PropertySource<?> p2 = new MapPropertySource("p2", Collections.emptyMap());
    PropertySource<?> p3 = new MapPropertySource("p3", Collections.emptyMap());
    CompositePropertySource composite = new CompositePropertySource("c");
    composite.addPropertySource(p2);
    composite.addPropertySource(p3);
    composite.addPropertySource(p1);
    composite.addFirstPropertySource(p1);
    String s = composite.toString();
    int i1 = s.indexOf("name='p1'");
    int i2 = s.indexOf("name='p2'");
    int i3 = s.indexOf("name='p3'");
    assertThat(((i1 < i2) && (i2 < i3))).as("Bad order: " + s).isTrue();
  }

}

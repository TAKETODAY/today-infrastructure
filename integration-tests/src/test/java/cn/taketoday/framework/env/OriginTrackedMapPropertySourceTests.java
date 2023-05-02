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

import java.util.LinkedHashMap;
import java.util.Map;

import cn.taketoday.origin.Origin;
import cn.taketoday.origin.OriginTrackedValue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link OriginTrackedMapPropertySource}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
class OriginTrackedMapPropertySourceTests {

  private final Map<String, Object> map = new LinkedHashMap<>();

  private final OriginTrackedMapPropertySource source = new OriginTrackedMapPropertySource("test", this.map);

  private final Origin origin = mock(Origin.class);

  @Test
  void getPropertyWhenMissingShouldReturnNull() {
    assertThat(this.source.getProperty("test")).isNull();
  }

  @Test
  void getPropertyWhenNonTrackedShouldReturnValue() {
    this.map.put("test", "foo");
    assertThat(this.source.getProperty("test")).isEqualTo("foo");
  }

  @Test
  void getPropertyWhenTrackedShouldReturnValue() {
    this.map.put("test", OriginTrackedValue.of("foo", this.origin));
    assertThat(this.source.getProperty("test")).isEqualTo("foo");
  }

  @Test
  void getPropertyOriginWhenMissingShouldReturnNull() {
    assertThat(this.source.getOrigin("test")).isNull();
  }

  @Test
  void getPropertyOriginWhenNonTrackedShouldReturnNull() {
    this.map.put("test", "foo");
    assertThat(this.source.getOrigin("test")).isNull();
  }

  @Test
  void getPropertyOriginWhenTrackedShouldReturnOrigin() {
    this.map.put("test", OriginTrackedValue.of("foo", this.origin));
    assertThat(this.source.getOrigin("test")).isEqualTo(this.origin);
  }

}

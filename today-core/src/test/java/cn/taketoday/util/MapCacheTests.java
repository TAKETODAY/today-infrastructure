/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.util;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.function.Function;

import cn.taketoday.core.Pair;
import cn.taketoday.lang.NullValue;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/3/27 13:53
 */
class MapCacheTests {

  @Test
  @SuppressWarnings({ "rawtypes", "unchecked" })
  void mappingFunction() {
    HashMap map = new HashMap<>();
    MapCache<String, String, Object> cache = new MapCache<>(map, Function.identity());
    assertThat(cache.get("1")).isEqualTo("1");
    assertThat(cache).extracting("mapping").isSameAs(map);
    assertThat(cache.get(null, null)).isNull();
    assertThat(cache.get(null)).isNull();

    assertThat(map).contains(Pair.of("1", "1"), Pair.of(null, NullValue.INSTANCE));

    cache.clear();
    assertThat(cache.get(null)).isNull();
    assertThat(map).contains(Pair.of(null, NullValue.INSTANCE));

    assertThat(cache.remove(null)).isSameAs(null);
    assertThat(map).isEmpty();
    assertThat(cache.put(null, null)).isSameAs(null);

    assertThat(map).isNotEmpty();
    assertThat(cache.get(null)).isNull();
  }

}
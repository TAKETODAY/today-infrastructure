/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author TODAY 2021/9/11 15:13
 */
public class ConcurrentLruCacheTests {

  private final ConcurrentLruCache<String, String> cache = new ConcurrentLruCache<>(2, key -> key + "value");

  @Test
  void zeroCapacity() {
    ConcurrentLruCache<String, String> cache = new ConcurrentLruCache<>(0, key -> key + "value");

    assertThat(cache.capacity()).isZero();
    assertThat(cache.size()).isZero();

    assertThat(cache.get("k1")).isEqualTo("k1value");
    assertThat(cache.size()).isZero();
    assertThat(cache.contains("k1")).isFalse();

    assertThat(cache.get("k2")).isEqualTo("k2value");
    assertThat(cache.size()).isZero();
    assertThat(cache.contains("k1")).isFalse();
    assertThat(cache.contains("k2")).isFalse();

    assertThat(cache.get("k3")).isEqualTo("k3value");
    assertThat(cache.size()).isZero();
    assertThat(cache.contains("k1")).isFalse();
    assertThat(cache.contains("k2")).isFalse();
    assertThat(cache.contains("k3")).isFalse();
  }

  @Test
  void getAndSize() {
    assertThat(this.cache.capacity()).isEqualTo(2);
    assertThat(this.cache.size()).isEqualTo(0);
    assertThat(this.cache.get("k1")).isEqualTo("k1value");
    assertThat(this.cache.size()).isEqualTo(1);
    assertThat(this.cache.contains("k1")).isTrue();
    assertThat(this.cache.get("k2")).isEqualTo("k2value");
    assertThat(this.cache.size()).isEqualTo(2);
    assertThat(this.cache.contains("k1")).isTrue();
    assertThat(this.cache.contains("k2")).isTrue();
    assertThat(this.cache.get("k3")).isEqualTo("k3value");
    assertThat(this.cache.size()).isEqualTo(2);
    assertThat(this.cache.contains("k1")).isFalse();
    assertThat(this.cache.contains("k2")).isTrue();
    assertThat(this.cache.contains("k3")).isTrue();
  }

  @Test
  void removeAndSize() {
    assertThat(this.cache.get("k1")).isEqualTo("k1value");
    assertThat(this.cache.get("k2")).isEqualTo("k2value");
    assertThat(this.cache.size()).isEqualTo(2);
    assertThat(this.cache.contains("k1")).isTrue();
    assertThat(this.cache.contains("k2")).isTrue();
    this.cache.remove("k2");
    assertThat(this.cache.size()).isEqualTo(1);
    assertThat(this.cache.contains("k1")).isTrue();
    assertThat(this.cache.contains("k2")).isFalse();
    assertThat(this.cache.get("k3")).isEqualTo("k3value");
    assertThat(this.cache.size()).isEqualTo(2);
    assertThat(this.cache.contains("k1")).isTrue();
    assertThat(this.cache.contains("k2")).isFalse();
    assertThat(this.cache.contains("k3")).isTrue();
  }

  @Test
  void clearAndSize() {
    assertThat(this.cache.get("k1")).isEqualTo("k1value");
    assertThat(this.cache.get("k2")).isEqualTo("k2value");
    assertThat(this.cache.size()).isEqualTo(2);
    assertThat(this.cache.contains("k1")).isTrue();
    assertThat(this.cache.contains("k2")).isTrue();
    this.cache.clear();
    assertThat(this.cache.size()).isEqualTo(0);
    assertThat(this.cache.contains("k1")).isFalse();
    assertThat(this.cache.contains("k2")).isFalse();
    assertThat(this.cache.get("k3")).isEqualTo("k3value");
    assertThat(this.cache.size()).isEqualTo(1);
    assertThat(this.cache.contains("k1")).isFalse();
    assertThat(this.cache.contains("k2")).isFalse();
    assertThat(this.cache.contains("k3")).isTrue();
  }

}

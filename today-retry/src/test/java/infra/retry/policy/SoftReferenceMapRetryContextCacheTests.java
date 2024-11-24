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

package infra.retry.policy;

import org.junit.jupiter.api.Test;

import infra.retry.context.RetryContextSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class SoftReferenceMapRetryContextCacheTests {

  SoftReferenceMapRetryContextCache cache = new SoftReferenceMapRetryContextCache();

  @Test
  public void testPut() {
    RetryContextSupport context = new RetryContextSupport(null);
    cache.put("foo", context);
    assertThat(cache.get("foo")).isEqualTo(context);
  }

  @Test
  public void testPutOverLimit() {
    RetryContextSupport context = new RetryContextSupport(null);
    cache.setCapacity(1);
    cache.put("foo", context);
    assertThatExceptionOfType(RetryCacheCapacityExceededException.class)
            .isThrownBy(() -> cache.put("foo", context));
  }

  @Test
  public void testRemove() {
    assertThat(cache.containsKey("foo")).isFalse();
    RetryContextSupport context = new RetryContextSupport(null);
    cache.put("foo", context);
    assertThat(cache.containsKey("foo")).isTrue();
    cache.remove("foo");
    assertThat(cache.containsKey("foo")).isFalse();
  }

}

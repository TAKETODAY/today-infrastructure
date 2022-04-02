/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.context.properties.source;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SoftReferenceConfigurationPropertyCache}.
 *
 * @author Phillip Webb
 */
class SoftReferenceConfigurationPropertyCacheTests {

  private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2020-01-02T09:00:00Z"), ZoneOffset.UTC);

  private Clock clock = FIXED_CLOCK;

  private AtomicInteger createCount = new AtomicInteger();

  private TestSoftReferenceConfigurationPropertyCache cache = new TestSoftReferenceConfigurationPropertyCache(false);

  @Test
  void getReturnsValueWithCorrectCounts() {
    get(this.cache).assertCounts(0, 0);
    get(this.cache).assertCounts(0, 1);
    get(this.cache).assertCounts(0, 2);
  }

  @Test
  void getWhenNeverExpireReturnsValueWithCorrectCounts() {
    this.cache = new TestSoftReferenceConfigurationPropertyCache(true);
    get(this.cache).assertCounts(0, 0);
    get(this.cache).assertCounts(0, 0);
    get(this.cache).assertCounts(0, 0);
  }

  @Test
  void enableEnablesCachingWithUnlimitedTimeToLive() {
    this.cache.enable();
    get(this.cache).assertCounts(0, 0);
    tick(Duration.ofDays(300));
    get(this.cache).assertCounts(0, 0);
  }

  @Test
  void setTimeToLiveEnablesCachingWithTimeToLive() {
    this.cache.setTimeToLive(Duration.ofDays(1));
    get(this.cache).assertCounts(0, 0);
    tick(Duration.ofHours(2));
    get(this.cache).assertCounts(0, 0);
    tick(Duration.ofDays(2));
    get(this.cache).assertCounts(0, 1);
  }

  @Test
  void setTimeToLiveWhenZeroDisablesCaching() {
    this.cache.setTimeToLive(Duration.ZERO);
    get(this.cache).assertCounts(0, 0);
    get(this.cache).assertCounts(0, 1);
    get(this.cache).assertCounts(0, 2);
  }

  @Test
  void setTimeToLiveWhenNullDisablesCaching() {
    this.cache.setTimeToLive(null);
    get(this.cache).assertCounts(0, 0);
    get(this.cache).assertCounts(0, 1);
    get(this.cache).assertCounts(0, 2);
  }

  @Test
  void clearExpiresCache() {
    this.cache.enable();
    get(this.cache).assertCounts(0, 0);
    get(this.cache).assertCounts(0, 0);
    this.cache.clear();
    get(this.cache).assertCounts(0, 1);

  }

  private Value get(SoftReferenceConfigurationPropertyCache<Value> cache) {
    return cache.get(this::createValue, this::updateValue);
  }

  private Value createValue() {
    return new Value(this.createCount.getAndIncrement(), -1);
  }

  private Value updateValue(Value value) {
    return new Value(value.createCount, value.refreshCount + 1);
  }

  private void tick(Duration duration) {
    this.clock = Clock.offset(this.clock, duration);
  }

  /**
   * Testable {@link SoftReferenceConfigurationPropertyCache} that actually uses real
   * references.
   */
  class TestSoftReferenceConfigurationPropertyCache extends SoftReferenceConfigurationPropertyCache<Value> {

    private Value value;

    TestSoftReferenceConfigurationPropertyCache(boolean neverExpire) {
      super(neverExpire);
    }

    @Override
    protected Value getValue() {
      return this.value;
    }

    @Override
    protected void setValue(Value value) {
      this.value = value;
    }

    @Override
    protected Instant now() {
      return SoftReferenceConfigurationPropertyCacheTests.this.clock.instant();
    }

  }

  /**
   * Value used for testing.
   */
  static class Value {

    private final int createCount;

    private int refreshCount;

    Value(int createCount, int refreshCount) {
      this.createCount = createCount;
      this.refreshCount = refreshCount;
    }

    void assertCounts(int expectedCreateCount, int expectedRefreshCount) {
      assertThat(this.createCount).as("created").isEqualTo(expectedCreateCount);
      assertThat(this.refreshCount).as("refreshed").isEqualTo(expectedRefreshCount);
    }

  }

}

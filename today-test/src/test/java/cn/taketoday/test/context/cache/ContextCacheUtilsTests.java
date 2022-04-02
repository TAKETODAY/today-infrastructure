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

package cn.taketoday.test.context.cache;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.lang.TodayStrategies;

import static cn.taketoday.test.context.cache.ContextCache.DEFAULT_MAX_CONTEXT_CACHE_SIZE;
import static cn.taketoday.test.context.cache.ContextCache.MAX_CONTEXT_CACHE_SIZE_PROPERTY_NAME;
import static cn.taketoday.test.context.cache.ContextCacheUtils.retrieveMaxCacheSize;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ContextCacheUtils}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
class ContextCacheUtilsTests {

  @BeforeEach
  @AfterEach
  void clearProperties() {
    System.clearProperty(MAX_CONTEXT_CACHE_SIZE_PROPERTY_NAME);
    TodayStrategies.setProperty(MAX_CONTEXT_CACHE_SIZE_PROPERTY_NAME, null);
  }

  @Test
  void retrieveMaxCacheSizeFromDefault() {
    assertDefaultValue();
  }

  @Test
  void retrieveMaxCacheSizeFromBogusSystemProperty() {
    System.setProperty(MAX_CONTEXT_CACHE_SIZE_PROPERTY_NAME, "bogus");
    assertDefaultValue();
  }

  @Test
  void retrieveMaxCacheSizeFromBogusSpringProperty() {
    TodayStrategies.setProperty(MAX_CONTEXT_CACHE_SIZE_PROPERTY_NAME, "bogus");
    assertDefaultValue();
  }

  @Test
  void retrieveMaxCacheSizeFromDecimalSpringProperty() {
    TodayStrategies.setProperty(MAX_CONTEXT_CACHE_SIZE_PROPERTY_NAME, "3.14");
    assertDefaultValue();
  }

  @Test
  void retrieveMaxCacheSizeFromSystemProperty() {
    System.setProperty(MAX_CONTEXT_CACHE_SIZE_PROPERTY_NAME, "42");
    assertThat(retrieveMaxCacheSize()).isEqualTo(42);
  }

  @Test
  void retrieveMaxCacheSizeFromSystemPropertyContainingWhitespace() {
    System.setProperty(MAX_CONTEXT_CACHE_SIZE_PROPERTY_NAME, "42\t");
    assertThat(retrieveMaxCacheSize()).isEqualTo(42);
  }

  @Test
  void retrieveMaxCacheSizeFromSpringProperty() {
    TodayStrategies.setProperty(MAX_CONTEXT_CACHE_SIZE_PROPERTY_NAME, "99");
    assertThat(retrieveMaxCacheSize()).isEqualTo(99);
  }

  private static void assertDefaultValue() {
    assertThat(retrieveMaxCacheSize()).isEqualTo(DEFAULT_MAX_CONTEXT_CACHE_SIZE);
  }

}

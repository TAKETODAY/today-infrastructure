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

package cn.taketoday.test.context.cache;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.test.context.MergedContextConfiguration;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for the LRU eviction policy in {@link DefaultContextCache}.
 *
 * @author Sam Brannen
 * @see ContextCacheTests
 * @since 4.0
 */
class LruContextCacheTests {

  private static final MergedContextConfiguration abcConfig = config(Abc.class);
  private static final MergedContextConfiguration fooConfig = config(Foo.class);
  private static final MergedContextConfiguration barConfig = config(Bar.class);
  private static final MergedContextConfiguration bazConfig = config(Baz.class);

  private final ConfigurableApplicationContext abcContext = mock();
  private final ConfigurableApplicationContext fooContext = mock();
  private final ConfigurableApplicationContext barContext = mock();
  private final ConfigurableApplicationContext bazContext = mock();

  @Test
  void maxCacheSizeNegativeOne() {
    assertThatIllegalArgumentException().isThrownBy(() -> new DefaultContextCache(-1));
  }

  @Test
  void maxCacheSizeZero() {
    assertThatIllegalArgumentException().isThrownBy(() -> new DefaultContextCache(0));
  }

  @Test
  void maxCacheSizeOne() {
    DefaultContextCache cache = new DefaultContextCache(1);
    assertThat(cache.size()).isEqualTo(0);
    assertThat(cache.getMaxSize()).isEqualTo(1);

    cache.put(fooConfig, fooContext);
    assertCacheContents(cache, "Foo");

    cache.put(fooConfig, fooContext);
    assertCacheContents(cache, "Foo");

    cache.put(barConfig, barContext);
    assertCacheContents(cache, "Bar");

    cache.put(fooConfig, fooContext);
    assertCacheContents(cache, "Foo");
  }

  @Test
  void maxCacheSizeThree() {
    DefaultContextCache cache = new DefaultContextCache(3);
    assertThat(cache.size()).isEqualTo(0);
    assertThat(cache.getMaxSize()).isEqualTo(3);

    cache.put(fooConfig, fooContext);
    assertCacheContents(cache, "Foo");

    cache.put(fooConfig, fooContext);
    assertCacheContents(cache, "Foo");

    cache.put(barConfig, barContext);
    assertCacheContents(cache, "Foo", "Bar");

    cache.put(bazConfig, bazContext);
    assertCacheContents(cache, "Foo", "Bar", "Baz");

    cache.put(abcConfig, abcContext);
    assertCacheContents(cache, "Bar", "Baz", "Abc");
  }

  @Test
  void ensureLruOrderingIsUpdated() {
    DefaultContextCache cache = new DefaultContextCache(3);

    // Note: when a new entry is added it is considered the MRU entry and inserted at the tail.
    cache.put(fooConfig, fooContext);
    cache.put(barConfig, barContext);
    cache.put(bazConfig, bazContext);
    assertCacheContents(cache, "Foo", "Bar", "Baz");

    // Note: the MRU entry is moved to the tail when accessed.
    cache.get(fooConfig);
    assertCacheContents(cache, "Bar", "Baz", "Foo");

    cache.get(barConfig);
    assertCacheContents(cache, "Baz", "Foo", "Bar");

    cache.get(bazConfig);
    assertCacheContents(cache, "Foo", "Bar", "Baz");

    cache.get(barConfig);
    assertCacheContents(cache, "Foo", "Baz", "Bar");
  }

  @Test
  void ensureEvictedContextsAreClosed() {
    DefaultContextCache cache = new DefaultContextCache(2);

    cache.put(fooConfig, fooContext);
    cache.put(barConfig, barContext);
    assertCacheContents(cache, "Foo", "Bar");

    cache.put(bazConfig, bazContext);
    assertCacheContents(cache, "Bar", "Baz");
    verify(fooContext, times(1)).close();

    cache.put(abcConfig, abcContext);
    assertCacheContents(cache, "Baz", "Abc");
    verify(barContext, times(1)).close();

    verify(abcContext, never()).close();
    verify(bazContext, never()).close();
  }

  private static MergedContextConfiguration config(Class<?> clazz) {
    return new MergedContextConfiguration(null, null, new Class<?>[] { clazz }, null, null);
  }

  @SuppressWarnings("unchecked")
  private static void assertCacheContents(DefaultContextCache cache, String... expectedNames) {
    assertThat(cache).extracting("contextMap", as(map(MergedContextConfiguration.class, ApplicationContext.class)))
            .satisfies(contextMap -> {
              List<String> actualNames = contextMap.keySet().stream()
                      .map(MergedContextConfiguration::getClasses)
                      .flatMap(Arrays::stream)
                      .map(Class::getSimpleName)
                      .toList();
              assertThat(actualNames).containsExactly(expectedNames);
            });
  }

  private static class Abc { }

  private static class Foo { }

  private static class Bar { }

  private static class Baz { }

}

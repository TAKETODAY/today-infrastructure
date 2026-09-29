/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.test.context.cache;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.Arrays;
import java.util.List;

import infra.context.ApplicationContext;
import infra.context.ConfigurableApplicationContext;
import infra.test.context.MergedContextConfiguration;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import static org.mockito.Mockito.inOrder;
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
  void clearClosesContexts() {
    DefaultContextCache cache = new DefaultContextCache(4);

    cache.put(fooConfig, fooContext);
    cache.put(barConfig, barContext);
    cache.put(bazConfig, bazContext);
    cache.registerContextUsage(fooConfig, getClass());
    cache.registerContextUsage(barConfig, getClass());
    cache.registerContextUsage(bazConfig, getClass());
    assertCacheContents(cache, "Foo", "Bar", "Baz");
    assertThat(cache.getContextUsageCount()).isEqualTo(3);

    cache.clear();
    assertThat(cache.size()).isZero();
    assertThat(cache.getParentContextCount()).isZero();
    assertThat(cache.getContextUsageCount()).isZero();

    verify(fooContext, times(1)).close();
    verify(barContext, times(1)).close();
    verify(bazContext, times(1)).close();
    verify(abcContext, never()).close();
  }

  @Test
  void resetClosesContexts() {
    DefaultContextCache cache = new DefaultContextCache(4);

    cache.put(fooConfig, fooContext);
    cache.put(barConfig, barContext);
    cache.get(fooConfig);
    cache.get(abcConfig);
    assertThat(cache.getHitCount()).isEqualTo(1);
    assertThat(cache.getMissCount()).isEqualTo(1);
    assertCacheContents(cache, "Bar", "Foo");

    cache.reset();
    assertThat(cache.size()).isZero();
    assertThat(cache.getHitCount()).isZero();
    assertThat(cache.getMissCount()).isZero();
    assertThat(cache.getParentContextCount()).isZero();
    assertThat(cache.getContextUsageCount()).isZero();

    verify(fooContext, times(1)).close();
    verify(barContext, times(1)).close();
  }

  @Test
  void clearClosesContextHierarchyBottomUp() {
    DefaultContextCache cache = new DefaultContextCache(4);

    MergedContextConfiguration parentConfig = config(Foo.class);
    MergedContextConfiguration childConfig = config(Bar.class, parentConfig);
    MergedContextConfiguration grandchildConfig = config(Baz.class, childConfig);

    cache.put(parentConfig, fooContext);
    cache.put(childConfig, barContext);
    cache.put(grandchildConfig, bazContext);
    assertCacheContents(cache, "Foo", "Bar", "Baz");
    assertThat(cache.getParentContextCount()).isEqualTo(2);

    cache.clear();
    assertThat(cache.size()).isZero();
    assertThat(cache.getParentContextCount()).isZero();

    InOrder inOrder = inOrder(bazContext, barContext, fooContext);
    inOrder.verify(bazContext).close();
    inOrder.verify(barContext).close();
    inOrder.verify(fooContext).close();
  }

  @Test
  void resetClosesContextHierarchyBottomUp() {
    DefaultContextCache cache = new DefaultContextCache(4);

    MergedContextConfiguration parentConfig = config(Foo.class);
    MergedContextConfiguration childConfig = config(Bar.class, parentConfig);

    cache.put(parentConfig, fooContext);
    cache.put(childConfig, barContext);
    assertCacheContents(cache, "Foo", "Bar");
    assertThat(cache.getParentContextCount()).isEqualTo(1);

    cache.reset();
    assertThat(cache.size()).isZero();
    assertThat(cache.getParentContextCount()).isZero();

    InOrder inOrder = inOrder(barContext, fooContext);
    inOrder.verify(barContext).close();
    inOrder.verify(fooContext).close();
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

  private static MergedContextConfiguration config(Class<?> clazz, MergedContextConfiguration parent) {
    return new MergedContextConfiguration(null, null, new Class<?>[] { clazz }, null, null, null, null, parent);
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

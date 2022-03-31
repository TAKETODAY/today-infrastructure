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

import org.junit.jupiter.api.Test;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.test.context.MergedContextConfiguration;
import cn.taketoday.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for the LRU eviction policy in {@link DefaultContextCache}.
 *
 * @author Sam Brannen
 * @since 4.0
 * @see ContextCacheTests
 */
class LruContextCacheTests {

	private static final MergedContextConfiguration abcConfig = config(Abc.class);
	private static final MergedContextConfiguration fooConfig = config(Foo.class);
	private static final MergedContextConfiguration barConfig = config(Bar.class);
	private static final MergedContextConfiguration bazConfig = config(Baz.class);


	private final ConfigurableApplicationContext abcContext = mock(ConfigurableApplicationContext.class);
	private final ConfigurableApplicationContext fooContext = mock(ConfigurableApplicationContext.class);
	private final ConfigurableApplicationContext barContext = mock(ConfigurableApplicationContext.class);
	private final ConfigurableApplicationContext bazContext = mock(ConfigurableApplicationContext.class);


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

		Map<MergedContextConfiguration, ApplicationContext> contextMap =
				(Map<MergedContextConfiguration, ApplicationContext>) ReflectionTestUtils.getField(cache, "contextMap");

		// @formatter:off
		List<String> actualNames = contextMap.keySet().stream()
			.map(cfg -> cfg.getClasses()[0])
			.map(Class::getSimpleName)
			.collect(toList());
		// @formatter:on

		assertThat(actualNames).isEqualTo(asList(expectedNames));
	}


	private static class Abc {}
	private static class Foo {}
	private static class Bar {}
	private static class Baz {}

}

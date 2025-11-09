/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.cache.transaction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;

import infra.cache.Cache;
import infra.cache.CacheManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/5 13:33
 */
@ExtendWith(MockitoExtension.class)
class TransactionAwareCacheManagerProxyTests {

  @Mock
  CacheManager targetCacheManager;

  @Mock
  Cache targetCache;

  private TransactionAwareCacheManagerProxy proxy;

  @BeforeEach
  void setUp() {
    proxy = new TransactionAwareCacheManagerProxy(targetCacheManager);
  }

  @Test
  void noTargetCacheManagerThrowsException() {
    TransactionAwareCacheManagerProxy proxy = new TransactionAwareCacheManagerProxy();
    assertThatIllegalArgumentException()
            .isThrownBy(proxy::afterPropertiesSet)
            .withMessage("Property 'targetCacheManager' is required");
  }

  @Test
  void getCacheReturnsTransactionAwareDecorator() {
    when(targetCacheManager.getCache("test")).thenReturn(targetCache);

    Cache cache = proxy.getCache("test");

    assertThat(cache).isNotNull()
            .isInstanceOf(TransactionAwareCacheDecorator.class);
  }

  @Test
  void getCacheReturnsNullWhenTargetReturnsNull() {
    when(targetCacheManager.getCache("test")).thenReturn(null);

    Cache cache = proxy.getCache("test");

    assertThat(cache).isNull();
  }

  @Test
  void getCacheNamesReturnsDelegatedCollection() {
    Collection<String> names = List.of("cache1", "cache2");
    when(targetCacheManager.getCacheNames()).thenReturn(names);

    Collection<String> result = proxy.getCacheNames();

    assertThat(result).isEqualTo(names);
  }

  @Test
  void constructorRejectsNullTargetCacheManager() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new TransactionAwareCacheManagerProxy(null))
            .withMessage("Target CacheManager is required");
  }
}

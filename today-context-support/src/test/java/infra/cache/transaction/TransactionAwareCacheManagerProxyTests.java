/*
 * Copyright 2017 - 2026 the TODAY authors.
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

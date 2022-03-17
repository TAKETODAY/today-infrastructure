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

package cn.taketoday.cache;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.cache.annotation.CachePut;
import cn.taketoday.cache.annotation.Cacheable;
import cn.taketoday.cache.annotation.Caching;
import cn.taketoday.cache.annotation.CachingConfigurer;
import cn.taketoday.cache.annotation.EnableCaching;
import cn.taketoday.cache.concurrent.ConcurrentMapCache;
import cn.taketoday.cache.concurrent.ConcurrentMapCacheManager;
import cn.taketoday.cache.interceptor.AbstractCacheResolver;
import cn.taketoday.cache.interceptor.CacheOperationInvocationContext;
import cn.taketoday.cache.interceptor.CacheResolver;
import cn.taketoday.cache.support.SimpleCacheManager;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests to reproduce raised caching issues.
 *
 * @author Phillip Webb
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 */
public class CacheReproTests {

  @Test
  public void spr11124MultipleAnnotations() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Spr11124Config.class);
    Spr11124Service bean = context.getBean(Spr11124Service.class);
    bean.single(2);
    bean.single(2);
    bean.multiple(2);
    bean.multiple(2);
    context.close();
  }

  @Test
  public void spr11249PrimitiveVarargs() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Spr11249Config.class);
    Spr11249Service bean = context.getBean(Spr11249Service.class);
    Object result = bean.doSomething("op", 2, 3);
    assertThat(bean.doSomething("op", 2, 3)).isSameAs(result);
    context.close();
  }

  @Test
  public void spr11592GetSimple() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Spr11592Config.class);
    Spr11592Service bean = context.getBean(Spr11592Service.class);
    Cache cache = context.getBean("cache", Cache.class);

    String key = "1";
    Object result = bean.getSimple("1");
    verify(cache, times(1)).get(key);  // first call: cache miss

    Object cachedResult = bean.getSimple("1");
    assertThat(cachedResult).isSameAs(result);
    verify(cache, times(2)).get(key);  // second call: cache hit

    context.close();
  }

  @Test
  public void spr11592GetNeverCache() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Spr11592Config.class);
    Spr11592Service bean = context.getBean(Spr11592Service.class);
    Cache cache = context.getBean("cache", Cache.class);

    String key = "1";
    Object result = bean.getNeverCache("1");
    verify(cache, times(0)).get(key);  // no cache hit at all, caching disabled

    Object cachedResult = bean.getNeverCache("1");
    assertThat(cachedResult).isNotSameAs(result);
    verify(cache, times(0)).get(key);  // caching disabled

    context.close();
  }

  @Test
  public void spr13081ConfigNoCacheNameIsRequired() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Spr13081Config.class);
    MyCacheResolver cacheResolver = context.getBean(MyCacheResolver.class);
    Spr13081Service bean = context.getBean(Spr13081Service.class);

    assertThat(cacheResolver.getCache("foo").get("foo")).isNull();
    Object result = bean.getSimple("foo");  // cache name = id
    assertThat(cacheResolver.getCache("foo").get("foo").get()).isEqualTo(result);
  }

  @Test
  public void spr13081ConfigFailIfCacheResolverReturnsNullCacheName() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Spr13081Config.class);
    Spr13081Service bean = context.getBean(Spr13081Service.class);

    assertThatIllegalStateException().isThrownBy(() ->
                    bean.getSimple(null))
            .withMessageContaining(MyCacheResolver.class.getName());
  }

  @Test
  public void spr14230AdaptsToOptional() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Spr14230Config.class);
    Spr14230Service bean = context.getBean(Spr14230Service.class);
    Cache cache = context.getBean(CacheManager.class).getCache("itemCache");

    TestBean tb = new TestBean("tb1");
    bean.insertItem(tb);
    assertThat(bean.findById("tb1").get()).isSameAs(tb);
    assertThat(cache.get("tb1").get()).isSameAs(tb);

    cache.clear();
    TestBean tb2 = bean.findById("tb1").get();
    assertThat(tb2).isNotSameAs(tb);
    assertThat(cache.get("tb1").get()).isSameAs(tb2);
  }

  @Test
  public void spr14853AdaptsToOptionalWithSync() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Spr14853Config.class);
    Spr14853Service bean = context.getBean(Spr14853Service.class);
    Cache cache = context.getBean(CacheManager.class).getCache("itemCache");

    TestBean tb = new TestBean("tb1");
    bean.insertItem(tb);
    assertThat(bean.findById("tb1").get()).isSameAs(tb);
    assertThat(cache.get("tb1").get()).isSameAs(tb);

    cache.clear();
    TestBean tb2 = bean.findById("tb1").get();
    assertThat(tb2).isNotSameAs(tb);
    assertThat(cache.get("tb1").get()).isSameAs(tb2);
  }

  @Test
  public void spr15271FindsOnInterfaceWithInterfaceProxy() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Spr15271ConfigA.class);
    Spr15271Interface bean = context.getBean(Spr15271Interface.class);
    Cache cache = context.getBean(CacheManager.class).getCache("itemCache");

    TestBean tb = new TestBean("tb1");
    bean.insertItem(tb);
    assertThat(bean.findById("tb1").get()).isSameAs(tb);
    assertThat(cache.get("tb1").get()).isSameAs(tb);
  }

  @Test
  public void spr15271FindsOnInterfaceWithCglibProxy() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Spr15271ConfigB.class);
    Spr15271Interface bean = context.getBean(Spr15271Interface.class);
    Cache cache = context.getBean(CacheManager.class).getCache("itemCache");

    TestBean tb = new TestBean("tb1");
    bean.insertItem(tb);
    assertThat(bean.findById("tb1").get()).isSameAs(tb);
    assertThat(cache.get("tb1").get()).isSameAs(tb);
  }

  @Configuration
  @EnableCaching
  public static class Spr11124Config {

    @Bean
    public CacheManager cacheManager() {
      return new ConcurrentMapCacheManager();
    }

    @Bean
    public Spr11124Service service() {
      return new Spr11124ServiceImpl();
    }
  }

  public interface Spr11124Service {

    List<String> single(int id);

    List<String> multiple(int id);
  }

  public static class Spr11124ServiceImpl implements Spr11124Service {

    private int multipleCount = 0;

    @Override
    @Cacheable("smallCache")
    public List<String> single(int id) {
      if (this.multipleCount > 0) {
        throw new AssertionError("Called too many times");
      }
      this.multipleCount++;
      return Collections.emptyList();
    }

    @Override
    @Caching(cacheable = {
            @Cacheable(cacheNames = "bigCache", unless = "#result.size() < 4"),
            @Cacheable(cacheNames = "smallCache", unless = "#result.size() > 3") })
    public List<String> multiple(int id) {
      if (this.multipleCount > 0) {
        throw new AssertionError("Called too many times");
      }
      this.multipleCount++;
      return Collections.emptyList();
    }
  }

  @Configuration
  @EnableCaching
  public static class Spr11249Config {

    @Bean
    public CacheManager cacheManager() {
      return new ConcurrentMapCacheManager();
    }

    @Bean
    public Spr11249Service service() {
      return new Spr11249Service();
    }
  }

  public static class Spr11249Service {

    @Cacheable("smallCache")
    public Object doSomething(String name, int... values) {
      return new Object();
    }
  }

  @Configuration
  @EnableCaching
  public static class Spr11592Config {

    @Bean
    public CacheManager cacheManager() {
      SimpleCacheManager cacheManager = new SimpleCacheManager();
      cacheManager.setCaches(Collections.singletonList(cache()));
      return cacheManager;
    }

    @Bean
    public Cache cache() {
      Cache cache = new ConcurrentMapCache("cache");
      return Mockito.spy(cache);
    }

    @Bean
    public Spr11592Service service() {
      return new Spr11592Service();
    }
  }

  public static class Spr11592Service {

    @Cacheable("cache")
    public Object getSimple(String key) {
      return new Object();
    }

    @Cacheable(cacheNames = "cache", condition = "false")
    public Object getNeverCache(String key) {
      return new Object();
    }
  }

  @Configuration
  @EnableCaching
  public static class Spr13081Config implements CachingConfigurer {

    @Bean
    @Override
    public CacheResolver cacheResolver() {
      return new MyCacheResolver();
    }

    @Bean
    public Spr13081Service service() {
      return new Spr13081Service();
    }
  }

  public static class MyCacheResolver extends AbstractCacheResolver {

    public MyCacheResolver() {
      super(new ConcurrentMapCacheManager());
    }

    @Override
    @Nullable
    protected Collection<String> getCacheNames(CacheOperationInvocationContext<?> context) {
      String cacheName = (String) context.getArgs()[0];
      if (cacheName != null) {
        return Collections.singleton(cacheName);
      }
      return null;
    }

    public Cache getCache(String name) {
      return getCacheManager().getCache(name);
    }
  }

  public static class Spr13081Service {

    @Cacheable
    public Object getSimple(String cacheName) {
      return new Object();
    }
  }

  public static class Spr14230Service {

    @Cacheable("itemCache")
    public Optional<TestBean> findById(String id) {
      return Optional.of(new TestBean(id));
    }

    @CachePut(cacheNames = "itemCache", key = "#item.name")
    public TestBean insertItem(TestBean item) {
      return item;
    }
  }

  @Configuration
  @EnableCaching
  public static class Spr14230Config {

    @Bean
    public CacheManager cacheManager() {
      return new ConcurrentMapCacheManager();
    }

    @Bean
    public Spr14230Service service() {
      return new Spr14230Service();
    }
  }

  public static class Spr14853Service {

    @Cacheable(value = "itemCache", sync = true)
    public Optional<TestBean> findById(String id) {
      return Optional.of(new TestBean(id));
    }

    @CachePut(cacheNames = "itemCache", key = "#item.name")
    public TestBean insertItem(TestBean item) {
      return item;
    }
  }

  @Configuration
  @EnableCaching
  public static class Spr14853Config {

    @Bean
    public CacheManager cacheManager() {
      return new ConcurrentMapCacheManager();
    }

    @Bean
    public Spr14853Service service() {
      return new Spr14853Service();
    }
  }

  public interface Spr15271Interface {

    @Cacheable(value = "itemCache", sync = true)
    Optional<TestBean> findById(String id);

    @CachePut(cacheNames = "itemCache", key = "#item.name")
    TestBean insertItem(TestBean item);
  }

  public static class Spr15271Service implements Spr15271Interface {

    @Override
    public Optional<TestBean> findById(String id) {
      return Optional.of(new TestBean(id));
    }

    @Override
    public TestBean insertItem(TestBean item) {
      return item;
    }
  }

  @Configuration
  @EnableCaching
  public static class Spr15271ConfigA {

    @Bean
    public CacheManager cacheManager() {
      return new ConcurrentMapCacheManager();
    }

    @Bean
    public Spr15271Interface service() {
      return new Spr15271Service();
    }
  }

  @Configuration
  @EnableCaching(proxyTargetClass = true)
  public static class Spr15271ConfigB {

    @Bean
    public CacheManager cacheManager() {
      return new ConcurrentMapCacheManager();
    }

    @Bean
    public Spr15271Interface service() {
      return new Spr15271Service();
    }
  }

}

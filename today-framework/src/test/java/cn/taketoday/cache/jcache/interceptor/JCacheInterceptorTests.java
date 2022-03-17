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

package cn.taketoday.cache.jcache.interceptor;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.cache.CacheManager;
import cn.taketoday.cache.interceptor.CacheOperationInvoker;
import cn.taketoday.cache.interceptor.CacheResolver;
import cn.taketoday.cache.interceptor.KeyGenerator;
import cn.taketoday.cache.interceptor.NamedCacheResolver;
import cn.taketoday.cache.jcache.AbstractJCacheTests;
import cn.taketoday.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Stephane Nicoll
 */
public class JCacheInterceptorTests extends AbstractJCacheTests {

  private final CacheOperationInvoker dummyInvoker = new DummyInvoker(null);

  @Test
  public void severalCachesNotSupported() {
    JCacheInterceptor interceptor = createInterceptor(createOperationSource(
            cacheManager, new NamedCacheResolver(cacheManager, "default", "simpleCache"),
            defaultExceptionCacheResolver, defaultKeyGenerator));

    AnnotatedJCacheableService service = new AnnotatedJCacheableService(cacheManager.getCache("default"));
    Method m = ReflectionUtils.findMethod(AnnotatedJCacheableService.class, "cache", String.class);

    assertThatIllegalStateException().isThrownBy(() ->
                    interceptor.execute(dummyInvoker, service, m, new Object[] { "myId" }))
            .withMessageContaining("JSR-107 only supports a single cache");
  }

  @Test
  public void noCacheCouldBeResolved() {
    JCacheInterceptor interceptor = createInterceptor(createOperationSource(
            cacheManager, new NamedCacheResolver(cacheManager), // Returns empty list
            defaultExceptionCacheResolver, defaultKeyGenerator));

    AnnotatedJCacheableService service = new AnnotatedJCacheableService(cacheManager.getCache("default"));
    Method m = ReflectionUtils.findMethod(AnnotatedJCacheableService.class, "cache", String.class);
    assertThatIllegalStateException().isThrownBy(() ->
                    interceptor.execute(dummyInvoker, service, m, new Object[] { "myId" }))
            .withMessageContaining("Cache could not have been resolved for");
  }

  @Test
  public void cacheManagerMandatoryIfCacheResolverNotSet() {
    assertThatIllegalStateException().isThrownBy(() ->
            createOperationSource(null, null, null, defaultKeyGenerator));
  }

  @Test
  public void cacheManagerOptionalIfCacheResolversSet() {
    createOperationSource(null, defaultCacheResolver, defaultExceptionCacheResolver, defaultKeyGenerator);
  }

  @Test
  public void cacheResultReturnsProperType() throws Throwable {
    JCacheInterceptor interceptor = createInterceptor(createOperationSource(
            cacheManager, defaultCacheResolver, defaultExceptionCacheResolver, defaultKeyGenerator));

    AnnotatedJCacheableService service = new AnnotatedJCacheableService(cacheManager.getCache("default"));
    Method method = ReflectionUtils.findMethod(AnnotatedJCacheableService.class, "cache", String.class);

    CacheOperationInvoker invoker = new DummyInvoker(0L);
    Object execute = interceptor.execute(invoker, service, method, new Object[] { "myId" });
    assertThat(execute).as("result cannot be null.").isNotNull();
    assertThat(execute.getClass()).as("Wrong result type").isEqualTo(Long.class);
    assertThat(execute).as("Wrong result").isEqualTo(0L);
  }

  protected JCacheOperationSource createOperationSource(CacheManager cacheManager,
          CacheResolver cacheResolver, CacheResolver exceptionCacheResolver, KeyGenerator keyGenerator) {

    DefaultJCacheOperationSource source = new DefaultJCacheOperationSource();
    source.setCacheManager(cacheManager);
    source.setCacheResolver(cacheResolver);
    source.setExceptionCacheResolver(exceptionCacheResolver);
    source.setKeyGenerator(keyGenerator);
    source.setBeanFactory(new StandardBeanFactory());
    source.afterSingletonsInstantiated();
    return source;
  }

  protected JCacheInterceptor createInterceptor(JCacheOperationSource source) {
    JCacheInterceptor interceptor = new JCacheInterceptor();
    interceptor.setCacheOperationSource(source);
    interceptor.afterPropertiesSet();
    return interceptor;
  }

  private static class DummyInvoker implements CacheOperationInvoker {

    private final Object result;

    private DummyInvoker(Object result) {
      this.result = result;
    }

    @Override
    public Object invoke() throws ThrowableWrapper {
      return result;
    }
  }

}

/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Comparator;

import javax.cache.annotation.CacheDefaults;
import javax.cache.annotation.CacheKeyGenerator;
import javax.cache.annotation.CacheRemove;
import javax.cache.annotation.CacheRemoveAll;
import javax.cache.annotation.CacheResult;

import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.cache.interceptor.CacheResolver;
import cn.taketoday.cache.interceptor.KeyGenerator;
import cn.taketoday.cache.jcache.AbstractJCacheTests;
import cn.taketoday.cache.jcache.TestableCacheKeyGenerator;
import cn.taketoday.cache.jcache.TestableCacheResolver;
import cn.taketoday.cache.jcache.TestableCacheResolverFactory;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Stephane Nicoll
 */
public class AnnotationCacheOperationSourceTests extends AbstractJCacheTests {

  private final DefaultJCacheOperationSource source = new DefaultJCacheOperationSource();

  private final StandardBeanFactory beanFactory = new StandardBeanFactory();

  @BeforeEach
  public void setup() {
    source.setCacheResolver(defaultCacheResolver);
    source.setExceptionCacheResolver(defaultExceptionCacheResolver);
    source.setKeyGenerator(defaultKeyGenerator);
    source.setBeanFactory(beanFactory);
  }

  @Test
  public void cache() {
    CacheResultOperation op = getDefaultCacheOperation(CacheResultOperation.class, String.class);
    assertDefaults(op);
    assertThat(op.getExceptionCacheResolver()).as("Exception caching not enabled so resolver should not be set").isNull();
  }

  @Test
  public void cacheWithException() {
    CacheResultOperation op = getDefaultCacheOperation(CacheResultOperation.class, String.class, boolean.class);
    assertDefaults(op);
    assertThat(op.getExceptionCacheResolver()).isEqualTo(defaultExceptionCacheResolver);
    assertThat(op.getExceptionCacheName()).isEqualTo("exception");
  }

  @Test
  public void put() {
    CachePutOperation op = getDefaultCacheOperation(CachePutOperation.class, String.class, Object.class);
    assertDefaults(op);
  }

  @Test
  public void remove() {
    CacheRemoveOperation op = getDefaultCacheOperation(CacheRemoveOperation.class, String.class);
    assertDefaults(op);
  }

  @Test
  public void removeAll() {
    CacheRemoveAllOperation op = getDefaultCacheOperation(CacheRemoveAllOperation.class);
    assertThat(op.getCacheResolver()).isEqualTo(defaultCacheResolver);
  }

  @Test
  public void noAnnotation() {
    assertThat(getCacheOperation(AnnotatedJCacheableService.class, this.cacheName)).isNull();
  }

  @Test
  public void multiAnnotations() {
    assertThatIllegalStateException().isThrownBy(() -> getCacheOperation(InvalidCases.class, this.cacheName));
  }

  @Test
  public void defaultCacheNameWithCandidate() {
    Method method = ReflectionUtils.findMethod(Object.class, "toString");
    assertThat(source.determineCacheName(method, null, "foo")).isEqualTo("foo");
  }

  @Test
  public void defaultCacheNameWithDefaults() {
    Method method = ReflectionUtils.findMethod(Object.class, "toString");
    CacheDefaults mock = mock(CacheDefaults.class);
    given(mock.cacheName()).willReturn("");
    assertThat(source.determineCacheName(method, mock, "")).isEqualTo("java.lang.Object.toString()");
  }

  @Test
  public void defaultCacheNameNoDefaults() {
    Method method = ReflectionUtils.findMethod(Object.class, "toString");
    assertThat(source.determineCacheName(method, null, "")).isEqualTo("java.lang.Object.toString()");
  }

  @Test
  public void defaultCacheNameWithParameters() {
    Method method = ReflectionUtils.findMethod(Comparator.class, "compare", Object.class, Object.class);
    assertThat(source.determineCacheName(method, null, "")).isEqualTo("java.util.Comparator.compare(java.lang.Object,java.lang.Object)");
  }

  @Test
  public void customCacheResolver() {
    CacheResultOperation operation =
            getCacheOperation(CacheResultOperation.class, CustomService.class, this.cacheName, Long.class);
    assertJCacheResolver(operation.getCacheResolver(), TestableCacheResolver.class);
    assertJCacheResolver(operation.getExceptionCacheResolver(), null);
    assertThat(operation.getKeyGenerator().getClass()).isEqualTo(KeyGeneratorAdapter.class);
    assertThat(((KeyGeneratorAdapter) operation.getKeyGenerator()).getTarget()).isEqualTo(defaultKeyGenerator);
  }

  @Test
  public void customKeyGenerator() {
    CacheResultOperation operation =
            getCacheOperation(CacheResultOperation.class, CustomService.class, this.cacheName, Long.class);
    assertThat(operation.getCacheResolver()).isEqualTo(defaultCacheResolver);
    assertThat(operation.getExceptionCacheResolver()).isNull();
    assertCacheKeyGenerator(operation.getKeyGenerator(), TestableCacheKeyGenerator.class);
  }

  @Test
  public void customKeyGeneratorSpringBean() {
    TestableCacheKeyGenerator bean = new TestableCacheKeyGenerator();
    beanFactory.registerSingleton("fooBar", bean);
    CacheResultOperation operation =
            getCacheOperation(CacheResultOperation.class, CustomService.class, this.cacheName, Long.class);
    assertThat(operation.getCacheResolver()).isEqualTo(defaultCacheResolver);
    assertThat(operation.getExceptionCacheResolver()).isNull();
    KeyGeneratorAdapter adapter = (KeyGeneratorAdapter) operation.getKeyGenerator();
    // take bean from context
    assertThat(adapter.getTarget()).isSameAs(bean);
  }

  @Test
  public void customKeyGeneratorAndCacheResolver() {
    CacheResultOperation operation = getCacheOperation(CacheResultOperation.class,
            CustomServiceWithDefaults.class, this.cacheName, Long.class);
    assertJCacheResolver(operation.getCacheResolver(), TestableCacheResolver.class);
    assertJCacheResolver(operation.getExceptionCacheResolver(), null);
    assertCacheKeyGenerator(operation.getKeyGenerator(), TestableCacheKeyGenerator.class);
  }

  @Test
  public void customKeyGeneratorAndCacheResolverWithExceptionName() {
    CacheResultOperation operation = getCacheOperation(CacheResultOperation.class,
            CustomServiceWithDefaults.class, this.cacheName, Long.class);
    assertJCacheResolver(operation.getCacheResolver(), TestableCacheResolver.class);
    assertJCacheResolver(operation.getExceptionCacheResolver(), TestableCacheResolver.class);
    assertCacheKeyGenerator(operation.getKeyGenerator(), TestableCacheKeyGenerator.class);
  }

  private void assertDefaults(AbstractJCacheKeyOperation<?> operation) {
    assertThat(operation.getCacheResolver()).isEqualTo(defaultCacheResolver);
    assertThat(operation.getKeyGenerator().getClass()).isEqualTo(KeyGeneratorAdapter.class);
    assertThat(((KeyGeneratorAdapter) operation.getKeyGenerator()).getTarget()).isEqualTo(defaultKeyGenerator);
  }

  protected <T extends JCacheOperation<?>> T getDefaultCacheOperation(Class<T> operationType, Class<?>... parameterTypes) {
    return getCacheOperation(operationType, AnnotatedJCacheableService.class, this.cacheName, parameterTypes);
  }

  protected <T extends JCacheOperation<?>> T getCacheOperation(
          Class<T> operationType, Class<?> targetType, String methodName, Class<?>... parameterTypes) {

    JCacheOperation<?> result = getCacheOperation(targetType, methodName, parameterTypes);
    assertThat(result).isNotNull();
    assertThat(result.getClass()).isEqualTo(operationType);
    return operationType.cast(result);
  }

  private JCacheOperation<?> getCacheOperation(Class<?> targetType, String methodName, Class<?>... parameterTypes) {
    Method method = ReflectionUtils.findMethod(targetType, methodName, parameterTypes);
    Assert.notNull(method, "requested method '" + methodName + "'does not exist");
    return source.getCacheOperation(method, targetType);
  }

  private void assertJCacheResolver(CacheResolver actual,
          Class<? extends javax.cache.annotation.CacheResolver> expectedTargetType) {

    if (expectedTargetType == null) {
      assertThat(actual).isNull();
    }
    else {
      assertThat(actual.getClass()).as("Wrong cache resolver implementation").isEqualTo(CacheResolverAdapter.class);
      CacheResolverAdapter adapter = (CacheResolverAdapter) actual;
      assertThat(adapter.getTarget().getClass()).as("Wrong target JCache implementation").isEqualTo(expectedTargetType);
    }
  }

  private void assertCacheKeyGenerator(KeyGenerator actual,
          Class<? extends CacheKeyGenerator> expectedTargetType) {
    assertThat(actual.getClass()).as("Wrong cache resolver implementation").isEqualTo(KeyGeneratorAdapter.class);
    KeyGeneratorAdapter adapter = (KeyGeneratorAdapter) actual;
    assertThat(adapter.getTarget().getClass()).as("Wrong target CacheKeyGenerator implementation").isEqualTo(expectedTargetType);
  }

  static class CustomService {

    @CacheResult(cacheKeyGenerator = TestableCacheKeyGenerator.class)
    public Object customKeyGenerator(Long id) {
      return null;
    }

    @CacheResult(cacheKeyGenerator = TestableCacheKeyGenerator.class)
    public Object customKeyGeneratorSpringBean(Long id) {
      return null;
    }

    @CacheResult(cacheResolverFactory = TestableCacheResolverFactory.class)
    public Object customCacheResolver(Long id) {
      return null;
    }
  }

  @CacheDefaults(cacheResolverFactory = TestableCacheResolverFactory.class, cacheKeyGenerator = TestableCacheKeyGenerator.class)
  static class CustomServiceWithDefaults {

    @CacheResult
    public Object customKeyGeneratorAndCacheResolver(Long id) {
      return null;
    }

    @CacheResult(exceptionCacheName = "exception")
    public Object customKeyGeneratorAndCacheResolverWithExceptionName(Long id) {
      return null;
    }
  }

  static class InvalidCases {

    @CacheRemove
    @CacheRemoveAll
    public void multiAnnotations() {
    }
  }

}

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

package infra.cache.annotation;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import infra.cache.interceptor.CacheEvictOperation;
import infra.cache.interceptor.CacheOperation;
import infra.cache.interceptor.CacheableOperation;
import infra.core.annotation.AliasFor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Costin Leau
 * @author Stephane Nicoll
 * @author Sam Brannen
 */
public class AnnotationCacheOperationSourceTests {

  private final AnnotationCacheOperationSource source = new AnnotationCacheOperationSource();

  @Test
  public void singularAnnotation() {
    Collection<CacheOperation> ops = getOps(AnnotatedClass.class, "singular", 1);
    assertThat(ops.iterator().next() instanceof CacheableOperation).isTrue();
  }

  @Test
  public void multipleAnnotation() {
    Collection<CacheOperation> ops = getOps(AnnotatedClass.class, "multiple", 2);
    Iterator<CacheOperation> it = ops.iterator();
    assertThat(it.next() instanceof CacheableOperation).isTrue();
    assertThat(it.next() instanceof CacheEvictOperation).isTrue();
  }

  @Test
  public void caching() {
    Collection<CacheOperation> ops = getOps(AnnotatedClass.class, "caching", 2);
    Iterator<CacheOperation> it = ops.iterator();
    assertThat(it.next() instanceof CacheableOperation).isTrue();
    assertThat(it.next() instanceof CacheEvictOperation).isTrue();
  }

  @Test
  public void emptyCaching() {
    getOps(AnnotatedClass.class, "emptyCaching", 0);
  }

  @Test
  public void singularStereotype() {
    Collection<CacheOperation> ops = getOps(AnnotatedClass.class, "singleStereotype", 1);
    assertThat(ops.iterator().next() instanceof CacheEvictOperation).isTrue();
  }

  @Test
  public void multipleStereotypes() {
    Collection<CacheOperation> ops = getOps(AnnotatedClass.class, "multipleStereotype", 3);
    Iterator<CacheOperation> it = ops.iterator();
    assertThat(it.next() instanceof CacheableOperation).isTrue();
    CacheOperation next = it.next();
    assertThat(next instanceof CacheEvictOperation).isTrue();
    assertThat(next.getCacheNames().contains("foo")).isTrue();
    next = it.next();
    assertThat(next instanceof CacheEvictOperation).isTrue();
    assertThat(next.getCacheNames().contains("bar")).isTrue();
  }

  @Test
  public void singleComposedAnnotation() {
    Collection<CacheOperation> ops = getOps(AnnotatedClass.class, "singleComposed", 2);
    Iterator<CacheOperation> it = ops.iterator();

    CacheOperation cacheOperation = it.next();
    assertThat(cacheOperation).isInstanceOf(CacheableOperation.class);
    assertThat(cacheOperation.getCacheNames()).isEqualTo(Collections.singleton("directly declared"));
    assertThat(cacheOperation.getKey()).isEqualTo("");

    cacheOperation = it.next();
    assertThat(cacheOperation).isInstanceOf(CacheableOperation.class);
    assertThat(cacheOperation.getCacheNames()).isEqualTo(Collections.singleton("composedCache"));
    assertThat(cacheOperation.getKey()).isEqualTo("composedKey");
  }

  @Test
  public void multipleComposedAnnotations() {
    Collection<CacheOperation> ops = getOps(AnnotatedClass.class, "multipleComposed", 4);
    Iterator<CacheOperation> it = ops.iterator();

    CacheOperation cacheOperation = it.next();
    assertThat(cacheOperation).isInstanceOf(CacheableOperation.class);
    assertThat(cacheOperation.getCacheNames()).isEqualTo(Collections.singleton("directly declared"));
    assertThat(cacheOperation.getKey()).isEqualTo("");

    cacheOperation = it.next();
    assertThat(cacheOperation).isInstanceOf(CacheableOperation.class);
    assertThat(cacheOperation.getCacheNames()).isEqualTo(Collections.singleton("composedCache"));
    assertThat(cacheOperation.getKey()).isEqualTo("composedKey");

    cacheOperation = it.next();
    assertThat(cacheOperation).isInstanceOf(CacheableOperation.class);
    assertThat(cacheOperation.getCacheNames()).isEqualTo(Collections.singleton("foo"));
    assertThat(cacheOperation.getKey()).isEqualTo("");

    cacheOperation = it.next();
    assertThat(cacheOperation).isInstanceOf(CacheEvictOperation.class);
    assertThat(cacheOperation.getCacheNames()).isEqualTo(Collections.singleton("composedCacheEvict"));
    assertThat(cacheOperation.getKey()).isEqualTo("composedEvictionKey");
  }

  @Test
  public void customKeyGenerator() {
    Collection<CacheOperation> ops = getOps(AnnotatedClass.class, "customKeyGenerator", 1);
    CacheOperation cacheOperation = ops.iterator().next();
    assertThat(cacheOperation.getKeyGenerator()).as("Custom key generator not set").isEqualTo("custom");
  }

  @Test
  public void customKeyGeneratorInherited() {
    Collection<CacheOperation> ops = getOps(AnnotatedClass.class, "customKeyGeneratorInherited", 1);
    CacheOperation cacheOperation = ops.iterator().next();
    assertThat(cacheOperation.getKeyGenerator()).as("Custom key generator not set").isEqualTo("custom");
  }

  @Test
  public void keyAndKeyGeneratorCannotBeSetTogether() {
    assertThatIllegalStateException().isThrownBy(() ->
            getOps(AnnotatedClass.class, "invalidKeyAndKeyGeneratorSet"));
  }

  @Test
  public void customCacheManager() {
    Collection<CacheOperation> ops = getOps(AnnotatedClass.class, "customCacheManager", 1);
    CacheOperation cacheOperation = ops.iterator().next();
    assertThat(cacheOperation.getCacheManager()).as("Custom cache manager not set").isEqualTo("custom");
  }

  @Test
  public void customCacheManagerInherited() {
    Collection<CacheOperation> ops = getOps(AnnotatedClass.class, "customCacheManagerInherited", 1);
    CacheOperation cacheOperation = ops.iterator().next();
    assertThat(cacheOperation.getCacheManager()).as("Custom cache manager not set").isEqualTo("custom");
  }

  @Test
  public void customCacheResolver() {
    Collection<CacheOperation> ops = getOps(AnnotatedClass.class, "customCacheResolver", 1);
    CacheOperation cacheOperation = ops.iterator().next();
    assertThat(cacheOperation.getCacheResolver()).as("Custom cache resolver not set").isEqualTo("custom");
  }

  @Test
  public void customCacheResolverInherited() {
    Collection<CacheOperation> ops = getOps(AnnotatedClass.class, "customCacheResolverInherited", 1);
    CacheOperation cacheOperation = ops.iterator().next();
    assertThat(cacheOperation.getCacheResolver()).as("Custom cache resolver not set").isEqualTo("custom");
  }

  @Test
  public void cacheResolverAndCacheManagerCannotBeSetTogether() {
    assertThatIllegalStateException().isThrownBy(() ->
            getOps(AnnotatedClass.class, "invalidCacheResolverAndCacheManagerSet"));
  }

  @Test
  public void fullClassLevelWithCustomCacheName() {
    Collection<CacheOperation> ops = getOps(AnnotatedClassWithFullDefault.class, "methodLevelCacheName", 1);
    CacheOperation cacheOperation = ops.iterator().next();
    assertSharedConfig(cacheOperation, "classKeyGenerator", "", "classCacheResolver", "custom");
  }

  @Test
  public void fullClassLevelWithCustomKeyManager() {
    Collection<CacheOperation> ops = getOps(AnnotatedClassWithFullDefault.class, "methodLevelKeyGenerator", 1);
    CacheOperation cacheOperation = ops.iterator().next();
    assertSharedConfig(cacheOperation, "custom", "", "classCacheResolver", "classCacheName");
  }

  @Test
  public void fullClassLevelWithCustomCacheManager() {
    Collection<CacheOperation> ops = getOps(AnnotatedClassWithFullDefault.class, "methodLevelCacheManager", 1);
    CacheOperation cacheOperation = ops.iterator().next();
    assertSharedConfig(cacheOperation, "classKeyGenerator", "custom", "", "classCacheName");
  }

  @Test
  public void fullClassLevelWithCustomCacheResolver() {
    Collection<CacheOperation> ops = getOps(AnnotatedClassWithFullDefault.class, "methodLevelCacheResolver", 1);
    CacheOperation cacheOperation = ops.iterator().next();
    assertSharedConfig(cacheOperation, "classKeyGenerator", "", "custom", "classCacheName");
  }

  @Test
  public void validateNoCacheIsValid() {
    // Valid as a CacheResolver might return the cache names to use with other info
    Collection<CacheOperation> ops = getOps(AnnotatedClass.class, "noCacheNameSpecified");
    CacheOperation cacheOperation = ops.iterator().next();
    assertThat(cacheOperation.getCacheNames()).as("cache names set is required").isNotNull();
    assertThat(cacheOperation.getCacheNames().size()).as("no cache names specified").isEqualTo(0);
  }

  @Test
  public void customClassLevelWithCustomCacheName() {
    Collection<CacheOperation> ops = getOps(AnnotatedClassWithCustomDefault.class, "methodLevelCacheName", 1);
    CacheOperation cacheOperation = ops.iterator().next();
    assertSharedConfig(cacheOperation, "classKeyGenerator", "", "classCacheResolver", "custom");
  }

  @Test
  public void severalCacheConfigUseClosest() {
    Collection<CacheOperation> ops = getOps(MultipleCacheConfig.class, "multipleCacheConfig");
    CacheOperation cacheOperation = ops.iterator().next();
    assertSharedConfig(cacheOperation, "", "", "", "myCache");
  }

  @Test
  public void cacheConfigFromInterface() {
    Collection<CacheOperation> ops = getOps(InterfaceCacheConfig.class, "interfaceCacheConfig");
    CacheOperation cacheOperation = ops.iterator().next();
    assertSharedConfig(cacheOperation, "", "", "", "myCache");
  }

  @Test
  public void cacheAnnotationOverride() {
    Collection<CacheOperation> ops = getOps(InterfaceCacheConfig.class, "interfaceCacheableOverride");
    assertThat(ops.size()).isSameAs(1);
    CacheOperation cacheOperation = ops.iterator().next();
    assertThat(cacheOperation instanceof CacheableOperation).isTrue();
  }

  @Test
  public void partialClassLevelWithCustomCacheManager() {
    Collection<CacheOperation> ops = getOps(AnnotatedClassWithSomeDefault.class, "methodLevelCacheManager", 1);
    CacheOperation cacheOperation = ops.iterator().next();
    assertSharedConfig(cacheOperation, "classKeyGenerator", "custom", "", "classCacheName");
  }

  @Test
  public void partialClassLevelWithCustomCacheResolver() {
    Collection<CacheOperation> ops = getOps(AnnotatedClassWithSomeDefault.class, "methodLevelCacheResolver", 1);
    CacheOperation cacheOperation = ops.iterator().next();
    assertSharedConfig(cacheOperation, "classKeyGenerator", "", "custom", "classCacheName");
  }

  @Test
  public void partialClassLevelWithNoCustomization() {
    Collection<CacheOperation> ops = getOps(AnnotatedClassWithSomeDefault.class, "noCustomization", 1);
    CacheOperation cacheOperation = ops.iterator().next();
    assertSharedConfig(cacheOperation, "classKeyGenerator", "classCacheManager", "", "classCacheName");
  }

  private Collection<CacheOperation> getOps(Class<?> target, String name, int expectedNumberOfOperations) {
    Collection<CacheOperation> result = getOps(target, name);
    assertThat(result.size()).as("Wrong number of operation(s) for '" + name + "'").isEqualTo(expectedNumberOfOperations);
    return result;
  }

  private Collection<CacheOperation> getOps(Class<?> target, String name) {
    try {
      Method method = target.getMethod(name);
      return this.source.getCacheOperations(method, target);
    }
    catch (NoSuchMethodException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private void assertSharedConfig(CacheOperation actual, String keyGenerator, String cacheManager,
          String cacheResolver, String... cacheNames) {

    assertThat(actual.getKeyGenerator()).as("Wrong key manager").isEqualTo(keyGenerator);
    assertThat(actual.getCacheManager()).as("Wrong cache manager").isEqualTo(cacheManager);
    assertThat(actual.getCacheResolver()).as("Wrong cache resolver").isEqualTo(cacheResolver);
    assertThat(actual.getCacheNames().size()).as("Wrong number of cache names").isEqualTo(cacheNames.length);
    Arrays.stream(cacheNames).forEach(cacheName -> assertThat(actual.getCacheNames().contains(cacheName)).as("Cache '" + cacheName + "' not found in " + actual.getCacheNames()).isTrue());
  }

  private static class AnnotatedClass {

    @Cacheable("test")
    public void singular() {
    }

    @CacheEvict("test")
    @Cacheable("test")
    public void multiple() {
    }

    @Caching(cacheable = @Cacheable("test"), evict = @CacheEvict("test"))
    public void caching() {
    }

    @Caching
    public void emptyCaching() {
    }

    @Cacheable(cacheNames = "test", keyGenerator = "custom")
    public void customKeyGenerator() {
    }

    @Cacheable(cacheNames = "test", cacheManager = "custom")
    public void customCacheManager() {
    }

    @Cacheable(cacheNames = "test", cacheResolver = "custom")
    public void customCacheResolver() {
    }

    @EvictFoo
    public void singleStereotype() {
    }

    @EvictFoo
    @CacheableFoo
    @EvictBar
    public void multipleStereotype() {
    }

    @Cacheable("directly declared")
    @ComposedCacheable(cacheNames = "composedCache", key = "composedKey")
    public void singleComposed() {
    }

    @Cacheable("directly declared")
    @ComposedCacheable(cacheNames = "composedCache", key = "composedKey")
    @CacheableFoo
    @ComposedCacheEvict(cacheNames = "composedCacheEvict", key = "composedEvictionKey")
    public void multipleComposed() {
    }

    @Caching(cacheable = { @Cacheable(cacheNames = "test", key = "a"), @Cacheable(cacheNames = "test", key = "b") })
    public void multipleCaching() {
    }

    @CacheableFooCustomKeyGenerator
    public void customKeyGeneratorInherited() {
    }

    @Cacheable(cacheNames = "test", key = "#root.methodName", keyGenerator = "custom")
    public void invalidKeyAndKeyGeneratorSet() {
    }

    @CacheableFooCustomCacheManager
    public void customCacheManagerInherited() {
    }

    @CacheableFooCustomCacheResolver
    public void customCacheResolverInherited() {
    }

    @Cacheable(cacheNames = "test", cacheManager = "custom", cacheResolver = "custom")
    public void invalidCacheResolverAndCacheManagerSet() {
    }

    @Cacheable // cache name can be inherited from CacheConfig. There's none here
    public void noCacheNameSpecified() {
    }
  }

  @CacheConfig(cacheNames = "classCacheName",
               keyGenerator = "classKeyGenerator",
               cacheManager = "classCacheManager", cacheResolver = "classCacheResolver")
  private static class AnnotatedClassWithFullDefault {

    @Cacheable("custom")
    public void methodLevelCacheName() {
    }

    @Cacheable(keyGenerator = "custom")
    public void methodLevelKeyGenerator() {
    }

    @Cacheable(cacheManager = "custom")
    public void methodLevelCacheManager() {
    }

    @Cacheable(cacheResolver = "custom")
    public void methodLevelCacheResolver() {
    }
  }

  @CacheConfigFoo
  private static class AnnotatedClassWithCustomDefault {

    @Cacheable("custom")
    public void methodLevelCacheName() {
    }
  }

  @CacheConfig(cacheNames = "classCacheName",
               keyGenerator = "classKeyGenerator",
               cacheManager = "classCacheManager")
  private static class AnnotatedClassWithSomeDefault {

    @Cacheable(cacheManager = "custom")
    public void methodLevelCacheManager() {
    }

    @Cacheable(cacheResolver = "custom")
    public void methodLevelCacheResolver() {
    }

    @Cacheable
    public void noCustomization() {
    }
  }

  @CacheConfigFoo
  @CacheConfig(cacheNames = "myCache")  // multiple sources
  private static class MultipleCacheConfig {

    @Cacheable
    public void multipleCacheConfig() {
    }
  }

  @CacheConfig(cacheNames = "myCache")
  private interface CacheConfigIfc {

    @Cacheable
    void interfaceCacheConfig();

    @CachePut
    void interfaceCacheableOverride();
  }

  private static class InterfaceCacheConfig implements CacheConfigIfc {

    @Override
    public void interfaceCacheConfig() {
    }

    @Override
    @Cacheable
    public void interfaceCacheableOverride() {
    }
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @Cacheable("foo")
  public @interface CacheableFoo {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @Cacheable(cacheNames = "foo", keyGenerator = "custom")
  public @interface CacheableFooCustomKeyGenerator {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @Cacheable(cacheNames = "foo", cacheManager = "custom")
  public @interface CacheableFooCustomCacheManager {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @Cacheable(cacheNames = "foo", cacheResolver = "custom")
  public @interface CacheableFooCustomCacheResolver {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @CacheEvict("foo")
  public @interface EvictFoo {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @CacheEvict("bar")
  public @interface EvictBar {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  @CacheConfig(keyGenerator = "classKeyGenerator",
               cacheManager = "classCacheManager",
               cacheResolver = "classCacheResolver")
  public @interface CacheConfigFoo {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.METHOD, ElementType.TYPE })
  @Cacheable(cacheNames = "shadowed cache name", key = "shadowed key")
  @interface ComposedCacheable {

    @AliasFor(annotation = Cacheable.class)
    String[] value() default {};

    @AliasFor(annotation = Cacheable.class)
    String[] cacheNames() default {};

    @AliasFor(annotation = Cacheable.class)
    String key() default "";
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.METHOD, ElementType.TYPE })
  @CacheEvict(cacheNames = "shadowed cache name", key = "shadowed key")
  @interface ComposedCacheEvict {

    @AliasFor(annotation = CacheEvict.class)
    String[] value() default {};

    @AliasFor(annotation = CacheEvict.class)
    String[] cacheNames() default {};

    @AliasFor(annotation = CacheEvict.class)
    String key() default "";
  }

}

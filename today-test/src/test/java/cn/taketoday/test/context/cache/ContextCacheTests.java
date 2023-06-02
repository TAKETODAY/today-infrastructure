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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.test.annotation.DirtiesContext.HierarchyMode;
import cn.taketoday.test.context.ActiveProfiles;
import cn.taketoday.test.context.ActiveProfilesResolver;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.ContextHierarchy;
import cn.taketoday.test.context.MergedContextConfiguration;
import cn.taketoday.test.context.TestContext;
import cn.taketoday.test.context.TestContextTestUtils;
import cn.taketoday.test.context.support.AnnotationConfigContextLoader;
import cn.taketoday.test.util.ReflectionTestUtils;

import static cn.taketoday.test.context.cache.ContextCacheTestUtils.assertContextCacheStatistics;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for verifying proper behavior of the {@link ContextCache} in
 * conjunction with cache keys used in {@link TestContext}.
 *
 * @author Sam Brannen
 * @author Michail Nikolaev
 * @see LruContextCacheTests
 * @see InfraExtensionContextCacheTests
 * @since 4.0
 */
class ContextCacheTests {

  private final ContextCache contextCache = new DefaultContextCache();

  @BeforeEach
  void initialCacheState() {
    assertContextCacheStatistics(contextCache, "initial state", 0, 0, 0);
    assertParentContextCount(0);
  }

  private void assertParentContextCount(int expected) {
    assertThat(contextCache.getParentContextCount()).as("parent context count").isEqualTo(expected);
  }

  private MergedContextConfiguration getMergedContextConfiguration(TestContext testContext) {
    return (MergedContextConfiguration) ReflectionTestUtils.getField(testContext, "mergedContextConfiguration");
  }

  private ApplicationContext loadContext(Class<?> testClass) {
    TestContext testContext = TestContextTestUtils.buildTestContext(testClass, contextCache);
    return testContext.getApplicationContext();
  }

  private void loadCtxAndAssertStats(Class<?> testClass, int expectedSize, int expectedHitCount, int expectedMissCount) {
    assertThat(loadContext(testClass)).isNotNull();
    assertContextCacheStatistics(contextCache, testClass.getName(), expectedSize, expectedHitCount,
            expectedMissCount);
  }

  @Test
  void verifyCacheKeyIsBasedOnContextLoader() {
    loadCtxAndAssertStats(AnnotationConfigContextLoaderTestCase.class, 1, 0, 1);
    loadCtxAndAssertStats(AnnotationConfigContextLoaderTestCase.class, 1, 1, 1);
    loadCtxAndAssertStats(CustomAnnotationConfigContextLoaderTestCase.class, 2, 1, 2);
    loadCtxAndAssertStats(CustomAnnotationConfigContextLoaderTestCase.class, 2, 2, 2);
    loadCtxAndAssertStats(AnnotationConfigContextLoaderTestCase.class, 2, 3, 2);
    loadCtxAndAssertStats(CustomAnnotationConfigContextLoaderTestCase.class, 2, 4, 2);
  }

  @Test
  void verifyCacheKeyIsBasedOnActiveProfiles() {
    int size = 0, hit = 0, miss = 0;
    loadCtxAndAssertStats(FooBarProfilesTestCase.class, ++size, hit, ++miss);
    loadCtxAndAssertStats(FooBarProfilesTestCase.class, size, ++hit, miss);
    // Profiles {foo, bar} should not hash to the same as {bar,foo}
    loadCtxAndAssertStats(BarFooProfilesTestCase.class, ++size, hit, ++miss);
    loadCtxAndAssertStats(FooBarProfilesTestCase.class, size, ++hit, miss);
    loadCtxAndAssertStats(FooBarProfilesTestCase.class, size, ++hit, miss);
    loadCtxAndAssertStats(BarFooProfilesTestCase.class, size, ++hit, miss);
    loadCtxAndAssertStats(FooBarActiveProfilesResolverTestCase.class, size, ++hit, miss);
  }

  @Test
  void verifyCacheBehaviorForContextHierarchies() {
    int size = 0;
    int hits = 0;
    int misses = 0;

    // Level 1
    loadCtxAndAssertStats(ClassHierarchyContextHierarchyLevel1TestCase.class, ++size, hits, ++misses);
    loadCtxAndAssertStats(ClassHierarchyContextHierarchyLevel1TestCase.class, size, ++hits, misses);

    // Level 2
    loadCtxAndAssertStats(ClassHierarchyContextHierarchyLevel2TestCase.class, ++size /* L2 */, ++hits /* L1 */,
            ++misses /* L2 */);
    loadCtxAndAssertStats(ClassHierarchyContextHierarchyLevel2TestCase.class, size, ++hits /* L2 */, misses);
    loadCtxAndAssertStats(ClassHierarchyContextHierarchyLevel2TestCase.class, size, ++hits /* L2 */, misses);

    // Level 3-A
    loadCtxAndAssertStats(ClassHierarchyContextHierarchyLevel3aTestCase.class, ++size /* L3A */, ++hits /* L2 */,
            ++misses /* L3A */);
    loadCtxAndAssertStats(ClassHierarchyContextHierarchyLevel3aTestCase.class, size, ++hits /* L3A */, misses);

    // Level 3-B
    loadCtxAndAssertStats(ClassHierarchyContextHierarchyLevel3bTestCase.class, ++size /* L3B */, ++hits /* L2 */,
            ++misses /* L3B */);
    loadCtxAndAssertStats(ClassHierarchyContextHierarchyLevel3bTestCase.class, size, ++hits /* L3B */, misses);
  }

  @Test
  void removeContextHierarchyCacheLevel1() {

    // Load Level 3-A
    TestContext testContext3a = TestContextTestUtils.buildTestContext(
            ClassHierarchyContextHierarchyLevel3aTestCase.class, contextCache);
    testContext3a.getApplicationContext();
    assertContextCacheStatistics(contextCache, "level 3, A", 3, 0, 3);
    assertParentContextCount(2);

    // Load Level 3-B
    TestContext testContext3b = TestContextTestUtils.buildTestContext(
            ClassHierarchyContextHierarchyLevel3bTestCase.class, contextCache);
    testContext3b.getApplicationContext();
    assertContextCacheStatistics(contextCache, "level 3, A and B", 4, 1, 4);
    assertParentContextCount(2);

    // Remove Level 1
    // Should also remove Levels 2, 3-A, and 3-B, leaving nothing.
    contextCache.remove(getMergedContextConfiguration(testContext3a).getParent().getParent(),
            HierarchyMode.CURRENT_LEVEL);
    assertContextCacheStatistics(contextCache, "removed level 1", 0, 1, 4);
    assertParentContextCount(0);
  }

  @Test
  void removeContextHierarchyCacheLevel1WithExhaustiveMode() {

    // Load Level 3-A
    TestContext testContext3a = TestContextTestUtils.buildTestContext(
            ClassHierarchyContextHierarchyLevel3aTestCase.class, contextCache);
    testContext3a.getApplicationContext();
    assertContextCacheStatistics(contextCache, "level 3, A", 3, 0, 3);
    assertParentContextCount(2);

    // Load Level 3-B
    TestContext testContext3b = TestContextTestUtils.buildTestContext(
            ClassHierarchyContextHierarchyLevel3bTestCase.class, contextCache);
    testContext3b.getApplicationContext();
    assertContextCacheStatistics(contextCache, "level 3, A and B", 4, 1, 4);
    assertParentContextCount(2);

    // Remove Level 1
    // Should also remove Levels 2, 3-A, and 3-B, leaving nothing.
    contextCache.remove(getMergedContextConfiguration(testContext3a).getParent().getParent(),
            HierarchyMode.EXHAUSTIVE);
    assertContextCacheStatistics(contextCache, "removed level 1", 0, 1, 4);
    assertParentContextCount(0);
  }

  @Test
  void removeContextHierarchyCacheLevel2() {

    // Load Level 3-A
    TestContext testContext3a = TestContextTestUtils.buildTestContext(
            ClassHierarchyContextHierarchyLevel3aTestCase.class, contextCache);
    testContext3a.getApplicationContext();
    assertContextCacheStatistics(contextCache, "level 3, A", 3, 0, 3);
    assertParentContextCount(2);

    // Load Level 3-B
    TestContext testContext3b = TestContextTestUtils.buildTestContext(
            ClassHierarchyContextHierarchyLevel3bTestCase.class, contextCache);
    testContext3b.getApplicationContext();
    assertContextCacheStatistics(contextCache, "level 3, A and B", 4, 1, 4);
    assertParentContextCount(2);

    // Remove Level 2
    // Should also remove Levels 3-A and 3-B, leaving only Level 1 as a context in the
    // cache but also removing the Level 1 hierarchy since all children have been
    // removed.
    contextCache.remove(getMergedContextConfiguration(testContext3a).getParent(), HierarchyMode.CURRENT_LEVEL);
    assertContextCacheStatistics(contextCache, "removed level 2", 1, 1, 4);
    assertParentContextCount(0);
  }

  @Test
  void removeContextHierarchyCacheLevel2WithExhaustiveMode() {

    // Load Level 3-A
    TestContext testContext3a = TestContextTestUtils.buildTestContext(
            ClassHierarchyContextHierarchyLevel3aTestCase.class, contextCache);
    testContext3a.getApplicationContext();
    assertContextCacheStatistics(contextCache, "level 3, A", 3, 0, 3);
    assertParentContextCount(2);

    // Load Level 3-B
    TestContext testContext3b = TestContextTestUtils.buildTestContext(
            ClassHierarchyContextHierarchyLevel3bTestCase.class, contextCache);
    testContext3b.getApplicationContext();
    assertContextCacheStatistics(contextCache, "level 3, A and B", 4, 1, 4);
    assertParentContextCount(2);

    // Remove Level 2
    // Should wipe the cache
    contextCache.remove(getMergedContextConfiguration(testContext3a).getParent(), HierarchyMode.EXHAUSTIVE);
    assertContextCacheStatistics(contextCache, "removed level 2", 0, 1, 4);
    assertParentContextCount(0);
  }

  @Test
  void removeContextHierarchyCacheLevel3Then2() {

    // Load Level 3-A
    TestContext testContext3a = TestContextTestUtils.buildTestContext(
            ClassHierarchyContextHierarchyLevel3aTestCase.class, contextCache);
    testContext3a.getApplicationContext();
    assertContextCacheStatistics(contextCache, "level 3, A", 3, 0, 3);
    assertParentContextCount(2);

    // Load Level 3-B
    TestContext testContext3b = TestContextTestUtils.buildTestContext(
            ClassHierarchyContextHierarchyLevel3bTestCase.class, contextCache);
    testContext3b.getApplicationContext();
    assertContextCacheStatistics(contextCache, "level 3, A and B", 4, 1, 4);
    assertParentContextCount(2);

    // Remove Level 3-A
    contextCache.remove(getMergedContextConfiguration(testContext3a), HierarchyMode.CURRENT_LEVEL);
    assertContextCacheStatistics(contextCache, "removed level 3-A", 3, 1, 4);
    assertParentContextCount(2);

    // Remove Level 2
    // Should also remove Level 3-B, leaving only Level 1.
    contextCache.remove(getMergedContextConfiguration(testContext3b).getParent(), HierarchyMode.CURRENT_LEVEL);
    assertContextCacheStatistics(contextCache, "removed level 2", 1, 1, 4);
    assertParentContextCount(0);
  }

  @Test
  void removeContextHierarchyCacheLevel3Then2WithExhaustiveMode() {

    // Load Level 3-A
    TestContext testContext3a = TestContextTestUtils.buildTestContext(
            ClassHierarchyContextHierarchyLevel3aTestCase.class, contextCache);
    testContext3a.getApplicationContext();
    assertContextCacheStatistics(contextCache, "level 3, A", 3, 0, 3);
    assertParentContextCount(2);

    // Load Level 3-B
    TestContext testContext3b = TestContextTestUtils.buildTestContext(
            ClassHierarchyContextHierarchyLevel3bTestCase.class, contextCache);
    testContext3b.getApplicationContext();
    assertContextCacheStatistics(contextCache, "level 3, A and B", 4, 1, 4);
    assertParentContextCount(2);

    // Remove Level 3-A
    // Should wipe the cache.
    contextCache.remove(getMergedContextConfiguration(testContext3a), HierarchyMode.EXHAUSTIVE);
    assertContextCacheStatistics(contextCache, "removed level 3-A", 0, 1, 4);
    assertParentContextCount(0);

    // Remove Level 2
    // Should not actually do anything since the cache was cleared in the
    // previous step. So the stats should remain the same.
    contextCache.remove(getMergedContextConfiguration(testContext3b).getParent(), HierarchyMode.EXHAUSTIVE);
    assertContextCacheStatistics(contextCache, "removed level 2", 0, 1, 4);
    assertParentContextCount(0);
  }

  @Configuration
  static class Config {
  }

  @ContextConfiguration(classes = Config.class, loader = AnnotationConfigContextLoader.class)
  private static class AnnotationConfigContextLoaderTestCase {
  }

  @ContextConfiguration(classes = Config.class, loader = CustomAnnotationConfigContextLoader.class)
  private static class CustomAnnotationConfigContextLoaderTestCase {
  }

  private static class CustomAnnotationConfigContextLoader extends AnnotationConfigContextLoader {
  }

  @ActiveProfiles({ "foo", "bar" })
  @ContextConfiguration(classes = Config.class, loader = AnnotationConfigContextLoader.class)
  private static class FooBarProfilesTestCase {
  }

  @ActiveProfiles({ "bar", "foo" })
  @ContextConfiguration(classes = Config.class, loader = AnnotationConfigContextLoader.class)
  private static class BarFooProfilesTestCase {
  }

  private static class FooBarActiveProfilesResolver implements ActiveProfilesResolver {

    @Override
    public String[] resolve(Class<?> testClass) {
      return new String[] { "foo", "bar" };
    }
  }

  @ActiveProfiles(resolver = FooBarActiveProfilesResolver.class)
  @ContextConfiguration(classes = Config.class, loader = AnnotationConfigContextLoader.class)
  private static class FooBarActiveProfilesResolverTestCase {
  }

  @ContextHierarchy({ @ContextConfiguration })
  private static class ClassHierarchyContextHierarchyLevel1TestCase {

    @Configuration
    static class Level1Config {

    }
  }

  @ContextHierarchy({ @ContextConfiguration })
  private static class ClassHierarchyContextHierarchyLevel2TestCase extends
          ClassHierarchyContextHierarchyLevel1TestCase {

    @Configuration
    static class Level2Config {

    }
  }

  @ContextHierarchy({ @ContextConfiguration })
  private static class ClassHierarchyContextHierarchyLevel3aTestCase extends
          ClassHierarchyContextHierarchyLevel2TestCase {

    @Configuration
    static class Level3aConfig {

    }
  }

  @ContextHierarchy({ @ContextConfiguration })
  private static class ClassHierarchyContextHierarchyLevel3bTestCase extends
          ClassHierarchyContextHierarchyLevel2TestCase {

    @Configuration
    static class Level3bConfig {

    }
  }

}

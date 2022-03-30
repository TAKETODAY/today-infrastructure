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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.test.annotation.DirtiesContext;
import cn.taketoday.test.annotation.DirtiesContext.ClassMode;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.TestExecutionListeners;
import cn.taketoday.test.context.support.DependencyInjectionTestExecutionListener;
import cn.taketoday.test.context.support.DirtiesContextTestExecutionListener;
import cn.taketoday.test.context.testng.AbstractTestNGSpringContextTests;
import cn.taketoday.test.context.testng.TrackingTestNGTestListener;

import org.testng.TestNG;

import java.util.concurrent.atomic.AtomicInteger;

import static cn.taketoday.test.context.cache.ContextCacheTestUtils.assertContextCacheStatistics;
import static cn.taketoday.test.context.cache.ContextCacheTestUtils.resetContextCache;

/**
 * JUnit based integration test which verifies correct {@linkplain ContextCache
 * application context caching} in conjunction with Spring's TestNG support
 * and {@link DirtiesContext @DirtiesContext} at the class level.
 *
 * <p>This class is a direct copy of {@link ClassLevelDirtiesContextTests},
 * modified to verify behavior in conjunction with TestNG.
 *
 * @author Sam Brannen
 * @since 4.2
 */
class ClassLevelDirtiesContextTestNGTests {

	private static final AtomicInteger cacheHits = new AtomicInteger();
	private static final AtomicInteger cacheMisses = new AtomicInteger();


	@BeforeAll
	static void verifyInitialCacheState() {
		resetContextCache();
		// Reset static counters in case tests are run multiple times in a test suite --
		// for example, via JUnit's @Suite.
		cacheHits.set(0);
		cacheMisses.set(0);
		assertContextCacheStatistics("BeforeClass", 0, cacheHits.get(), cacheMisses.get());
	}

	@Test
	void verifyDirtiesContextBehavior() throws Exception {

		assertBehaviorForCleanTestCase();

		runTestClassAndAssertStats(ClassLevelDirtiesContextWithCleanMethodsAndDefaultModeTestCase.class, 1);
		assertContextCacheStatistics("after class-level @DirtiesContext with clean test method and default class mode",
			0, cacheHits.incrementAndGet(), cacheMisses.get());
		assertBehaviorForCleanTestCase();

		runTestClassAndAssertStats(InheritedClassLevelDirtiesContextWithCleanMethodsAndDefaultModeTestCase.class, 1);
		assertContextCacheStatistics(
			"after inherited class-level @DirtiesContext with clean test method and default class mode", 0,
			cacheHits.incrementAndGet(), cacheMisses.get());
		assertBehaviorForCleanTestCase();

		runTestClassAndAssertStats(ClassLevelDirtiesContextWithCleanMethodsAndAfterClassModeTestCase.class, 1);
		assertContextCacheStatistics("after class-level @DirtiesContext with clean test method and AFTER_CLASS mode",
			0, cacheHits.incrementAndGet(), cacheMisses.get());
		assertBehaviorForCleanTestCase();

		runTestClassAndAssertStats(InheritedClassLevelDirtiesContextWithCleanMethodsAndAfterClassModeTestCase.class, 1);
		assertContextCacheStatistics(
			"after inherited class-level @DirtiesContext with clean test method and AFTER_CLASS mode", 0,
			cacheHits.incrementAndGet(), cacheMisses.get());
		assertBehaviorForCleanTestCase();

		runTestClassAndAssertStats(ClassLevelDirtiesContextWithAfterEachTestMethodModeTestCase.class, 3);
		assertContextCacheStatistics(
			"after class-level @DirtiesContext with clean test method and AFTER_EACH_TEST_METHOD mode", 0,
			cacheHits.incrementAndGet(), cacheMisses.addAndGet(2));
		assertBehaviorForCleanTestCase();

		runTestClassAndAssertStats(InheritedClassLevelDirtiesContextWithAfterEachTestMethodModeTestCase.class, 3);
		assertContextCacheStatistics(
			"after inherited class-level @DirtiesContext with clean test method and AFTER_EACH_TEST_METHOD mode", 0,
			cacheHits.incrementAndGet(), cacheMisses.addAndGet(2));
		assertBehaviorForCleanTestCase();

		runTestClassAndAssertStats(ClassLevelDirtiesContextWithDirtyMethodsTestCase.class, 1);
		assertContextCacheStatistics("after class-level @DirtiesContext with dirty test method", 0,
			cacheHits.incrementAndGet(), cacheMisses.get());
		runTestClassAndAssertStats(ClassLevelDirtiesContextWithDirtyMethodsTestCase.class, 1);
		assertContextCacheStatistics("after class-level @DirtiesContext with dirty test method", 0, cacheHits.get(),
			cacheMisses.incrementAndGet());
		runTestClassAndAssertStats(ClassLevelDirtiesContextWithDirtyMethodsTestCase.class, 1);
		assertContextCacheStatistics("after class-level @DirtiesContext with dirty test method", 0, cacheHits.get(),
			cacheMisses.incrementAndGet());
		assertBehaviorForCleanTestCase();

		runTestClassAndAssertStats(InheritedClassLevelDirtiesContextWithDirtyMethodsTestCase.class, 1);
		assertContextCacheStatistics("after inherited class-level @DirtiesContext with dirty test method", 0,
			cacheHits.incrementAndGet(), cacheMisses.get());
		runTestClassAndAssertStats(InheritedClassLevelDirtiesContextWithDirtyMethodsTestCase.class, 1);
		assertContextCacheStatistics("after inherited class-level @DirtiesContext with dirty test method", 0,
			cacheHits.get(), cacheMisses.incrementAndGet());
		runTestClassAndAssertStats(InheritedClassLevelDirtiesContextWithDirtyMethodsTestCase.class, 1);
		assertContextCacheStatistics("after inherited class-level @DirtiesContext with dirty test method", 0,
			cacheHits.get(), cacheMisses.incrementAndGet());
		assertBehaviorForCleanTestCase();

		runTestClassAndAssertStats(ClassLevelDirtiesContextWithCleanMethodsAndAfterClassModeTestCase.class, 1);
		assertContextCacheStatistics("after class-level @DirtiesContext with clean test method and AFTER_CLASS mode",
			0, cacheHits.incrementAndGet(), cacheMisses.get());
	}

	private void runTestClassAndAssertStats(Class<?> testClass, int expectedTestCount) {
		final int expectedTestFailureCount = 0;
		final int expectedTestStartedCount = expectedTestCount;
		final int expectedTestFinishedCount = expectedTestCount;

		final TrackingTestNGTestListener listener = new TrackingTestNGTestListener();
		final TestNG testNG = new TestNG();
		testNG.addListener(listener);
		testNG.setTestClasses(new Class<?>[] { testClass });
		testNG.setVerbose(0);
		testNG.run();

		assertThat(listener.testFailureCount).as("Failures for test class [" + testClass + "].").isEqualTo(expectedTestFailureCount);
		assertThat(listener.testStartCount).as("Tests started for test class [" + testClass + "].").isEqualTo(expectedTestStartedCount);
		assertThat(listener.testSuccessCount).as("Successful tests for test class [" + testClass + "].").isEqualTo(expectedTestFinishedCount);
	}

	private void assertBehaviorForCleanTestCase() {
		runTestClassAndAssertStats(CleanTestCase.class, 1);
		assertContextCacheStatistics("after clean test class", 1, cacheHits.get(), cacheMisses.incrementAndGet());
	}

	@AfterAll
	static void verifyFinalCacheState() {
		assertContextCacheStatistics("AfterClass", 0, cacheHits.get(), cacheMisses.get());
	}


	// -------------------------------------------------------------------

	@TestExecutionListeners(listeners = { DependencyInjectionTestExecutionListener.class,
		DirtiesContextTestExecutionListener.class }, inheritListeners = false)
	@ContextConfiguration
	static abstract class BaseTestCase extends AbstractTestNGSpringContextTests {

		@Configuration
		static class Config {
			/* no beans */
		}


		@Autowired
		protected ApplicationContext applicationContext;


		protected void assertApplicationContextWasAutowired() {
			assertThat(this.applicationContext).as("The application context should have been autowired.").isNotNull();
		}
	}

	static final class CleanTestCase extends BaseTestCase {

		@org.testng.annotations.Test
		void verifyContextWasAutowired() {
			assertApplicationContextWasAutowired();
		}

	}

	@DirtiesContext
	static class ClassLevelDirtiesContextWithCleanMethodsAndDefaultModeTestCase extends BaseTestCase {

		@org.testng.annotations.Test
		void verifyContextWasAutowired() {
			assertApplicationContextWasAutowired();
		}
	}

	static class InheritedClassLevelDirtiesContextWithCleanMethodsAndDefaultModeTestCase extends
			ClassLevelDirtiesContextWithCleanMethodsAndDefaultModeTestCase {
	}

	@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
	static class ClassLevelDirtiesContextWithCleanMethodsAndAfterClassModeTestCase extends BaseTestCase {

		@org.testng.annotations.Test
		void verifyContextWasAutowired() {
			assertApplicationContextWasAutowired();
		}
	}

	static class InheritedClassLevelDirtiesContextWithCleanMethodsAndAfterClassModeTestCase extends
			ClassLevelDirtiesContextWithCleanMethodsAndAfterClassModeTestCase {
	}

	@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
	static class ClassLevelDirtiesContextWithAfterEachTestMethodModeTestCase extends BaseTestCase {

		@org.testng.annotations.Test
		void verifyContextWasAutowired1() {
			assertApplicationContextWasAutowired();
		}

		@org.testng.annotations.Test
		void verifyContextWasAutowired2() {
			assertApplicationContextWasAutowired();
		}

		@org.testng.annotations.Test
		void verifyContextWasAutowired3() {
			assertApplicationContextWasAutowired();
		}
	}

	static class InheritedClassLevelDirtiesContextWithAfterEachTestMethodModeTestCase extends
			ClassLevelDirtiesContextWithAfterEachTestMethodModeTestCase {
	}

	@DirtiesContext
	static class ClassLevelDirtiesContextWithDirtyMethodsTestCase extends BaseTestCase {

		@org.testng.annotations.Test
		@DirtiesContext
		void dirtyContext() {
			assertApplicationContextWasAutowired();
		}
	}

	static class InheritedClassLevelDirtiesContextWithDirtyMethodsTestCase extends
			ClassLevelDirtiesContextWithDirtyMethodsTestCase {
	}

}

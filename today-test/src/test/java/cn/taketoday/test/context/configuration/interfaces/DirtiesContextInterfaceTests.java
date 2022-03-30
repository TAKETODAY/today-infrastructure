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

package cn.taketoday.test.context.configuration.interfaces;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.test.context.TestExecutionListeners;
import cn.taketoday.test.context.junit4.JUnitTestingUtils;
import cn.taketoday.test.context.junit4.ApplicationRunner;
import cn.taketoday.test.context.support.DependencyInjectionTestExecutionListener;
import cn.taketoday.test.context.support.DirtiesContextBeforeModesTestExecutionListener;
import cn.taketoday.test.context.support.DirtiesContextTestExecutionListener;

import java.util.concurrent.atomic.AtomicInteger;

import static cn.taketoday.test.context.cache.ContextCacheTestUtils.assertContextCacheStatistics;
import static cn.taketoday.test.context.cache.ContextCacheTestUtils.resetContextCache;
import static cn.taketoday.test.context.junit4.JUnitTestingUtils.runTestsAndAssertCounters;

/**
 * @author Sam Brannen
 * @since 4.3
 */
class DirtiesContextInterfaceTests {

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

	@AfterAll
	static void verifyFinalCacheState() {
		assertContextCacheStatistics("AfterClass", 0, cacheHits.get(), cacheMisses.get());
	}

	@Test
	void verifyDirtiesContextBehavior() throws Exception {
		runTestClassAndAssertStats(ClassLevelDirtiesContextWithCleanMethodsAndDefaultModeTestCase.class, 1);
		assertContextCacheStatistics("after class-level @DirtiesContext with clean test method and default class mode",
			0, cacheHits.get(), cacheMisses.incrementAndGet());
	}

	private void runTestClassAndAssertStats(Class<?> testClass, int expectedTestCount) throws Exception {
		JUnitTestingUtils.runTestsAndAssertCounters(testClass, expectedTestCount, 0, expectedTestCount, 0, 0);
	}


	@RunWith(ApplicationRunner.class)
	// Ensure that we do not include the EventPublishingTestExecutionListener
	// since it will access the ApplicationContext for each method in the
	// TestExecutionListener API, thus distorting our cache hit/miss results.
	@TestExecutionListeners({
		DirtiesContextBeforeModesTestExecutionListener.class,
		DependencyInjectionTestExecutionListener.class,
		DirtiesContextTestExecutionListener.class
	})
	public static class ClassLevelDirtiesContextWithCleanMethodsAndDefaultModeTestCase
			implements DirtiesContextTestInterface {

		@Autowired
		ApplicationContext applicationContext;


		@org.junit.Test
		public void verifyContextWasAutowired() {
			assertThat(this.applicationContext).as("The application context should have been autowired.").isNotNull();
		}


		@Configuration
		static class Config {
			/* no beans */
		}

	}

}

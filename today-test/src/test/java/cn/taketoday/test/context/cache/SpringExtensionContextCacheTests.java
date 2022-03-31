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
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.test.annotation.DirtiesContext;
import cn.taketoday.test.context.TestExecutionListeners;
import cn.taketoday.test.context.junit.jupiter.ApplicationExtension;
import cn.taketoday.test.context.junit.jupiter.JUnitConfig;
import cn.taketoday.test.context.support.DependencyInjectionTestExecutionListener;
import cn.taketoday.test.context.support.DirtiesContextTestExecutionListener;

import static cn.taketoday.test.context.cache.ContextCacheTestUtils.assertContextCacheStatistics;
import static cn.taketoday.test.context.cache.ContextCacheTestUtils.resetContextCache;

/**
 * Unit tests which verify correct {@link ContextCache
 * application context caching} in conjunction with the
 * {@link ApplicationExtension} and the {@link DirtiesContext
 * &#064;DirtiesContext} annotation at the method level.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 2.5
 * @see ContextCacheTests
 * @see LruContextCacheTests
 */
@JUnitConfig(locations = "../junit4/SpringJUnit4ClassRunnerAppCtxTests-context.xml")
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class, DirtiesContextTestExecutionListener.class })
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SpringExtensionContextCacheTests {

	private static ApplicationContext dirtiedApplicationContext;

	@Autowired
	ApplicationContext applicationContext;

	@BeforeAll
	static void verifyInitialCacheState() {
		dirtiedApplicationContext = null;
		resetContextCache();
		assertContextCacheStatistics("BeforeClass", 0, 0, 0);
	}

	@AfterAll
	static void verifyFinalCacheState() {
		assertContextCacheStatistics("AfterClass", 1, 1, 2);
	}

	@Test
	@DirtiesContext
	@Order(1)
	void dirtyContext() {
		assertContextCacheStatistics("dirtyContext()", 1, 0, 1);
		assertThat(this.applicationContext).as("The application context should have been autowired.").isNotNull();
		SpringExtensionContextCacheTests.dirtiedApplicationContext = this.applicationContext;
	}

	@Test
	@Order(2)
	void verifyContextDirty() {
		assertContextCacheStatistics("verifyContextWasDirtied()", 1, 0, 2);
		assertThat(this.applicationContext).as("The application context should have been autowired.").isNotNull();
		assertThat(this.applicationContext).as("The application context should have been 'dirtied'.").isNotSameAs(SpringExtensionContextCacheTests.dirtiedApplicationContext);
		SpringExtensionContextCacheTests.dirtiedApplicationContext = this.applicationContext;
	}

	@Test
	@Order(3)
	void verifyContextNotDirty() {
		assertContextCacheStatistics("verifyContextWasNotDirtied()", 1, 1, 2);
		assertThat(this.applicationContext).as("The application context should have been autowired.").isNotNull();
		assertThat(this.applicationContext).as("The application context should NOT have been 'dirtied'.").isSameAs(SpringExtensionContextCacheTests.dirtiedApplicationContext);
	}

}

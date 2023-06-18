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

package cn.taketoday.core.test.io.support;

import java.util.List;

import org.junit.jupiter.api.Test;

import cn.taketoday.core.annotation.Order;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MockTodayStrategies}.
 *
 * @author Phillip Webb
 */
class MockTodayStrategiesTests {

	@Test
	void addWithClassesAddsFactories() {
		MockTodayStrategies loader = new MockTodayStrategies();
		loader.add(TestFactoryType.class, TestFactoryOne.class, TestFactoryTwo.class);
		assertThatLoaderHasTestFactories(loader);
	}

	@Test
	void addWithClassNamesAddsFactories() {
		MockTodayStrategies loader = new MockTodayStrategies();
		loader.add(TestFactoryType.class.getName(), TestFactoryOne.class.getName(), TestFactoryTwo.class.getName());
		assertThatLoaderHasTestFactories(loader);
	}

	@Test
	void addWithClassAndInstancesAddsFactories() {
		MockTodayStrategies loader = new MockTodayStrategies();
		loader.addInstance(TestFactoryType.class, new TestFactoryOne(), new TestFactoryTwo());
		assertThatLoaderHasTestFactories(loader);
	}

	@Test
	void addWithClassNameAndInstancesAddsFactories() {
		MockTodayStrategies loader = new MockTodayStrategies();
		loader.addInstance(TestFactoryType.class.getName(), new TestFactoryOne(), new TestFactoryTwo());
		assertThatLoaderHasTestFactories(loader);
	}

	private void assertThatLoaderHasTestFactories(MockTodayStrategies loader) {
		List<TestFactoryType> factories = loader.load(TestFactoryType.class);
		assertThat(factories).hasSize(2);
		assertThat(factories.get(0)).isInstanceOf(TestFactoryOne.class);
		assertThat(factories.get(1)).isInstanceOf(TestFactoryTwo.class);
	}

	interface TestFactoryType {

	}

	@Order(1)
	static class TestFactoryOne implements TestFactoryType {

	}

	@Order(2)
	static class TestFactoryTwo implements TestFactoryType {

	}

}

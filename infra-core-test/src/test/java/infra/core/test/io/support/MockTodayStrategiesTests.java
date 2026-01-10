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

package infra.core.test.io.support;

import java.util.List;

import org.junit.jupiter.api.Test;

import infra.core.annotation.Order;

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

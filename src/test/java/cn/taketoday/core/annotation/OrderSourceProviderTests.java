/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.core.annotation;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.taketoday.core.Ordered;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 */
class OrderSourceProviderTests {

	private final AnnotationAwareOrderComparator comparator = AnnotationAwareOrderComparator.INSTANCE;


	@Test
	void plainComparator() {
		List<Object> items = new ArrayList<>();
		C c = new C(5);
		C c2 = new C(-5);
		items.add(c);
		items.add(c2);
		items.sort(comparator);
		assertOrder(items, c2, c);
	}

	@Test
	void listNoFactoryMethod() {
		A a = new A();
		C c = new C(-50);
		B b = new B();

		List<?> items = Arrays.asList(a, c, b);
		items.sort(comparator.withSourceProvider(obj -> null));
		assertOrder(items, c, a, b);
	}

	@Test
	void listFactoryMethod() {
		A a = new A();
		C c = new C(3);
		B b = new B();

		List<?> items = Arrays.asList(a, c, b);
		items.sort(comparator.withSourceProvider(obj -> {
			if (obj == a) {
				return new C(4);
			}
			if (obj == b) {
				return new C(2);
			}
			return null;
		}));
		assertOrder(items, b, c, a);
	}

	@Test
	void listFactoryMethodOverridesStaticOrder() {
		A a = new A();
		C c = new C(5);
		C c2 = new C(-5);

		List<?> items = Arrays.asList(a, c, c2);
		items.sort(comparator.withSourceProvider(obj -> {
			if (obj == a) {
				return 4;
			}
			if (obj == c2) {
				return 2;
			}
			return null;
		}));
		assertOrder(items, c2, a, c);
	}

	@Test
	void arrayNoFactoryMethod() {
		A a = new A();
		C c = new C(-50);
		B b = new B();

		Object[] items = new Object[] {a, c, b};
		Arrays.sort(items, comparator.withSourceProvider(obj -> null));
		assertOrder(items, c, a, b);
	}

	@Test
	void arrayFactoryMethod() {
		A a = new A();
		C c = new C(3);
		B b = new B();

		Object[] items = new Object[] {a, c, b};
		Arrays.sort(items, comparator.withSourceProvider(obj -> {
			if (obj == a) {
				return new C(4);
			}
			if (obj == b) {
				return new C(2);
			}
			return null;
		}));
		assertOrder(items, b, c, a);
	}

	@Test
	void arrayFactoryMethodOverridesStaticOrder() {
		A a = new A();
		C c = new C(5);
		C c2 = new C(-5);

		Object[] items = new Object[] {a, c, c2};
		Arrays.sort(items, comparator.withSourceProvider(obj -> {
			if (obj == a) {
				return 4;
			}
			if (obj == c2) {
				return 2;
			}
			return null;
		}));
		assertOrder(items, c2, a, c);
	}


	private void assertOrder(List<?> actual, Object... expected) {
		for (int i = 0; i < actual.size(); i++) {
			assertThat(actual.get(i)).as("Wrong instance at index '" + i + "'").isSameAs(expected[i]);
		}
		assertThat(actual.size()).as("Wrong number of items").isEqualTo(expected.length);
	}

	private void assertOrder(Object[] actual, Object... expected) {
		for (int i = 0; i < actual.length; i++) {
			assertThat(actual[i]).as("Wrong instance at index '" + i + "'").isSameAs(expected[i]);
		}
		assertThat(expected.length).as("Wrong number of items").isEqualTo(expected.length);
	}


	@Order(1)
	private static class A {
	}


	@Order(2)
	private static class B {
	}


	private static class C implements Ordered {

		private final int order;

		private C(int order) {
			this.order = order;
		}

		@Override
		public int getOrder() {
			return order;
		}
	}

}

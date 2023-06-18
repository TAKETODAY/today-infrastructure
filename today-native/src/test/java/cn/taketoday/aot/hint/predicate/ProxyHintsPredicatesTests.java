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

package cn.taketoday.aot.hint.predicate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.Predicate;

import cn.taketoday.aot.hint.RuntimeHints;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link ProxyHintsPredicates}.
 *
 * @author Brian Clozel
 */
class ProxyHintsPredicatesTests {

	private final ProxyHintsPredicates proxy = new ProxyHintsPredicates();

	private RuntimeHints runtimeHints;

	@BeforeEach
	void setup() {
		this.runtimeHints = new RuntimeHints();
	}

	@Test
	void shouldFailForEmptyInterfacesArray() {
		assertThatThrownBy(() -> this.proxy.forInterfaces(new Class<?>[] {})).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void proxyForInterfacesMatchesProxyHint() {
		this.runtimeHints.proxies().registerJdkProxy(FirstTestInterface.class, SecondTestInterface.class);
		assertPredicateMatches(this.proxy.forInterfaces(FirstTestInterface.class, SecondTestInterface.class));
	}

	@Test
	void proxyForInterfacesDoesNotMatchProxyHintDifferentOrder() {
		this.runtimeHints.proxies().registerJdkProxy(SecondTestInterface.class, FirstTestInterface.class);
		assertPredicateDoesNotMatch(this.proxy.forInterfaces(FirstTestInterface.class, SecondTestInterface.class));
	}

	interface FirstTestInterface {

	}

	interface SecondTestInterface {

	}

	private void assertPredicateMatches(Predicate<RuntimeHints> predicate) {
		assertThat(predicate.test(this.runtimeHints)).isTrue();
	}

	private void assertPredicateDoesNotMatch(Predicate<RuntimeHints> predicate) {
		assertThat(predicate.test(this.runtimeHints)).isFalse();
	}

}

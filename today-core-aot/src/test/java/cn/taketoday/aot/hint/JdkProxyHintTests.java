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

package cn.taketoday.aot.hint;

import org.junit.jupiter.api.Test;

import java.util.function.Consumer;
import java.util.function.Function;

import cn.taketoday.aot.hint.JdkProxyHint.Builder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JdkProxyHint}.
 *
 * @author Stephane Nicoll
 */
class JdkProxyHintTests {

	@Test
	void equalsWithSameInstanceIsTrue() {
		JdkProxyHint hint = new Builder().proxiedInterfaces(Function.class, Consumer.class).build();
		assertThat(hint).isEqualTo(hint);
	}

	@Test
	void equalsWithSameProxiedInterfacesIsTrue() {
		JdkProxyHint first = new Builder().proxiedInterfaces(Function.class, Consumer.class).build();
		JdkProxyHint second = new Builder().proxiedInterfaces(TypeReference.of(Function.class.getName()),
				TypeReference.of(Consumer.class)).build();
		assertThat(first).isEqualTo(second);
	}

	@Test
	void equalsWithSameProxiedInterfacesAndDifferentConditionIsFalse() {
		JdkProxyHint first = new Builder().proxiedInterfaces(Function.class, Consumer.class)
				.onReachableType(TypeReference.of(String.class)).build();
		JdkProxyHint second = new Builder().proxiedInterfaces(TypeReference.of(Function.class.getName()),
				TypeReference.of(Consumer.class)).onReachableType(TypeReference.of(Function.class)).build();
		assertThat(first).isNotEqualTo(second);
	}

	@Test
	void equalsWithSameProxiedInterfacesDifferentOrderIsFalse() {
		JdkProxyHint first = new Builder().proxiedInterfaces(Function.class, Consumer.class).build();
		JdkProxyHint second = new Builder().proxiedInterfaces(TypeReference.of(Consumer.class),
				TypeReference.of(Function.class.getName())).build();
		assertThat(first).isNotEqualTo(second);
	}

	@Test
	void equalsWithDifferentProxiedInterfacesIsFalse() {
		JdkProxyHint first = new Builder().proxiedInterfaces(Function.class).build();
		JdkProxyHint second = new Builder().proxiedInterfaces(TypeReference.of(Function.class.getName()),
				TypeReference.of(Consumer.class)).build();
		assertThat(first).isNotEqualTo(second);
	}

	@Test
	void equalsWithNonJdkProxyHintIsFalse() {
		JdkProxyHint first = new Builder().proxiedInterfaces(Function.class).build();
		TypeReference second = TypeReference.of(Function.class);
		assertThat(first).isNotEqualTo(second);
	}

}

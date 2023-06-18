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

import org.junit.jupiter.api.Test;

import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.hint.TypeReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SerializationHintsPredicates}.
 *
 * @author Stephane Nicoll
 */
class SerializationHintsPredicatesTests {

	private final SerializationHintsPredicates serialization = new SerializationHintsPredicates();

	private final RuntimeHints runtimeHints = new RuntimeHints();

	@Test
	void shouldMatchRegisteredClass() {
		runtimeHints.serialization().registerType(String.class);
		assertThat(serialization.onType(String.class).test(runtimeHints)).isTrue();
	}

	@Test
	void shouldMatchRegisteredTypeReference() {
		runtimeHints.serialization().registerType(TypeReference.of(String.class));
		assertThat(serialization.onType(String.class).test(runtimeHints)).isTrue();
	}

	@Test
	void shouldNotMatchUnregisteredType() {
		runtimeHints.serialization().registerType(Integer.class);
		assertThat(serialization.onType(Long.class).test(runtimeHints)).isFalse();
	}

}

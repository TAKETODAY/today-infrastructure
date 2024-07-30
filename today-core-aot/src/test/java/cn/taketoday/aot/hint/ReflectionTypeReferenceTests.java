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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Tests for {@link ReflectionTypeReference}.
 *
 * @author Stephane Nicoll
 * @author Moritz Halbritter
 * @author Sebastien Deleuze
 */
class ReflectionTypeReferenceTests {

	@Test
	void typeReferenceWithNullClass() {
		assertThatIllegalArgumentException().isThrownBy(() -> ReflectionTypeReference.of(null));
	}

	@Test
	void typeReferenceWithLambda() {
		Runnable lambda = () -> { };
		assertThatIllegalArgumentException().isThrownBy(() -> ReflectionTypeReference.of(lambda.getClass()));
	}

	@ParameterizedTest
	@MethodSource("reflectionTargetNames")
	void typeReferenceFromClassHasSuitableReflectionTargetName(Class<?> clazz, String binaryName) {
		assertThat(ReflectionTypeReference.of(clazz).getName()).isEqualTo(binaryName);
	}

	static Stream<Arguments> reflectionTargetNames() {
		return Stream.of(
				arguments(int.class, "int"),
				arguments(int[].class, "int[]"),
				arguments(Integer[].class, "java.lang.Integer[]"),
				arguments(Object[].class, "java.lang.Object[]"),
				arguments(StaticNested.class, "cn.taketoday.aot.hint.ReflectionTypeReferenceTests$StaticNested"),
				arguments(StaticNested[].class, "cn.taketoday.aot.hint.ReflectionTypeReferenceTests$StaticNested[]"),
				arguments(Inner.class, "cn.taketoday.aot.hint.ReflectionTypeReferenceTests$Inner"),
				arguments(Inner[].class, "cn.taketoday.aot.hint.ReflectionTypeReferenceTests$Inner[]")
		);
	}

	static class StaticNested {
	}

	class Inner {
	}

}

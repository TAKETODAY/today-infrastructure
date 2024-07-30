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
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link SimpleTypeReference}.
 *
 * @author Stephane Nicoll
 */
class SimpleTypeReferenceTests {


	@ParameterizedTest
	@MethodSource("primitivesAndPrimitivesArray")
	void primitivesAreHandledProperly(TypeReference typeReference, String expectedName) {
		assertThat(typeReference.getName()).isEqualTo(expectedName);
		assertThat(typeReference.getCanonicalName()).isEqualTo(expectedName);
		assertThat(typeReference.getPackageName()).isEqualTo("java.lang");
	}

	static Stream<Arguments> primitivesAndPrimitivesArray() {
		return Stream.of(
				Arguments.of(SimpleTypeReference.of("boolean"), "boolean"),
				Arguments.of(SimpleTypeReference.of("byte"), "byte"),
				Arguments.of(SimpleTypeReference.of("short"), "short"),
				Arguments.of(SimpleTypeReference.of("int"), "int"),
				Arguments.of(SimpleTypeReference.of("long"), "long"),
				Arguments.of(SimpleTypeReference.of("char"), "char"),
				Arguments.of(SimpleTypeReference.of("float"), "float"),
				Arguments.of(SimpleTypeReference.of("double"), "double"),
				Arguments.of(SimpleTypeReference.of("boolean[]"), "boolean[]"),
				Arguments.of(SimpleTypeReference.of("byte[]"), "byte[]"),
				Arguments.of(SimpleTypeReference.of("short[]"), "short[]"),
				Arguments.of(SimpleTypeReference.of("int[]"), "int[]"),
				Arguments.of(SimpleTypeReference.of("long[]"), "long[]"),
				Arguments.of(SimpleTypeReference.of("char[]"), "char[]"),
				Arguments.of(SimpleTypeReference.of("float[]"), "float[]"),
				Arguments.of(SimpleTypeReference.of("double[]"), "double[]"));
	}

	@ParameterizedTest
	@MethodSource("arrays")
	void arraysHaveSuitableReflectionTargetName(TypeReference typeReference, String expectedName) {
		assertThat(typeReference.getName()).isEqualTo(expectedName);
	}

	static Stream<Arguments> arrays() {
		return Stream.of(
				Arguments.of(SimpleTypeReference.of("java.lang.Object[]"), "java.lang.Object[]"),
				Arguments.of(SimpleTypeReference.of("java.lang.Integer[]"), "java.lang.Integer[]"),
				Arguments.of(SimpleTypeReference.of("com.example.Test[]"), "com.example.Test[]"));
	}

	@Test
	void nameOfCglibProxy() {
		TypeReference reference = TypeReference.of("com.example.Test$$Infra$$0");
		assertThat(reference.getSimpleName()).isEqualTo("Test$$Infra$$0");
		assertThat(reference.getEnclosingType()).isNull();
	}

	@Test
	void nameOfNestedCglibProxy() {
		TypeReference reference = TypeReference.of("com.example.Test$Another$$Infra$$0");
		assertThat(reference.getSimpleName()).isEqualTo("Another$$Infra$$0");
		assertThat(reference.getEnclosingType()).isNotNull();
		assertThat(reference.getEnclosingType().getSimpleName()).isEqualTo("Test");
	}

	@Test
	void typeReferenceInRootPackage() {
		TypeReference type = SimpleTypeReference.of("MyRootClass");
		assertThat(type.getCanonicalName()).isEqualTo("MyRootClass");
		assertThat(type.getPackageName()).isEmpty();
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = { "com.example.Tes(t", "com.example..Test" })
	void typeReferenceWithInvalidClassName(String invalidClassName) {
		assertThatIllegalStateException().isThrownBy(() -> SimpleTypeReference.of(invalidClassName))
				.withMessageContaining("Invalid class name");
	}

}

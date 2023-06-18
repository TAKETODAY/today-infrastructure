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

import java.util.List;
import java.util.function.Consumer;

import cn.taketoday.aot.hint.TypeHint.Builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link TypeHint}.
 *
 * @author Stephane Nicoll
 */
class TypeHintTests {

	@Test
	void createWithNullTypeReference() {
		assertThatIllegalArgumentException().isThrownBy(() -> TypeHint.of(null));
	}

	@Test
	void createWithType() {
		TypeHint hint = TypeHint.of(TypeReference.of(String.class)).build();
		assertThat(hint).isNotNull();
		assertThat(hint.getType().getCanonicalName()).isEqualTo("java.lang.String");
	}

	@Test
	void createWithTypeAndReachableType() {
		TypeHint hint = TypeHint.of(TypeReference.of(String.class))
				.onReachableType(TypeReference.of("com.example.Test")).build();
		assertThat(hint).isNotNull();
		assertThat(hint.getReachableType()).isNotNull();
		assertThat(hint.getReachableType().getCanonicalName()).isEqualTo("com.example.Test");
	}

	@Test
	void createWithField() {
		assertFieldHint(TypeHint.of(TypeReference.of(String.class))
				.withField("value"), fieldHint -> assertThat(fieldHint.getName()).isEqualTo("value"));
	}

	void assertFieldHint(Builder builder, Consumer<FieldHint> fieldHint) {
		TypeHint hint = builder.build();
		assertThat(hint.fields()).singleElement().satisfies(fieldHint);
		assertThat(hint.constructors()).isEmpty();
		assertThat(hint.methods()).isEmpty();
		assertThat(hint.getMemberCategories()).isEmpty();
	}

	@Test
	void createWithConstructor() {
		List<TypeReference> parameterTypes = TypeReference.listOf(byte[].class, int.class);
		assertConstructorHint(TypeHint.of(TypeReference.of(String.class))
				.withConstructor(parameterTypes, ExecutableMode.INTROSPECT), constructorHint -> {
			assertThat(constructorHint.getParameterTypes()).containsOnlyOnceElementsOf(parameterTypes);
			assertThat(constructorHint.getMode()).isEqualTo(ExecutableMode.INTROSPECT);
		});
	}

	@Test
	void createWithConstructorWithSameConstructorUpdatesEntry() {
		List<TypeReference> parameterTypes = TypeReference.listOf(byte[].class, int.class);
		Builder builder = TypeHint.of(TypeReference.of(String.class))
				.withConstructor(parameterTypes, ExecutableMode.INTROSPECT);
		assertConstructorHint(builder.withConstructor(parameterTypes, ExecutableMode.INVOKE), constructorHint -> {
			assertThat(constructorHint.getParameterTypes()).containsExactlyElementsOf(parameterTypes);
			assertThat(constructorHint.getMode()).isEqualTo(ExecutableMode.INVOKE);
		});
	}

	@Test
	void createWithConstructorAndSameConstructorAppliesExecutableModePrecedence() {
		List<TypeReference> parameterTypes = TypeReference.listOf(byte[].class, int.class);
		Builder builder = TypeHint.of(TypeReference.of(String.class))
				.withConstructor(parameterTypes, ExecutableMode.INVOKE);
		assertConstructorHint(builder.withConstructor(parameterTypes, ExecutableMode.INTROSPECT), constructorHint -> {
			assertThat(constructorHint.getParameterTypes()).containsExactlyElementsOf(parameterTypes);
			assertThat(constructorHint.getMode()).isEqualTo(ExecutableMode.INVOKE);
		});
	}

	void assertConstructorHint(Builder builder, Consumer<ExecutableHint> constructorHint) {
		TypeHint hint = builder.build();
		assertThat(hint.fields()).isEmpty();
		assertThat(hint.constructors()).singleElement().satisfies(constructorHint);
		assertThat(hint.methods()).isEmpty();
		assertThat(hint.getMemberCategories()).isEmpty();
	}

	@Test
	void createWithMethod() {
		List<TypeReference> parameterTypes = List.of(TypeReference.of(char[].class));
		assertMethodHint(TypeHint.of(TypeReference.of(String.class))
				.withMethod("valueOf", parameterTypes, ExecutableMode.INTROSPECT), methodHint -> {
			assertThat(methodHint.getName()).isEqualTo("valueOf");
			assertThat(methodHint.getParameterTypes()).containsExactlyElementsOf(parameterTypes);
			assertThat(methodHint.getMode()).isEqualTo(ExecutableMode.INTROSPECT);
		});
	}

	@Test
	void createWithMethodWithSameMethodUpdatesEntry() {
		List<TypeReference> parameterTypes = TypeReference.listOf(char[].class);
		Builder builder = TypeHint.of(TypeReference.of(String.class))
				.withMethod("valueOf", parameterTypes, ExecutableMode.INTROSPECT);
		assertMethodHint(builder.withMethod("valueOf", parameterTypes, ExecutableMode.INVOKE), methodHint -> {
			assertThat(methodHint.getName()).isEqualTo("valueOf");
			assertThat(methodHint.getParameterTypes()).containsExactlyElementsOf(parameterTypes);
			assertThat(methodHint.getMode()).isEqualTo(ExecutableMode.INVOKE);
		});
	}

	@Test
	void createWithMethodAndSameMethodAppliesExecutableModePrecedence() {
		List<TypeReference> parameterTypes = TypeReference.listOf(char[].class);
		Builder builder = TypeHint.of(TypeReference.of(String.class))
				.withMethod("valueOf", parameterTypes, ExecutableMode.INVOKE);
		assertMethodHint(builder.withMethod("valueOf", parameterTypes, ExecutableMode.INTROSPECT), methodHint -> {
			assertThat(methodHint.getName()).isEqualTo("valueOf");
			assertThat(methodHint.getParameterTypes()).containsExactlyElementsOf(parameterTypes);
			assertThat(methodHint.getMode()).isEqualTo(ExecutableMode.INVOKE);
		});
	}

	void assertMethodHint(Builder builder, Consumer<ExecutableHint> methodHint) {
		TypeHint hint = builder.build();
		assertThat(hint.fields()).isEmpty();
		assertThat(hint.constructors()).isEmpty();
		assertThat(hint.methods()).singleElement().satisfies(methodHint);
		assertThat(hint.getMemberCategories()).isEmpty();
	}


	@Test
	void createWithMemberCategory() {
		TypeHint hint = TypeHint.of(TypeReference.of(String.class))
				.withMembers(MemberCategory.DECLARED_FIELDS).build();
		assertThat(hint.getMemberCategories()).containsOnly(MemberCategory.DECLARED_FIELDS);
	}

	@Test
	void typeHintHasAppropriateToString() {
		TypeHint hint = TypeHint.of(TypeReference.of(String.class)).build();
		assertThat(hint).hasToString("TypeHint[type=java.lang.String]");
	}

	@Test
	void builtWithAppliesMemberCategories() {
		TypeHint.Builder builder = new TypeHint.Builder(TypeReference.of(String.class));
		assertThat(builder.build().getMemberCategories()).isEmpty();
		TypeHint.builtWith(MemberCategory.DECLARED_CLASSES, MemberCategory.DECLARED_FIELDS).accept(builder);
		assertThat(builder.build().getMemberCategories()).containsExactlyInAnyOrder(MemberCategory.DECLARED_CLASSES,
				MemberCategory.DECLARED_FIELDS);
	}

}

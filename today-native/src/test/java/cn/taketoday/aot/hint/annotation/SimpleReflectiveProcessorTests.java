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

package cn.taketoday.aot.hint.annotation;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import cn.taketoday.aot.hint.ExecutableMode;
import cn.taketoday.aot.hint.ReflectionHints;
import cn.taketoday.aot.hint.TypeReference;

/**
 * Tests for {@link SimpleReflectiveProcessor}.
 *
 * @author Stephane Nicoll
 */
class SimpleReflectiveProcessorTests {

	private final SimpleReflectiveProcessor processor = new SimpleReflectiveProcessor();

	private final ReflectionHints hints = new ReflectionHints();

	@Test
	void registerReflectiveHintsForClass() {
		processor.registerReflectionHints(hints, SampleBean.class);
		assertThat(hints.typeHints()).singleElement().satisfies(typeHint -> {
			assertThat(typeHint.getType()).isEqualTo(TypeReference.of(SampleBean.class));
			assertThat(typeHint.getMemberCategories()).isEmpty();
			assertThat(typeHint.constructors()).isEmpty();
			assertThat(typeHint.fields()).isEmpty();
			assertThat(typeHint.methods()).isEmpty();
		});
	}

	@Test
	void registerReflectiveHintsForConstructor() {
		Constructor<?> constructor = SampleBean.class.getDeclaredConstructors()[0];
		processor.registerReflectionHints(hints, constructor);
		assertThat(hints.typeHints()).singleElement().satisfies(typeHint -> {
			assertThat(typeHint.getType()).isEqualTo(TypeReference.of(SampleBean.class));
			assertThat(typeHint.getMemberCategories()).isEmpty();
			assertThat(typeHint.constructors()).singleElement().satisfies(constructorHint -> {
				assertThat(constructorHint.getName()).isEqualTo("<init>");
				assertThat(constructorHint.getMode()).isEqualTo(ExecutableMode.INVOKE);
				assertThat(constructorHint.getParameterTypes()).containsExactly(TypeReference.of(String.class));
			});
			assertThat(typeHint.fields()).isEmpty();
			assertThat(typeHint.methods()).isEmpty();
		});
	}

	@Test
	void registerReflectiveHintsForField() throws NoSuchFieldException {
		Field field = SampleBean.class.getDeclaredField("name");
		processor.registerReflectionHints(hints, field);
		assertThat(hints.typeHints()).singleElement().satisfies(typeHint -> {
			assertThat(typeHint.getType()).isEqualTo(TypeReference.of(SampleBean.class));
			assertThat(typeHint.getMemberCategories()).isEmpty();
			assertThat(typeHint.constructors()).isEmpty();
			assertThat(typeHint.fields()).singleElement().satisfies(fieldHint ->
					assertThat(fieldHint.getName()).isEqualTo("name"));
			assertThat(typeHint.methods()).isEmpty();
		});
	}

	@Test
	void registerReflectiveHintsForMethod() throws NoSuchMethodException {
		Method method = SampleBean.class.getDeclaredMethod("setName", String.class);
		processor.registerReflectionHints(hints, method);
		assertThat(hints.typeHints()).singleElement().satisfies(typeHint -> {
			assertThat(typeHint.getType()).isEqualTo(TypeReference.of(SampleBean.class));
			assertThat(typeHint.getMemberCategories()).isEmpty();
			assertThat(typeHint.constructors()).isEmpty();
			assertThat(typeHint.fields()).isEmpty();
			assertThat(typeHint.methods()).singleElement().satisfies(methodHint -> {
				assertThat(methodHint.getName()).isEqualTo("setName");
				assertThat(methodHint.getMode()).isEqualTo(ExecutableMode.INVOKE);
				assertThat(methodHint.getParameterTypes()).containsExactly(TypeReference.of(String.class));
			});
		});
	}

	static class SampleBean {

		@SuppressWarnings("unused")
		private String name;

		SampleBean(String name) {
			this.name = name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

}

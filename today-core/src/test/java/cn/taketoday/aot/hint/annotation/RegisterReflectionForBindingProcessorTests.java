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

import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.hint.predicate.RuntimeHintsPredicates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link RegisterReflectionForBindingProcessor}.
 *
 * @author Sebastien Deleuze
 */
public class RegisterReflectionForBindingProcessorTests {

	private final RegisterReflectionForBindingProcessor processor = new RegisterReflectionForBindingProcessor();

	private final RuntimeHints hints = new RuntimeHints();

	@Test
	void registerReflectionForBindingOnClass() {
		processor.registerReflectionHints(hints.reflection(), ClassLevelAnnotatedBean.class);
		assertThat(RuntimeHintsPredicates.reflection().onType(SampleClassWithGetter.class)).accepts(hints);
		assertThat(RuntimeHintsPredicates.reflection().onType(String.class)).accepts(hints);
		assertThat(RuntimeHintsPredicates.reflection().onMethod(SampleClassWithGetter.class, "getName")).accepts(hints);
	}

	@Test
	void registerReflectionForBindingOnMethod() throws NoSuchMethodException {
		processor.registerReflectionHints(hints.reflection(), MethodLevelAnnotatedBean.class.getMethod("method"));
		assertThat(RuntimeHintsPredicates.reflection().onType(SampleClassWithGetter.class)).accepts(hints);
		assertThat(RuntimeHintsPredicates.reflection().onType(String.class)).accepts(hints);
		assertThat(RuntimeHintsPredicates.reflection().onMethod(SampleClassWithGetter.class, "getName")).accepts(hints);
	}

	@Test
	void throwExceptionWithoutAnnotationAttributeOnClass() {
		assertThatThrownBy(() -> processor.registerReflectionHints(hints.reflection(),
				SampleClassWithoutAnnotationAttribute.class))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void throwExceptionWithoutAnnotationAttributeOnMethod() throws NoSuchMethodException {
		assertThatThrownBy(() -> processor.registerReflectionHints(hints.reflection(),
				SampleClassWithoutMethodLevelAnnotationAttribute.class.getMethod("method")))
				.isInstanceOf(IllegalStateException.class);
	}


	@RegisterReflectionForBinding(SampleClassWithGetter.class)
	static class ClassLevelAnnotatedBean {
	}

	static class MethodLevelAnnotatedBean {

		@RegisterReflectionForBinding(SampleClassWithGetter.class)
		public void method() {
		}
	}

	static class SampleClassWithGetter {

		public String getName() {
			return null;
		}
	}

	@RegisterReflectionForBinding
	static class SampleClassWithoutAnnotationAttribute {
	}

	static class SampleClassWithoutMethodLevelAnnotationAttribute {

		@RegisterReflectionForBinding
		public void method() {
		}
	}

}

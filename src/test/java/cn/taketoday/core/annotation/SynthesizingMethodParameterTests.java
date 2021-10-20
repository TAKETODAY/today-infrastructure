/*
 * Copyright 2002-2020 the original author or authors.
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

package cn.taketoday.core.annotation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import cn.taketoday.core.MethodParameter;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Juergen Hoeller
 * @since 5.0
 */
class SynthesizingMethodParameterTests {

	private Method method;

	private SynthesizingMethodParameter stringParameter;

	private SynthesizingMethodParameter longParameter;

	private SynthesizingMethodParameter intReturnType;


	@BeforeEach
	void setUp() throws NoSuchMethodException {
		method = getClass().getMethod("method", String.class, Long.TYPE);
		stringParameter = new SynthesizingMethodParameter(method, 0);
		longParameter = new SynthesizingMethodParameter(method, 1);
		intReturnType = new SynthesizingMethodParameter(method, -1);
	}


	@Test
	void equals() throws NoSuchMethodException {
		assertThat(stringParameter).isEqualTo(stringParameter);
		assertThat(longParameter).isEqualTo(longParameter);
		assertThat(intReturnType).isEqualTo(intReturnType);

		assertThat(stringParameter.equals(longParameter)).isFalse();
		assertThat(stringParameter.equals(intReturnType)).isFalse();
		assertThat(longParameter.equals(stringParameter)).isFalse();
		assertThat(longParameter.equals(intReturnType)).isFalse();
		assertThat(intReturnType.equals(stringParameter)).isFalse();
		assertThat(intReturnType.equals(longParameter)).isFalse();

		Method method = getClass().getMethod("method", String.class, Long.TYPE);
		MethodParameter methodParameter = new SynthesizingMethodParameter(method, 0);
		assertThat(methodParameter).isEqualTo(stringParameter);
		assertThat(stringParameter).isEqualTo(methodParameter);
		assertThat(methodParameter).isNotEqualTo(longParameter);
		assertThat(longParameter).isNotEqualTo(methodParameter);

		methodParameter = new MethodParameter(method, 0);
		assertThat(methodParameter).isEqualTo(stringParameter);
		assertThat(stringParameter).isEqualTo(methodParameter);
		assertThat(methodParameter).isNotEqualTo(longParameter);
		assertThat(longParameter).isNotEqualTo(methodParameter);
	}

	@Test
	void testHashCode() throws NoSuchMethodException {
		assertThat(stringParameter.hashCode()).isEqualTo(stringParameter.hashCode());
		assertThat(longParameter.hashCode()).isEqualTo(longParameter.hashCode());
		assertThat(intReturnType.hashCode()).isEqualTo(intReturnType.hashCode());

		Method method = getClass().getMethod("method", String.class, Long.TYPE);
		SynthesizingMethodParameter methodParameter = new SynthesizingMethodParameter(method, 0);
		assertThat(methodParameter.hashCode()).isEqualTo(stringParameter.hashCode());
		assertThat(methodParameter.hashCode()).isNotEqualTo(longParameter.hashCode());
	}

	@Test
	void factoryMethods() {
		assertThat(SynthesizingMethodParameter.forExecutable(method, 0)).isEqualTo(stringParameter);
		assertThat(SynthesizingMethodParameter.forExecutable(method, 1)).isEqualTo(longParameter);

		assertThat(SynthesizingMethodParameter.forParameter(method.getParameters()[0])).isEqualTo(stringParameter);
		assertThat(SynthesizingMethodParameter.forParameter(method.getParameters()[1])).isEqualTo(longParameter);
	}

	@Test
	void indexValidation() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new SynthesizingMethodParameter(method, 2));
	}


	public int method(String p1, long p2) {
		return 42;
	}

}

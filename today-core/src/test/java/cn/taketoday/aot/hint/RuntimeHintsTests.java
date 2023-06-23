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

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RuntimeHints}.
 *
 * @author Stephane Nicoll
 */
class RuntimeHintsTests {

	private final RuntimeHints hints = new RuntimeHints();


	@Test
	void reflectionHintWithClass() {
		this.hints.reflection().registerType(String.class, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
		assertThat(this.hints.reflection().typeHints()).singleElement().satisfies(typeHint -> {
			assertThat(typeHint.getType().getCanonicalName()).isEqualTo(String.class.getCanonicalName());
			assertThat(typeHint.fields()).isEmpty();
			assertThat(typeHint.constructors()).isEmpty();
			assertThat(typeHint.methods()).isEmpty();
			assertThat(typeHint.getMemberCategories()).containsOnly(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
		});
	}

	@Test
	void resourceHintWithClass() {
		this.hints.resources().registerType(String.class);
		assertThat(this.hints.resources().resourcePatternHints()).singleElement().satisfies(resourceHint -> {
			assertThat(resourceHint.getIncludes()).map(ResourcePatternHint::getPattern)
					.containsExactlyInAnyOrder("/", "java", "java/lang", "java/lang/String.class");
			assertThat(resourceHint.getExcludes()).isEmpty();
		});
	}

	@Test
	void javaSerializationHintWithClass() {
		this.hints.serialization().registerType(String.class);
		assertThat(this.hints.serialization().javaSerializationHints().map(JavaSerializationHint::getType))
				.containsExactly(TypeReference.of(String.class));
	}

	@Test
	void jdkProxyWithClass() {
		this.hints.proxies().registerJdkProxy(Function.class);
		assertThat(this.hints.proxies().jdkProxyHints()).singleElement().satisfies(jdkProxyHint ->
				assertThat(jdkProxyHint.getProxiedInterfaces()).containsExactly(TypeReference.of(Function.class)));
	}

}

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

package cn.taketoday.core.test.tools;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link SourceFile}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
class SourceFileTests {

	private static final String HELLO_WORLD = """
			package com.example.helloworld;

			public class HelloWorld {
				public static void main(String[] args) {
					System.out.println("Hello World!");
				}
			}
			""";

	@Test
	void ofWhenContentIsEmptyThrowsException() {
		assertThatIllegalStateException().isThrownBy(() -> SourceFile.of("")).withMessage(
				"WritableContent did not append any content");
	}

	@Test
	void ofWhenSourceDefinesNoClassThrowsException() {
		assertThatIllegalStateException().isThrownBy(
				() -> SourceFile.of("package com.example;")).withMessageContaining(
						"Unable to parse").havingCause().withMessage(
								"Source must define a single class");
	}

	@Test
	void ofWhenSourceDefinesMultipleClassesThrowsException() {
		assertThatIllegalStateException().isThrownBy(() -> SourceFile.of(
				"public class One {}\npublic class Two{}")).withMessageContaining(
						"Unable to parse").havingCause().withMessage(
								"Source must define a single class");
	}

	@Test
	void ofWhenSourceCannotBeParsedThrowsException() {
		assertThatIllegalStateException().isThrownBy(
				() -> SourceFile.of("well this is broken {")).withMessageContaining(
						"Unable to parse source file content");
	}

	@Test
	void ofWithoutPathDeducesPath() {
		SourceFile sourceFile = SourceFile.of(HELLO_WORLD);
		assertThat(sourceFile.getPath()).isEqualTo(
				"com/example/helloworld/HelloWorld.java");
	}

	@Test
	void ofWithPathUsesPath() {
		SourceFile sourceFile = SourceFile.of("com/example/DifferentPath.java",
				HELLO_WORLD);
		assertThat(sourceFile.getPath()).isEqualTo("com/example/DifferentPath.java");
	}

	@Test
	void forClassWithClassUsesClass() {
		SourceFile sourceFile = SourceFile.forClass(new File("src/test/java"), SourceFileTests.class);
		assertThat(sourceFile.getPath()).isEqualTo("cn/taketoday/core/test/tools/SourceFileTests.java");
		assertThat(sourceFile.getClassName()).isEqualTo("cn.taketoday.core.test.tools.SourceFileTests");
	}

	@Test
	void forTestClassWithClassUsesClass() {
		SourceFile sourceFile = SourceFile.forTestClass(SourceFileTests.class);
		assertThat(sourceFile.getPath()).isEqualTo("cn/taketoday/core/test/tools/SourceFileTests.java");
		assertThat(sourceFile.getClassName()).isEqualTo("cn.taketoday.core.test.tools.SourceFileTests");
	}

	@Test
	void getContentReturnsContent() {
		SourceFile sourceFile = SourceFile.of(HELLO_WORLD);
		assertThat(sourceFile.getContent()).isEqualTo(HELLO_WORLD);
	}

	@Test
	@SuppressWarnings("deprecation")
	void assertThatReturnsAssert() {
		SourceFile sourceFile = SourceFile.of(HELLO_WORLD);
		assertThat(sourceFile.assertThat()).isInstanceOf(SourceFileAssert.class);
	}

	@Test
	void createFromJavaPoetStyleApi() {
		JavaFile javaFile = new JavaFile(HELLO_WORLD);
		SourceFile sourceFile = SourceFile.of(javaFile::writeTo);
		assertThat(sourceFile.getContent()).isEqualTo(HELLO_WORLD);
	}

	@Test
	void getClassNameFromSimpleRecord() {
		SourceFile sourceFile = SourceFile.of("""
				package com.example.helloworld;

				record HelloWorld(String name) {
				}
				""");
		assertThat(sourceFile.getClassName()).isEqualTo("com.example.helloworld.HelloWorld");
	}

	@Test
	void getClassNameFromMoreComplexRecord() {
		SourceFile sourceFile = SourceFile.of("""
				package com.example.helloworld;

				public record HelloWorld(String name) {

					String getFoo() {
						return name();
					}

				}
				""");
		assertThat(sourceFile.getClassName()).isEqualTo("com.example.helloworld.HelloWorld");
	}

	@Test
	void getClassNameFromAnnotatedRecord() {
		SourceFile sourceFile = SourceFile.of("""
			package com.example;

			public record RecordProperties(
					@cn.taketoday.lang.NonNull("test") String property1,
					@cn.taketoday.lang.NonNull("test") String property2) {
			}
		""");
		assertThat(sourceFile.getClassName()).isEqualTo("com.example.RecordProperties");
	}

	/**
	 * JavaPoet style API with a {@code writeTo} method.
	 */
	static class JavaFile {

		private final String content;

		JavaFile(String content) {
			this.content = content;
		}

		void writeTo(Appendable out) throws IOException {
			out.append(this.content);
		}

	}

}

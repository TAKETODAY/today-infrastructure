/*
 * Copyright 2012 - 2023 the original author or authors.
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

package cn.taketoday.app.loader.tools;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import cn.taketoday.app.loader.tools.MainClassFinder.MainClass;
import cn.taketoday.app.loader.tools.MainClassFinder.MainClassCallback;
import cn.taketoday.app.loader.tools.sample.AnnotatedClassWithMainMethod;
import cn.taketoday.app.loader.tools.sample.ClassWithMainMethod;
import cn.taketoday.app.loader.tools.sample.ClassWithoutMainMethod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link MainClassFinder}.
 *
 * @author Phillip Webb
 */
class MainClassFinderTests {

	private TestJarFile testJarFile;

	@BeforeEach
	void setup(@TempDir File tempDir) {
		this.testJarFile = new TestJarFile(tempDir);
	}

	@Test
	void findMainClassInJar() throws Exception {
		this.testJarFile.addClass("B.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("A.class", ClassWithoutMainMethod.class);
		try (JarFile jarFile = this.testJarFile.getJarFile()) {
			String actual = MainClassFinder.findMainClass(jarFile, "");
			assertThat(actual).isEqualTo("B");
		}
	}

	@Test
	void findMainClassInJarSubDirectory() throws Exception {
		this.testJarFile.addClass("a/b/c/D.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("a/b/c/E.class", ClassWithoutMainMethod.class);
		this.testJarFile.addClass("a/b/F.class", ClassWithoutMainMethod.class);
		try (JarFile jarFile = this.testJarFile.getJarFile()) {
			String actual = MainClassFinder.findMainClass(jarFile, "");
			assertThat(actual).isEqualTo("a.b.c.D");
		}
	}

	@Test
	void usesBreadthFirstJarSearch() throws Exception {
		this.testJarFile.addClass("a/B.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("a/b/c/E.class", ClassWithMainMethod.class);
		try (JarFile jarFile = this.testJarFile.getJarFile()) {
			String actual = MainClassFinder.findMainClass(jarFile, "");
			assertThat(actual).isEqualTo("a.B");
		}
	}

	@Test
	void findSingleJarSearch() throws Exception {
		this.testJarFile.addClass("a/B.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("a/b/c/E.class", ClassWithMainMethod.class);
		try (JarFile jarFile = this.testJarFile.getJarFile()) {
			assertThatIllegalStateException().isThrownBy(() -> MainClassFinder.findSingleMainClass(jarFile, ""))
				.withMessageContaining(
						"Unable to find a single main class from the following candidates [a.B, a.b.c.E]");
		}
	}

	@Test
	void findSingleJarSearchPrefersAnnotatedMainClass() throws Exception {
		this.testJarFile.addClass("a/B.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("a/b/c/E.class", AnnotatedClassWithMainMethod.class);
		try (JarFile jarFile = this.testJarFile.getJarFile()) {
			String mainClass = MainClassFinder.findSingleMainClass(jarFile, "",
					"cn.taketoday.app.loader.tools.sample.SomeApplication");
			assertThat(mainClass).isEqualTo("a.b.c.E");
		}
	}

	@Test
	void findMainClassInJarSubLocation() throws Exception {
		this.testJarFile.addClass("a/B.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("a/b/c/E.class", ClassWithMainMethod.class);
		try (JarFile jarFile = this.testJarFile.getJarFile()) {
			String actual = MainClassFinder.findMainClass(jarFile, "a/");
			assertThat(actual).isEqualTo("B");
		}

	}

	@Test
	void findMainClassInDirectory() throws Exception {
		this.testJarFile.addClass("B.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("A.class", ClassWithoutMainMethod.class);
		String actual = MainClassFinder.findMainClass(this.testJarFile.getJarSource());
		assertThat(actual).isEqualTo("B");
	}

	@Test
	void findMainClassInSubDirectory() throws Exception {
		this.testJarFile.addClass("a/b/c/D.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("a/b/c/E.class", ClassWithoutMainMethod.class);
		this.testJarFile.addClass("a/b/F.class", ClassWithoutMainMethod.class);
		String actual = MainClassFinder.findMainClass(this.testJarFile.getJarSource());
		assertThat(actual).isEqualTo("a.b.c.D");
	}

	@Test
	void usesBreadthFirstDirectorySearch() throws Exception {
		this.testJarFile.addClass("a/B.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("a/b/c/E.class", ClassWithMainMethod.class);
		String actual = MainClassFinder.findMainClass(this.testJarFile.getJarSource());
		assertThat(actual).isEqualTo("a.B");
	}

	@Test
	void findSingleDirectorySearch() throws Exception {
		this.testJarFile.addClass("a/B.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("a/b/c/E.class", ClassWithMainMethod.class);
		assertThatIllegalStateException()
			.isThrownBy(() -> MainClassFinder.findSingleMainClass(this.testJarFile.getJarSource()))
			.withMessageContaining("Unable to find a single main class from the following candidates [a.B, a.b.c.E]");
	}

	@Test
	void findSingleDirectorySearchPrefersAnnotatedMainClass() throws Exception {
		this.testJarFile.addClass("a/B.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("a/b/c/E.class", AnnotatedClassWithMainMethod.class);
		String mainClass = MainClassFinder.findSingleMainClass(this.testJarFile.getJarSource(),
				"cn.taketoday.app.loader.tools.sample.SomeApplication");
		assertThat(mainClass).isEqualTo("a.b.c.E");
	}

	@Test
	void doWithDirectoryMainMethods() throws Exception {
		this.testJarFile.addClass("a/b/c/D.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("a/b/c/E.class", ClassWithoutMainMethod.class);
		this.testJarFile.addClass("a/b/F.class", ClassWithoutMainMethod.class);
		this.testJarFile.addClass("a/b/G.class", ClassWithMainMethod.class);
		ClassNameCollector callback = new ClassNameCollector();
		MainClassFinder.doWithMainClasses(this.testJarFile.getJarSource(), callback);
		assertThat(callback.getClassNames()).hasToString("[a.b.G, a.b.c.D]");
	}

	@Test
	void doWithJarMainMethods() throws Exception {
		this.testJarFile.addClass("a/b/c/D.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("a/b/c/E.class", ClassWithoutMainMethod.class);
		this.testJarFile.addClass("a/b/F.class", ClassWithoutMainMethod.class);
		this.testJarFile.addClass("a/b/G.class", ClassWithMainMethod.class);
		ClassNameCollector callback = new ClassNameCollector();
		try (JarFile jarFile = this.testJarFile.getJarFile()) {
			MainClassFinder.doWithMainClasses(jarFile, null, callback);
			assertThat(callback.getClassNames()).hasToString("[a.b.G, a.b.c.D]");
		}
	}

	static class ClassNameCollector implements MainClassCallback<Object> {

		private final List<String> classNames = new ArrayList<>();

		@Override
		public Object doWith(MainClass mainClass) {
			this.classNames.add(mainClass.getName());
			return null;
		}

		List<String> getClassNames() {
			return this.classNames;
		}

	}

}

/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.aot.test.generate;

import java.util.function.UnaryOperator;

import infra.aot.generate.ClassNameGenerator;
import infra.aot.generate.DefaultGenerationContext;
import infra.aot.generate.GenerationContext;
import infra.aot.generate.InMemoryGeneratedFiles;
import infra.core.test.tools.TestCompiler;
import infra.javapoet.ClassName;

/**
 * {@link GenerationContext} test implementation that uses
 * {@link InMemoryGeneratedFiles} and can configure a {@link TestCompiler}
 * instance.
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 4.0
 */
public class TestGenerationContext extends DefaultGenerationContext implements UnaryOperator<TestCompiler> {

  /**
   * The default test target {@link ClassName}.
   */
  public static final ClassName TEST_TARGET = ClassName.get("com.example", "TestTarget");

  /**
   * Create an instance using the specified {@link ClassNameGenerator}.
   *
   * @param classNameGenerator the class name generator to use
   */
  public TestGenerationContext(ClassNameGenerator classNameGenerator) {
    super(classNameGenerator, new InMemoryGeneratedFiles());
  }

  /**
   * Create an instance using the specified {@code target}.
   *
   * @param target the default target class name to use
   */
  public TestGenerationContext(ClassName target) {
    this(new ClassNameGenerator(target));
  }

  /**
   * Create an instance using the specified {@code target}.
   *
   * @param target the default target class to use
   */
  public TestGenerationContext(Class<?> target) {
    this(ClassName.get(target));
  }

  /**
   * Create an instance using {@link #TEST_TARGET} as the {@code target}.
   */
  public TestGenerationContext() {
    this(TEST_TARGET);
  }

  @Override
  public InMemoryGeneratedFiles getGeneratedFiles() {
    return (InMemoryGeneratedFiles) super.getGeneratedFiles();
  }

  /**
   * Configure the specified {@link TestCompiler} with the state of this context.
   *
   * @param testCompiler the compiler to configure
   * @return a new {@link TestCompiler} instance configured with the generated files
   * @see TestCompiler#with(UnaryOperator)
   */
  @Override
  public TestCompiler apply(TestCompiler testCompiler) {
    return CompilerFiles.from(getGeneratedFiles()).apply(testCompiler);
  }

}

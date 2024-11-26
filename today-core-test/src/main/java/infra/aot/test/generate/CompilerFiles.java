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

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

import infra.aot.generate.GeneratedFiles;
import infra.aot.generate.GeneratedFiles.Kind;
import infra.aot.generate.InMemoryGeneratedFiles;
import infra.core.io.InputStreamSource;
import infra.core.test.tools.ClassFile;
import infra.core.test.tools.ResourceFile;
import infra.core.test.tools.SourceFile;
import infra.core.test.tools.TestCompiler;

/**
 * Adapter class that can be used to apply AOT {@link GeneratedFiles} to the
 * {@link TestCompiler}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 4.0
 */
public final class CompilerFiles implements UnaryOperator<TestCompiler> {

  private final InMemoryGeneratedFiles generatedFiles;

  private CompilerFiles(InMemoryGeneratedFiles generatedFiles) {
    this.generatedFiles = generatedFiles;
  }

  public static UnaryOperator<TestCompiler> from(
          InMemoryGeneratedFiles generatedFiles) {
    return new CompilerFiles(generatedFiles);
  }

  @Override
  public TestCompiler apply(TestCompiler testCompiler) {
    return testCompiler
            .withSources(adapt(Kind.SOURCE, (path, inputStreamSource) ->
                    SourceFile.of(inputStreamSource)))
            .withResources(adapt(Kind.RESOURCE, ResourceFile::of))
            .withClasses(adapt(Kind.CLASS, (path, inputStreamSource) ->
                    ClassFile.of(ClassFile.toClassName(path), inputStreamSource)));
  }

  private <T> List<T> adapt(Kind kind,
          BiFunction<String, InputStreamSource, T> adapter) {
    List<T> result = new ArrayList<>();
    this.generatedFiles.getGeneratedFiles(kind)
            .forEach((k, v) -> result.add(adapter.apply(k, v)));
    return result;
  }

}

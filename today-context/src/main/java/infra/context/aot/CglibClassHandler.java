/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.context.aot;

import java.util.function.Consumer;

import infra.aot.generate.GeneratedFiles;
import infra.aot.generate.GeneratedFiles.Kind;
import infra.aot.generate.GenerationContext;
import infra.aot.hint.MemberCategory;
import infra.aot.hint.RuntimeHints;
import infra.aot.hint.TypeHint.Builder;
import infra.aot.hint.TypeReference;
import infra.aot.hint.support.ClassHintUtils;
import infra.bytecode.BytecodeCompiler;
import infra.core.io.ByteArrayResource;

/**
 * Handle CGLIB classes by adding them to a {@link GenerationContext},
 * and register the necessary hints so that they can be instantiated.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see BytecodeCompiler#setGeneratedClassHandler
 * @see BytecodeCompiler#setLoadedClassHandler
 * @see ClassHintUtils#registerProxyIfNecessary
 * @since 4.0
 */
class CglibClassHandler {

  private static final Consumer<Builder> instantiateCglibProxy = hint ->
          hint.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);

  private final RuntimeHints runtimeHints;

  private final GeneratedFiles generatedFiles;

  CglibClassHandler(GenerationContext generationContext) {
    this.runtimeHints = generationContext.getRuntimeHints();
    this.generatedFiles = generationContext.getGeneratedFiles();
  }

  /**
   * Handle the specified generated CGLIB class.
   *
   * @param cglibClassName the name of the generated class
   * @param content the bytecode of the generated class
   */
  public void handleGeneratedClass(String cglibClassName, byte[] content) {
    registerHints(TypeReference.of(cglibClassName));
    String path = cglibClassName.replace(".", "/") + ".class";
    this.generatedFiles.addFile(Kind.CLASS, path, new ByteArrayResource(content));
  }

  /**
   * Handle the specified loaded CGLIB class.
   *
   * @param cglibClass a cglib class that has been loaded
   */
  public void handleLoadedClass(Class<?> cglibClass) {
    registerHints(TypeReference.of(cglibClass));
  }

  private void registerHints(TypeReference cglibTypeReference) {
    this.runtimeHints.reflection().registerType(cglibTypeReference, instantiateCglibProxy);
  }

}

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

package cn.taketoday.context.aot;

import java.util.function.Consumer;

import cn.taketoday.aot.generate.GeneratedFiles;
import cn.taketoday.aot.generate.GeneratedFiles.Kind;
import cn.taketoday.aot.generate.GenerationContext;
import cn.taketoday.aot.hint.MemberCategory;
import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.hint.TypeHint.Builder;
import cn.taketoday.aot.hint.TypeReference;
import cn.taketoday.aot.hint.support.ClassHintUtils;
import cn.taketoday.core.io.ByteArrayResource;
import cn.taketoday.util.DefineClassHelper;

/**
 * Handle CGLIB classes by adding them to a {@link GenerationContext},
 * and register the necessary hints so that they can be instantiated.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see DefineClassHelper#setGeneratedClassHandler
 * @see DefineClassHelper#setLoadedClassHandler
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

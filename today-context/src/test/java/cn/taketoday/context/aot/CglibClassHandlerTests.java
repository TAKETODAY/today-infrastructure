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

import org.junit.jupiter.api.Test;

import cn.taketoday.aot.generate.GeneratedFiles.Kind;
import cn.taketoday.aot.generate.InMemoryGeneratedFiles;
import cn.taketoday.aot.hint.MemberCategory;
import cn.taketoday.aot.hint.TypeReference;
import cn.taketoday.aot.hint.predicate.RuntimeHintsPredicates;
import cn.taketoday.aot.test.generate.TestGenerationContext;
import cn.taketoday.core.io.InputStreamSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CglibClassHandler}.
 *
 * @author Stephane Nicoll
 */
class CglibClassHandlerTests {

  private static final byte[] TEST_CONTENT = new byte[] { 'a' };

  private final TestGenerationContext generationContext;

  private final CglibClassHandler handler;

  public CglibClassHandlerTests() {
    this.generationContext = new TestGenerationContext();
    this.handler = new CglibClassHandler(this.generationContext);
  }

  @Test
  void handlerGeneratedClassCreatesRuntimeHintsForProxy() {
    String className = "com.example.Test$$Infra$$0";
    this.handler.handleGeneratedClass(className, TEST_CONTENT);
    assertThat(RuntimeHintsPredicates.reflection().onType(TypeReference.of(className))
            .withMemberCategories(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS))
            .accepts(this.generationContext.getRuntimeHints());
  }

  @Test
  void handlerLoadedClassCreatesRuntimeHintsForProxy() {
    this.handler.handleLoadedClass(CglibClassHandler.class);
    assertThat(RuntimeHintsPredicates.reflection().onType(CglibClassHandler.class)
            .withMemberCategories(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS))
            .accepts(this.generationContext.getRuntimeHints());
  }

  @Test
  void handlerRegisterGeneratedClass() throws IOException {
    String className = "com.example.Test$$Infra$$0";
    this.handler.handleGeneratedClass(className, TEST_CONTENT);
    InMemoryGeneratedFiles generatedFiles = this.generationContext.getGeneratedFiles();
    assertThat(generatedFiles.getGeneratedFiles(Kind.SOURCE)).isEmpty();
    assertThat(generatedFiles.getGeneratedFiles(Kind.RESOURCE)).isEmpty();
    String expectedPath = "com/example/Test$$Infra$$0.class";
    assertThat(generatedFiles.getGeneratedFiles(Kind.CLASS)).containsOnlyKeys(expectedPath);
    assertContent(generatedFiles.getGeneratedFiles(Kind.CLASS).get(expectedPath), TEST_CONTENT);
  }

  private void assertContent(InputStreamSource source, byte[] expectedContent) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    source.getInputStream().transferTo(out);
    assertThat(out.toByteArray()).isEqualTo(expectedContent);
  }

}

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

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import infra.aot.generate.GeneratedFiles.Kind;
import infra.aot.generate.InMemoryGeneratedFiles;
import infra.aot.hint.MemberCategory;
import infra.aot.hint.TypeReference;
import infra.aot.hint.predicate.RuntimeHintsPredicates;
import infra.aot.test.generate.TestGenerationContext;
import infra.core.io.InputStreamSource;

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

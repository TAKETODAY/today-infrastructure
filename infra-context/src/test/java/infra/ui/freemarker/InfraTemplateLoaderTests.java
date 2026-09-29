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

package infra.ui.freemarker;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import infra.core.io.DefaultResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link InfraTemplateLoader}.
 *
 * @author Sébastien Deleuze
 */
class InfraTemplateLoaderTests {

  @Test
  void findTemplateSourceResolvesTemplateInsidePath(@TempDir Path tempDir) throws Exception {
    Path templates = Files.createDirectory(tempDir.resolve("templates"));
    Files.writeString(templates.resolve("hello.ftl"), "Hello");
    InfraTemplateLoader loader = new InfraTemplateLoader(new DefaultResourceLoader(),
            "file:" + templates.toAbsolutePath() + File.separator);
    assertThat(loader.findTemplateSource("hello.ftl")).isNotNull();
  }

  @Test
  void findTemplateSourceRejectsBackslash(@TempDir Path tempDir) throws Exception {
    Path templates = Files.createDirectory(tempDir.resolve("templates"));
    Files.writeString(tempDir.resolve("other.txt"), "other");
    InfraTemplateLoader loader = new InfraTemplateLoader(new DefaultResourceLoader(),
            "file:" + templates.toAbsolutePath() + File.separator);
    assertThat(loader.findTemplateSource("..\\other.txt")).isNull();
  }
}

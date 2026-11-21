/*
 * Copyright 2026-present the original author or authors.
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

package infra.shell.command;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;

import infra.shell.InputReader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link Script}.
 *
 * @author Ezequiel Primon
 */
class ScriptTests {

  @TempDir(cleanup = CleanupMode.ALWAYS)
  private File tempDir;

  private CommandRegistry commandRegistry;

  private StringWriter stringWriter;

  private PrintWriter outputWriter;

  private InputReader inputReader = new InputReader() {
  };

  @BeforeEach
  void setUp() {
    this.commandRegistry = new CommandRegistry();
    Command sayHello = Command.builder()
            .name("say-hello")
            .description("Say hello")
            .group("Test")
            .execute(commandContext -> {
              commandContext.outputWriter().println("Hello World");
            });
    this.commandRegistry.registerCommand(sayHello);
    this.stringWriter = new StringWriter();
    this.outputWriter = new PrintWriter(this.stringWriter);
  }

  @Test
  void testScriptFromClasspathResource() throws Exception {
    // when
    executeScript("classpath:scripts/test-script.txt");

    // then
    assertThat(normalize(this.stringWriter.toString())).isEqualTo("Hello World\nHello World\n");
  }

  @Test
  void testScriptFromAbsoluteFilePath() throws Exception {
    // given
    File scriptFile = new File(this.tempDir, "script.txt");
    Files.writeString(scriptFile.toPath(), "say-hello\n// This is a comment\nsay-hello\n");

    // when
    executeScript(scriptFile.getAbsolutePath());

    // then
    assertThat(normalize(this.stringWriter.toString())).isEqualTo("Hello World\nHello World\n");
  }

  @Test
  void testScriptFromFileUrl() throws Exception {
    // given
    File scriptFile = new File(this.tempDir, "script.txt");
    Files.writeString(scriptFile.toPath(), "say-hello\n// This is a comment\nsay-hello\n");

    // when
    executeScript(scriptFile.toURI().toString());

    // then
    assertThat(normalize(this.stringWriter.toString())).isEqualTo("Hello World\nHello World\n");
  }

  @Test
  void testScriptWithMissingClasspathResource() {
    // when & then
    assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(() -> executeScript("classpath:does-not-exist.txt"));
  }

  @Test
  void testScriptWithUnsupportedReferenceIsRejected() {
    // when & then
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> executeScript("does-not-exist.txt"));
  }

  @Test
  void testScriptWithHttpPrefixIsRejected() {
    // when & then
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> executeScript("https://example.com/script.txt"));
  }

  @Test
  void testScriptWithUrlPrefixIsRejected() {
    // when & then
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> executeScript("url:https://example.com/script.txt"));
  }

  private void executeScript(String scriptRef) throws Exception {
    CommandOption fileOption = CommandOption.with().shortName('f').longName("file").value(scriptRef).build();
    ParsedInput parsedInput = ParsedInput.builder().commandName("script").addOption(fileOption).build();
    CommandContext commandContext = new CommandContext(parsedInput, this.commandRegistry, this.outputWriter,
            this.inputReader);
    Script script = new Script(this.commandRegistry);
    script.execute(commandContext);
    this.outputWriter.flush();
  }

  private String normalize(String output) {
    return output.replaceAll("\\R", "\n");
  }

}

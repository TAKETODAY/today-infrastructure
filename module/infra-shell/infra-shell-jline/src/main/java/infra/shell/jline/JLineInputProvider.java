/*
 * Copyright 2025-present the original author or authors.
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

package infra.shell.jline;

import org.jline.reader.LineReader;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;

import infra.shell.InputProvider;

public class JLineInputProvider implements InputProvider {

  private final LineReader lineReader;

  private PromptProvider promptProvider = () -> new AttributedString("shell:>",
          AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));

  /**
   * Create a new {@link JLineInputProvider} instance.
   *
   * @param lineReader the JLine line reader
   */
  public JLineInputProvider(LineReader lineReader) {
    this.lineReader = lineReader;
  }

  @Override
  public String readInput() {
    try {
      AttributedString prompt = this.promptProvider.getPrompt();
      String ansiPrompt = prompt.toAnsi(this.lineReader.getTerminal());
      return this.lineReader.readLine(ansiPrompt);
    }
    catch (IllegalStateException ex) {
      // Terminal closed externally (e.g. process killed from an IDE): treat as EOF.
      if (ex.getMessage() != null && ex.getMessage().contains("closed")) {
        return null;
      }
      throw ex;
    }
  }

  public void setPromptProvider(PromptProvider promptProvider) {
    this.promptProvider = promptProvider;
  }

  public LineReader getLineReader() {
    return this.lineReader;
  }

}

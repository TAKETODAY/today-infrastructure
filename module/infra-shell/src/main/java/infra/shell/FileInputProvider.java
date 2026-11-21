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

package infra.shell;

import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import infra.util.StringUtils;

/**
 * An {@link InputProvider} that reads input from a file.
 * <p>
 * Supports backslashes at end of line to signal line continuation.
 * </p>
 *
 * @author Eric Bottard
 * @author Piotr Olaszewski
 * @author Mahmoud Ben Hassine
 * @author David Pilar
 * @author Ezequiel Primon
 */
public class FileInputProvider implements InputProvider, AutoCloseable {

  private static final String BACKSLASH_AT_EOL_REGEX = "(.*)\\\\\\s*$";

  private final BufferedReader reader;

  /**
   * Create a new {@link FileInputProvider} instance.
   *
   * @param file the file to read input from
   * @throws FileNotFoundException if the file does not exist
   */
  public FileInputProvider(File file) throws FileNotFoundException {
    this.reader = new BufferedReader(new FileReader(file));
  }

  /**
   * Create a new {@link FileInputProvider} instance.
   *
   * @param reader the reader to read input from
   * @since 4.0.4
   */
  public FileInputProvider(Reader reader) {
    this.reader = new BufferedReader(reader);
  }

  @Override
  @Nullable
  public String readInput() throws IOException {
    StringBuilder sb = new StringBuilder();
    boolean continued;
    String line;
    do {
      line = reader.readLine();
      if (line == null) {
        break;
      }
      continued = line.matches(BACKSLASH_AT_EOL_REGEX);
      sb.append(line.replaceFirst(BACKSLASH_AT_EOL_REGEX, "$1 "));
    }
    while (continued);
    if (line == null) {
      return null;
    }
    else if (!StringUtils.isNotEmpty(line) || isComment(line)) {
      return readInput();
    }
    else {
      return sb.toString();
    }
  }

  @Override
  public void close() throws IOException {
    reader.close();
  }

  private boolean isComment(String line) {
    return line.matches("\\s*//.*");
  }

}

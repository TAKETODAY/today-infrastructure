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

package infra.jdbc.datasource.embedded;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Internal helper for exposing dummy OutputStreams to embedded databases
 * such as Derby, preventing the creation of a log file.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
public final class OutputStreamFactory {

  private OutputStreamFactory() { }

  /**
   * Returns an {@link OutputStream} that ignores all data given to it.
   */
  public static OutputStream getNoopOutputStream() {
    return new OutputStream() {
      @Override
      public void write(int b) throws IOException {
        // ignore the output
      }
    };
  }

}

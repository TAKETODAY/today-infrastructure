/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.test.web.mock.result;

import org.jspecify.annotations.Nullable;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.test.web.mock.MvcResult;
import infra.test.web.mock.ResultHandler;
import infra.util.CollectionUtils;

/**
 * Static factory methods for {@link ResultHandler}-based result actions.
 *
 * <h3>Eclipse Users</h3>
 * <p>Consider adding this class as a Java editor favorite. To navigate to
 * this setting, open the Preferences and type "favorites".
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class MockMvcResultHandlers {

  private static final Logger logger = LoggerFactory.getLogger("infra.test.web.servlet.result");

  /**
   * Log {@link MvcResult} details as a {@code DEBUG} log message via
   * Apache Commons Logging using the log category
   * {@code infra.test.web.servlet.result}.
   *
   * @see #print()
   * @see #print(OutputStream)
   * @see #print(Writer)
   */
  public static ResultHandler log() {
    return new LoggingResultHandler();
  }

  /**
   * Print {@link MvcResult} details to the "standard" output stream.
   *
   * @see System#out
   * @see #print(OutputStream)
   * @see #print(Writer)
   * @see #log()
   */
  public static ResultHandler print() {
    return print(System.out);
  }

  /**
   * Print {@link MvcResult} details to the supplied {@link OutputStream}.
   *
   * @see #print()
   * @see #print(Writer)
   * @see #log()
   */
  public static ResultHandler print(OutputStream stream) {
    return new PrintWriterPrintingResultHandler(new PrintWriter(stream, true));
  }

  /**
   * Print {@link MvcResult} details to the supplied {@link Writer}.
   *
   * @see #print()
   * @see #print(OutputStream)
   * @see #log()
   */
  public static ResultHandler print(Writer writer) {
    return new PrintWriterPrintingResultHandler(new PrintWriter(writer, true));
  }

  /**
   * A {@link PrintingResultHandler} that writes to a {@link PrintWriter}.
   */
  private static class PrintWriterPrintingResultHandler extends PrintingResultHandler {

    public PrintWriterPrintingResultHandler(PrintWriter writer) {
      super(new ResultValuePrinter() {
        @Override
        public void printHeading(String heading) {
          writer.println();
          writer.println(String.format("%s:", heading));
        }

        @Override
        public void printValue(String label, @Nullable Object value) {
          if (value != null && value.getClass().isArray()) {
            value = CollectionUtils.arrayToList(value);
          }
          writer.println(String.format("%17s = %s", label, value));
        }
      });
    }
  }

  /**
   * A {@link ResultHandler} that logs {@link MvcResult} details at
   * {@code DEBUG} level via Apache Commons Logging.
   *
   * <p>Delegates to a {@link PrintWriterPrintingResultHandler} for
   * building the log message.
   */
  private static final class LoggingResultHandler implements ResultHandler {

    @Override
    public void handle(MvcResult result) throws Exception {
      if (logger.isDebugEnabled()) {
        StringWriter stringWriter = new StringWriter();
        ResultHandler printingResultHandler =
                new PrintWriterPrintingResultHandler(new PrintWriter(stringWriter));
        printingResultHandler.handle(result);
        logger.debug("MvcResult details:\n" + stringWriter);
      }
    }
  }

}

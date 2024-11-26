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

package infra.app.test.system;

/**
 * Provides access to {@link System#out System.out} and {@link System#err System.err}
 * output that has been captured by the {@link OutputCaptureExtension} or
 * {@link OutputCaptureRule}. Can be used to apply assertions either using AssertJ or
 * standard JUnit assertions. For example: <pre class="code">
 * assertThat(output).contains("started"); // Checks all output
 * assertThat(output.getErr()).contains("failed"); // Only checks System.err
 * assertThat(output.getOut()).contains("ok"); // Only checks System.out
 * </pre>
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @see OutputCaptureExtension
 * @since 4.0
 */
public interface CapturedOutput extends CharSequence {

  @Override
  default int length() {
    return toString().length();
  }

  @Override
  default char charAt(int index) {
    return toString().charAt(index);
  }

  @Override
  default CharSequence subSequence(int start, int end) {
    return toString().subSequence(start, end);
  }

  /**
   * Return all content (both {@link System#out System.out} and {@link System#err
   * System.err}) in the order that it was captured.
   *
   * @return all captured output
   */
  String getAll();

  /**
   * Return {@link System#out System.out} content in the order that it was captured.
   *
   * @return {@link System#out System.out} captured output
   */
  String getOut();

  /**
   * Return {@link System#err System.err} content in the order that it was captured.
   *
   * @return {@link System#err System.err} captured output
   */
  String getErr();

}

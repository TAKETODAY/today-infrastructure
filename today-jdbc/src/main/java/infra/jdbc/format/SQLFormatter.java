/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.


package infra.jdbc.format;

/**
 * Formatter contract
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Steve Ebersole
 * @since 4.0 2022/9/12 19:20
 */
public interface SQLFormatter {

  String WHITESPACE = " \n\r\f\t";

  /**
   * Format the source SQL string.
   *
   * @param source The original SQL string
   * @return The formatted version
   */
  String format(String source);
}

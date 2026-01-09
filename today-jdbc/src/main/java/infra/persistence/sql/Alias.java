/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.persistence.sql;

import infra.persistence.platform.Platform;

/**
 * An alias generator for SQL identifiers
 *
 * @author Gavin King
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public final class Alias {

  private final int length;

  private final String suffix;

  /**
   * Constructor for Alias.
   */
  public Alias(int length, String suffix) {
    this.length = (suffix == null) ? length : length - suffix.length();
    this.suffix = suffix;
  }

  /**
   * Constructor for Alias.
   */
  public Alias(String suffix) {
    this.length = Integer.MAX_VALUE;
    this.suffix = suffix;
  }

  public String toAliasString(String sqlIdentifier) {
    char begin = sqlIdentifier.charAt(0);
    int quoteType = Platform.QUOTE.indexOf(begin);
    String unquoted = getUnquotedAliasString(sqlIdentifier, quoteType);
    if (quoteType >= 0) {
      char endQuote = Platform.CLOSED_QUOTE.charAt(quoteType);
      return begin + unquoted + endQuote;
    }
    else {
      return unquoted;
    }
  }

  public String toUnquotedAliasString(String sqlIdentifier) {
    return getUnquotedAliasString(sqlIdentifier);
  }

  private String getUnquotedAliasString(String sqlIdentifier) {
    char begin = sqlIdentifier.charAt(0);
    int quoteType = Platform.QUOTE.indexOf(begin);
    return getUnquotedAliasString(sqlIdentifier, quoteType);
  }

  private String getUnquotedAliasString(String sqlIdentifier, int quoteType) {
    String unquoted = sqlIdentifier;
    if (quoteType >= 0) {
      //if the identifier is quoted, remove the quotes
      unquoted = unquoted.substring(1, unquoted.length() - 1);
    }
    if (unquoted.length() > length) {
      //truncate the identifier to the max alias length, less the suffix length
      unquoted = unquoted.substring(0, length);
    }
    return (suffix == null) ? unquoted : unquoted + suffix;
  }

  public String[] toUnquotedAliasStrings(String[] sqlIdentifiers) {
    String[] aliases = new String[sqlIdentifiers.length];
    for (int i = 0; i < sqlIdentifiers.length; i++) {
      aliases[i] = toUnquotedAliasString(sqlIdentifiers[i]);
    }
    return aliases;
  }

  public String[] toAliasStrings(String[] sqlIdentifiers) {
    String[] aliases = new String[sqlIdentifiers.length];
    for (int i = 0; i < sqlIdentifiers.length; i++) {
      aliases[i] = toAliasString(sqlIdentifiers[i]);
    }
    return aliases;
  }

}

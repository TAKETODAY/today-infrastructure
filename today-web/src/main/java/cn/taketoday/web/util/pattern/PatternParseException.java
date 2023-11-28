/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.web.util.pattern;

import java.text.MessageFormat;

/**
 * Exception that is thrown when there is a problem with the pattern being parsed.
 *
 * @author Andy Clement
 * @since 4.0
 */
@SuppressWarnings("serial")
public class PatternParseException extends IllegalArgumentException {

  private final int position;

  private final char[] pattern;

  private final PatternMessage messageType;

  private final Object[] inserts;

  PatternParseException(int pos, char[] pattern, PatternMessage messageType, Object... inserts) {
    super(messageType.formatMessage(inserts));
    this.position = pos;
    this.pattern = pattern;
    this.messageType = messageType;
    this.inserts = inserts;
  }

  PatternParseException(Throwable cause, int pos, char[] pattern, PatternMessage messageType, Object... inserts) {
    super(messageType.formatMessage(inserts), cause);
    this.position = pos;
    this.pattern = pattern;
    this.messageType = messageType;
    this.inserts = inserts;
  }

  /**
   * Return a formatted message with inserts applied.
   */
  @Override
  public String getMessage() {
    return this.messageType.formatMessage(this.inserts);
  }

  /**
   * Return a detailed message that includes the original pattern text
   * with a pointer to the error position, as well as the error message.
   */
  public String toDetailedString() {
    StringBuilder sb = new StringBuilder();
    sb.append(this.pattern).append('\n');
    sb.append(" ".repeat(Math.max(0, this.position)));
    sb.append("^\n");
    sb.append(getMessage());
    return sb.toString();
  }

  public int getPosition() {
    return this.position;
  }

  public PatternMessage getMessageType() {
    return this.messageType;
  }

  public Object[] getInserts() {
    return this.inserts;
  }

  /**
   * The messages that can be included in a {@link PatternParseException} when there is a parse failure.
   */
  public enum PatternMessage {

    MISSING_CLOSE_CAPTURE("Expected close capture character after variable name '}'"),
    MISSING_OPEN_CAPTURE("Missing preceding open capture character before variable name'{'"),
    ILLEGAL_NESTED_CAPTURE("Not allowed to nest variable captures"),
    CANNOT_HAVE_ADJACENT_CAPTURES("Adjacent captures are not allowed"),
    ILLEGAL_CHARACTER_AT_START_OF_CAPTURE_DESCRIPTOR("Char ''{0}'' not allowed at start of captured variable name"),
    ILLEGAL_CHARACTER_IN_CAPTURE_DESCRIPTOR("Char ''{0}'' is not allowed in a captured variable name"),
    NO_MORE_DATA_EXPECTED_AFTER_CAPTURE_THE_REST("No more pattern data allowed after '{*...}' or '**' pattern element"),
    BADLY_FORMED_CAPTURE_THE_REST("Expected form when capturing the rest of the path is simply '{*...}'"),
    MISSING_REGEX_CONSTRAINT("Missing regex constraint on capture"),
    ILLEGAL_DOUBLE_CAPTURE("Not allowed to capture ''{0}'' twice in the same pattern"),
    REGEX_PATTERN_SYNTAX_EXCEPTION("Exception occurred in regex pattern compilation"),
    CAPTURE_ALL_IS_STANDALONE_CONSTRUCT("'{*...}' can only be preceded by a path separator");

    private final String message;

    PatternMessage(String message) {
      this.message = message;
    }

    public String formatMessage(Object... inserts) {
      return MessageFormat.format(this.message, inserts);
    }
  }

}

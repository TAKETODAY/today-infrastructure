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

package infra.expression.spel;

import infra.expression.ParseException;
import infra.lang.Nullable;

/**
 * Root exception for Spring EL related exceptions. Rather than holding a hard coded
 * string indicating the problem, it records a message key and the inserts for the
 * message. See {@link SpelMessage} for the list of all possible messages that can occur.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
@SuppressWarnings("serial")
public class SpelParseException extends ParseException {

  private final SpelMessage message;

  private final Object[] inserts;

  public SpelParseException(@Nullable String expressionString, int position, SpelMessage message, Object... inserts) {
    super(expressionString, position, message.formatMessage(inserts));
    this.message = message;
    this.inserts = inserts;
  }

  public SpelParseException(int position, Throwable cause, SpelMessage message, Object... inserts) {
    super(position, message.formatMessage(inserts), cause);
    this.message = message;
    this.inserts = inserts;
  }

  /**
   * Return the message code.
   */
  public SpelMessage getMessageCode() {
    return this.message;
  }

  /**
   * Return the message inserts.
   */
  public Object[] getInserts() {
    return this.inserts;
  }

}

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

package cn.taketoday.expression.spel;

import cn.taketoday.expression.EvaluationException;

/**
 * Root exception for SpEL related exceptions.
 *
 * <p>Rather than holding a hard-coded string indicating the problem, it records
 * a message key and the inserts for the message.
 *
 * <p>See {@link SpelMessage} for the list of all possible messages that can occur.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@SuppressWarnings("serial")
public class SpelEvaluationException extends EvaluationException {

  private final SpelMessage message;

  private final Object[] inserts;

  public SpelEvaluationException(SpelMessage message, Object... inserts) {
    super(message.formatMessage(inserts));
    this.message = message;
    this.inserts = inserts;
  }

  public SpelEvaluationException(int position, SpelMessage message, Object... inserts) {
    super(position, message.formatMessage(inserts));
    this.message = message;
    this.inserts = inserts;
  }

  public SpelEvaluationException(int position, Throwable cause, SpelMessage message, Object... inserts) {
    super(position, message.formatMessage(inserts), cause);
    this.message = message;
    this.inserts = inserts;
  }

  public SpelEvaluationException(Throwable cause, SpelMessage message, Object... inserts) {
    super(message.formatMessage(inserts), cause);
    this.message = message;
    this.inserts = inserts;
  }

  /**
   * Set the position in the related expression which gave rise to this exception.
   */
  public void setPosition(int position) {
    this.position = position;
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

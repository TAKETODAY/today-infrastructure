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

package infra.mail;

/**
 * Exception thrown if illegal message properties are encountered.
 *
 * @author Dmitriy Kopylenko
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class MailParseException extends MailException {

  /**
   * Constructor for MailParseException.
   *
   * @param msg the detail message
   */
  public MailParseException(String msg) {
    super(msg);
  }

  /**
   * Constructor for MailParseException.
   *
   * @param msg the detail message
   * @param cause the root cause from the mail API in use
   */
  public MailParseException(String msg, Throwable cause) {
    super(msg, cause);
  }

  /**
   * Constructor for MailParseException.
   *
   * @param cause the root cause from the mail API in use
   */
  public MailParseException(Throwable cause) {
    super("Could not parse mail", cause);
  }

}

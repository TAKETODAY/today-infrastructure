/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.mail;

import cn.taketoday.mail.javamail.JavaMailSender;

/**
 * This interface defines a strategy for sending simple mails. Can be
 * implemented for a variety of mailing systems due to the simple requirements.
 * For richer functionality like MIME messages, consider JavaMailSender.
 *
 * <p>Allows for easy testing of clients, as it does not depend on JavaMail's
 * infrastructure classes: no mocking of JavaMail Session or Transport necessary.
 *
 * @author Dmitriy Kopylenko
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see JavaMailSender
 * @since 4.0
 */
public interface MailSender {

  /**
   * Send the given simple mail message.
   *
   * @param simpleMessage the message to send
   * @throws MailParseException in case of failure when parsing the message
   * @throws MailAuthenticationException in case of authentication failure
   * @throws MailSendException in case of failure when sending the message
   */
  default void send(SimpleMailMessage simpleMessage) throws MailException {
    send(new SimpleMailMessage[] { simpleMessage });
  }

  /**
   * Send the given array of simple mail messages in batch.
   *
   * @param simpleMessages the messages to send
   * @throws MailParseException in case of failure when parsing a message
   * @throws MailAuthenticationException in case of authentication failure
   * @throws MailSendException in case of failure when sending a message
   */
  void send(SimpleMailMessage... simpleMessages) throws MailException;

}

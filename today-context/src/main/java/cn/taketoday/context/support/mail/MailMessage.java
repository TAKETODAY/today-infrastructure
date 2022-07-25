/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.context.support.mail;

import java.util.Date;

/**
 * This is a common interface for mail messages, allowing a user to set key
 * values required in assembling a mail message, without needing to know if
 * the underlying message is a simple text message or a more sophisticated
 * MIME message.
 *
 * <p>Implemented by both SimpleMailMessage and MimeMessageHelper,
 * to let message population code interact with a simple message or a
 * MIME message through a common interface.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see SimpleMailMessage
 * @see cn.taketoday.context.support.mail.javamail.MimeMessageHelper
 * @since 4.0
 */
public interface MailMessage {

  void setFrom(String from) throws MailParseException;

  void setReplyTo(String replyTo) throws MailParseException;

  void setTo(String to) throws MailParseException;

  void setTo(String... to) throws MailParseException;

  void setCc(String cc) throws MailParseException;

  void setCc(String... cc) throws MailParseException;

  void setBcc(String bcc) throws MailParseException;

  void setBcc(String... bcc) throws MailParseException;

  void setSentDate(Date sentDate) throws MailParseException;

  void setSubject(String subject) throws MailParseException;

  void setText(String text) throws MailParseException;

}

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

package cn.taketoday.context.support.mail.javamail;

import java.util.Date;

import cn.taketoday.context.support.mail.MailMessage;
import cn.taketoday.context.support.mail.MailParseException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Implementation of the MailMessage interface for a JavaMail MIME message,
 * to let message population code interact with a simple message or a MIME
 * message through a common interface.
 *
 * <p>Uses a MimeMessageHelper underneath. Can either be created with a
 * MimeMessageHelper instance or with a JavaMail MimeMessage instance.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see MimeMessageHelper
 * @see MimeMessage
 * @since 4.0
 */
public class MimeMailMessage implements MailMessage {

  private final MimeMessageHelper helper;

  /**
   * Create a new MimeMailMessage based on the given MimeMessageHelper.
   *
   * @param mimeMessageHelper the MimeMessageHelper
   */
  public MimeMailMessage(MimeMessageHelper mimeMessageHelper) {
    this.helper = mimeMessageHelper;
  }

  /**
   * Create a new MimeMailMessage based on the given JavaMail MimeMessage.
   *
   * @param mimeMessage the JavaMail MimeMessage
   */
  public MimeMailMessage(MimeMessage mimeMessage) {
    this.helper = new MimeMessageHelper(mimeMessage);
  }

  /**
   * Return the MimeMessageHelper that this MimeMailMessage is based on.
   */
  public final MimeMessageHelper getMimeMessageHelper() {
    return this.helper;
  }

  /**
   * Return the JavaMail MimeMessage that this MimeMailMessage is based on.
   */
  public final MimeMessage getMimeMessage() {
    return this.helper.getMimeMessage();
  }

  @Override
  public void setFrom(String from) throws MailParseException {
    try {
      this.helper.setFrom(from);
    }
    catch (MessagingException ex) {
      throw new MailParseException(ex);
    }
  }

  @Override
  public void setReplyTo(String replyTo) throws MailParseException {
    try {
      this.helper.setReplyTo(replyTo);
    }
    catch (MessagingException ex) {
      throw new MailParseException(ex);
    }
  }

  @Override
  public void setTo(String to) throws MailParseException {
    try {
      this.helper.setTo(to);
    }
    catch (MessagingException ex) {
      throw new MailParseException(ex);
    }
  }

  @Override
  public void setTo(String... to) throws MailParseException {
    try {
      this.helper.setTo(to);
    }
    catch (MessagingException ex) {
      throw new MailParseException(ex);
    }
  }

  @Override
  public void setCc(String cc) throws MailParseException {
    try {
      this.helper.setCc(cc);
    }
    catch (MessagingException ex) {
      throw new MailParseException(ex);
    }
  }

  @Override
  public void setCc(String... cc) throws MailParseException {
    try {
      this.helper.setCc(cc);
    }
    catch (MessagingException ex) {
      throw new MailParseException(ex);
    }
  }

  @Override
  public void setBcc(String bcc) throws MailParseException {
    try {
      this.helper.setBcc(bcc);
    }
    catch (MessagingException ex) {
      throw new MailParseException(ex);
    }
  }

  @Override
  public void setBcc(String... bcc) throws MailParseException {
    try {
      this.helper.setBcc(bcc);
    }
    catch (MessagingException ex) {
      throw new MailParseException(ex);
    }
  }

  @Override
  public void setSentDate(Date sentDate) throws MailParseException {
    try {
      this.helper.setSentDate(sentDate);
    }
    catch (MessagingException ex) {
      throw new MailParseException(ex);
    }
  }

  @Override
  public void setSubject(String subject) throws MailParseException {
    try {
      this.helper.setSubject(subject);
    }
    catch (MessagingException ex) {
      throw new MailParseException(ex);
    }
  }

  @Override
  public void setText(String text) throws MailParseException {
    try {
      this.helper.setText(text);
    }
    catch (MessagingException ex) {
      throw new MailParseException(ex);
    }
  }

}

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

import java.io.Serializable;
import java.util.Date;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.mail.javamail.JavaMailSender;
import cn.taketoday.mail.javamail.MimeMailMessage;
import cn.taketoday.mail.javamail.MimeMessageHelper;
import cn.taketoday.mail.javamail.MimeMessagePreparator;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

/**
 * Models a simple mail message, including data such as the from, to, cc, subject,
 * and text fields.
 *
 * <p>Consider {@code JavaMailSender} and JavaMail {@code MimeMessages} for creating
 * more sophisticated messages, for example messages with attachments, special
 * character encodings, or personal names that accompany mail addresses.
 *
 * @author Dmitriy Kopylenko
 * @author Juergen Hoeller
 * @see MailSender
 * @see JavaMailSender
 * @see MimeMessagePreparator
 * @see MimeMessageHelper
 * @see MimeMailMessage
 * @since 4.0
 */
@SuppressWarnings("serial")
public class SimpleMailMessage implements MailMessage, Serializable {

  @Nullable
  private String from;

  @Nullable
  private String replyTo;

  @Nullable
  private String[] to;

  @Nullable
  private String[] cc;

  @Nullable
  private String[] bcc;

  @Nullable
  private Date sentDate;

  @Nullable
  private String subject;

  @Nullable
  private String text;

  /**
   * Create a new {@code SimpleMailMessage}.
   */
  public SimpleMailMessage() {
  }

  /**
   * Copy constructor for creating a new {@code SimpleMailMessage} from the state
   * of an existing {@code SimpleMailMessage} instance.
   */
  public SimpleMailMessage(SimpleMailMessage original) {
    Assert.notNull(original, "'original' message argument must not be null");
    this.from = original.getFrom();
    this.replyTo = original.getReplyTo();
    this.to = copyOrNull(original.getTo());
    this.cc = copyOrNull(original.getCc());
    this.bcc = copyOrNull(original.getBcc());
    this.sentDate = original.getSentDate();
    this.subject = original.getSubject();
    this.text = original.getText();
  }

  @Override
  public void setFrom(String from) {
    this.from = from;
  }

  @Nullable
  public String getFrom() {
    return this.from;
  }

  @Override
  public void setReplyTo(String replyTo) {
    this.replyTo = replyTo;
  }

  @Nullable
  public String getReplyTo() {
    return this.replyTo;
  }

  @Override
  public void setTo(String to) {
    this.to = new String[] { to };
  }

  @Override
  public void setTo(String... to) {
    this.to = to;
  }

  @Nullable
  public String[] getTo() {
    return this.to;
  }

  @Override
  public void setCc(String cc) {
    this.cc = new String[] { cc };
  }

  @Override
  public void setCc(String... cc) {
    this.cc = cc;
  }

  @Nullable
  public String[] getCc() {
    return this.cc;
  }

  @Override
  public void setBcc(String bcc) {
    this.bcc = new String[] { bcc };
  }

  @Override
  public void setBcc(String... bcc) {
    this.bcc = bcc;
  }

  @Nullable
  public String[] getBcc() {
    return this.bcc;
  }

  @Override
  public void setSentDate(Date sentDate) {
    this.sentDate = sentDate;
  }

  @Nullable
  public Date getSentDate() {
    return this.sentDate;
  }

  @Override
  public void setSubject(String subject) {
    this.subject = subject;
  }

  @Nullable
  public String getSubject() {
    return this.subject;
  }

  @Override
  public void setText(String text) {
    this.text = text;
  }

  @Nullable
  public String getText() {
    return this.text;
  }

  /**
   * Copy the contents of this message to the given target message.
   *
   * @param target the {@code MailMessage} to copy to
   */
  public void copyTo(MailMessage target) {
    Assert.notNull(target, "'target' MailMessage must not be null");
    if (getFrom() != null) {
      target.setFrom(getFrom());
    }
    if (getReplyTo() != null) {
      target.setReplyTo(getReplyTo());
    }
    if (getTo() != null) {
      target.setTo(copy(getTo()));
    }
    if (getCc() != null) {
      target.setCc(copy(getCc()));
    }
    if (getBcc() != null) {
      target.setBcc(copy(getBcc()));
    }
    if (getSentDate() != null) {
      target.setSentDate(getSentDate());
    }
    if (getSubject() != null) {
      target.setSubject(getSubject());
    }
    if (getText() != null) {
      target.setText(getText());
    }
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof SimpleMailMessage otherMessage)) {
      return false;
    }
    return (ObjectUtils.nullSafeEquals(this.from, otherMessage.from) &&
            ObjectUtils.nullSafeEquals(this.replyTo, otherMessage.replyTo) &&
            ObjectUtils.nullSafeEquals(this.to, otherMessage.to) &&
            ObjectUtils.nullSafeEquals(this.cc, otherMessage.cc) &&
            ObjectUtils.nullSafeEquals(this.bcc, otherMessage.bcc) &&
            ObjectUtils.nullSafeEquals(this.sentDate, otherMessage.sentDate) &&
            ObjectUtils.nullSafeEquals(this.subject, otherMessage.subject) &&
            ObjectUtils.nullSafeEquals(this.text, otherMessage.text));
  }

  @Override
  public int hashCode() {
    return ObjectUtils.nullSafeHash(this.from, this.replyTo, this.to, this.cc,
            this.bcc, this.sentDate, this.subject);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("SimpleMailMessage: ");
    sb.append("from=").append(this.from).append("; ");
    sb.append("replyTo=").append(this.replyTo).append("; ");
    sb.append("to=").append(StringUtils.arrayToCommaDelimitedString(this.to)).append("; ");
    sb.append("cc=").append(StringUtils.arrayToCommaDelimitedString(this.cc)).append("; ");
    sb.append("bcc=").append(StringUtils.arrayToCommaDelimitedString(this.bcc)).append("; ");
    sb.append("sentDate=").append(this.sentDate).append("; ");
    sb.append("subject=").append(this.subject).append("; ");
    sb.append("text=").append(this.text);
    return sb.toString();
  }

  @Nullable
  private static String[] copyOrNull(@Nullable String[] state) {
    if (state == null) {
      return null;
    }
    return copy(state);
  }

  private static String[] copy(String[] state) {
    return state.clone();
  }

}

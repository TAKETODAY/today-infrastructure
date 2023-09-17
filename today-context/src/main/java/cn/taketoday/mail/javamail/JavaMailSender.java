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

package cn.taketoday.mail.javamail;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import cn.taketoday.mail.MailAuthenticationException;
import cn.taketoday.mail.MailException;
import cn.taketoday.mail.MailParseException;
import cn.taketoday.mail.MailPreparationException;
import cn.taketoday.mail.MailSendException;
import cn.taketoday.mail.MailSender;
import cn.taketoday.mail.SimpleMailMessage;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Extended {@link MailSender} interface for JavaMail,
 * supporting MIME messages both as direct arguments and through preparation
 * callbacks. Typically used in conjunction with the {@link MimeMessageHelper}
 * class for convenient creation of JavaMail {@link MimeMessage MimeMessages},
 * including attachments etc.
 *
 * <p>Clients should talk to the mail sender through this interface if they need
 * mail functionality beyond {@link SimpleMailMessage}.
 * The production implementation is {@link JavaMailSenderImpl}; for testing,
 * mocks can be created based on this interface. Clients will typically receive
 * the JavaMailSender reference through dependency injection.
 *
 * <p>The recommended way of using this interface is the {@link MimeMessagePreparator}
 * mechanism, possibly using a {@link MimeMessageHelper} for populating the message.
 * See {@link MimeMessageHelper MimeMessageHelper's javadoc} for an example.
 *
 * <p>The entire JavaMail {@link jakarta.mail.Session} management is abstracted
 * by the JavaMailSender. Client code should not deal with a Session in any way,
 * rather leave the entire JavaMail configuration and resource handling to the
 * JavaMailSender implementation. This also increases testability.
 *
 * <p>A JavaMailSender client is not as easy to test as a plain
 * {@link MailSender} client, but still straightforward
 * compared to traditional JavaMail code: Just let {@link #createMimeMessage()}
 * return a plain {@link MimeMessage} created with a
 * {@code Session.getInstance(new Properties())} call, and check the passed-in
 * messages in your mock implementations of the various {@code send} methods.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see MimeMessage
 * @see jakarta.mail.Session
 * @see JavaMailSenderImpl
 * @see MimeMessagePreparator
 * @see MimeMessageHelper
 * @since 4.0
 */
public interface JavaMailSender extends MailSender {

  /**
   * Create a new JavaMail MimeMessage for the underlying JavaMail Session
   * of this sender. Needs to be called to create MimeMessage instances
   * that can be prepared by the client and passed to send(MimeMessage).
   *
   * @return the new MimeMessage instance
   * @see #send(MimeMessage)
   * @see #send(MimeMessage[])
   */
  MimeMessage createMimeMessage();

  /**
   * Create a new JavaMail MimeMessage for the underlying JavaMail Session
   * of this sender, using the given input stream as the message source.
   *
   * @param contentStream the raw MIME input stream for the message
   * @return the new MimeMessage instance
   * @throws MailParseException in case of message creation failure
   */
  MimeMessage createMimeMessage(InputStream contentStream) throws MailException;

  /**
   * Send the given JavaMail MIME message.
   * The message needs to have been created with {@link #createMimeMessage()}.
   *
   * @param mimeMessage message to send
   * @throws MailAuthenticationException in case of authentication failure
   * @throws MailSendException in case of failure when sending the message
   * @see #createMimeMessage
   */
  default void send(MimeMessage mimeMessage) throws MailException {
    send(new MimeMessage[] { mimeMessage });
  }

  /**
   * Send the given array of JavaMail MIME messages in batch.
   * The messages need to have been created with {@link #createMimeMessage()}.
   *
   * @param mimeMessages messages to send
   * @throws MailAuthenticationException in case of authentication failure
   * @throws MailSendException in case of failure when sending a message
   * @see #createMimeMessage
   */
  void send(MimeMessage... mimeMessages) throws MailException;

  /**
   * Send the JavaMail MIME message prepared by the given MimeMessagePreparator.
   * <p>Alternative way to prepare MimeMessage instances, instead of
   * {@link #createMimeMessage()} and {@link #send(MimeMessage)} calls.
   * Takes care of proper exception conversion.
   *
   * @param mimeMessagePreparator the preparator to use
   * @throws MailPreparationException in case of failure when preparing the message
   * @throws MailParseException in case of failure when parsing the message
   * @throws MailAuthenticationException in case of authentication failure
   * @throws MailSendException in case of failure when sending the message
   */
  default void send(MimeMessagePreparator mimeMessagePreparator) throws MailException {
    send(new MimeMessagePreparator[] { mimeMessagePreparator });
  }

  /**
   * Send the JavaMail MIME messages prepared by the given MimeMessagePreparators.
   * <p>Alternative way to prepare MimeMessage instances, instead of
   * {@link #createMimeMessage()} and {@link #send(MimeMessage[])} calls.
   * Takes care of proper exception conversion.
   *
   * @param mimeMessagePreparators the preparator to use
   * @throws MailPreparationException in case of failure when preparing a message
   * @throws MailParseException in case of failure when parsing a message
   * @throws MailAuthenticationException in case of authentication failure
   * @throws MailSendException in case of failure when sending a message
   */
  default void send(MimeMessagePreparator... mimeMessagePreparators) throws MailException {
    try {
      List<MimeMessage> mimeMessages = new ArrayList<>(mimeMessagePreparators.length);
      for (MimeMessagePreparator preparator : mimeMessagePreparators) {
        MimeMessage mimeMessage = createMimeMessage();
        preparator.prepare(mimeMessage);
        mimeMessages.add(mimeMessage);
      }
      send(mimeMessages.toArray(new MimeMessage[0]));
    }
    catch (MailException ex) {
      throw ex;
    }
    catch (MessagingException ex) {
      throw new MailParseException(ex);
    }
    catch (Exception ex) {
      throw new MailPreparationException(ex);
    }
  }

}

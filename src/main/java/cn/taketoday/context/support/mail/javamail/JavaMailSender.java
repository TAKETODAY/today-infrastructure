/*
 * Copyright 2002-2014 the original author or authors.
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

package cn.taketoday.context.support.mail.javamail;

import java.io.InputStream;

import cn.taketoday.context.support.mail.MailException;
import cn.taketoday.context.support.mail.MailSender;
import jakarta.mail.internet.MimeMessage;

/**
 * Extended {@link cn.taketoday.context.support.mail.MailSender} interface for JavaMail,
 * supporting MIME messages both as direct arguments and through preparation
 * callbacks. Typically used in conjunction with the {@link MimeMessageHelper}
 * class for convenient creation of JavaMail {@link MimeMessage MimeMessages},
 * including attachments etc.
 *
 * <p>Clients should talk to the mail sender through this interface if they need
 * mail functionality beyond {@link cn.taketoday.context.support.mail.SimpleMailMessage}.
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
 * {@link cn.taketoday.context.support.mail.MailSender} client, but still straightforward
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
   * @throws cn.taketoday.context.support.mail.MailParseException in case of message creation failure
   */
  MimeMessage createMimeMessage(InputStream contentStream) throws MailException;

  /**
   * Send the given JavaMail MIME message.
   * The message needs to have been created with {@link #createMimeMessage()}.
   *
   * @param mimeMessage message to send
   * @throws cn.taketoday.context.support.mail.MailAuthenticationException in case of authentication failure
   * @throws cn.taketoday.context.support.mail.MailSendException in case of failure when sending the message
   * @see #createMimeMessage
   */
  void send(MimeMessage mimeMessage) throws MailException;

  /**
   * Send the given array of JavaMail MIME messages in batch.
   * The messages need to have been created with {@link #createMimeMessage()}.
   *
   * @param mimeMessages messages to send
   * @throws cn.taketoday.context.support.mail.MailAuthenticationException in case of authentication failure
   * @throws cn.taketoday.context.support.mail.MailSendException in case of failure when sending a message
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
   * @throws cn.taketoday.context.support.mail.MailPreparationException in case of failure when preparing the message
   * @throws cn.taketoday.context.support.mail.MailParseException in case of failure when parsing the message
   * @throws cn.taketoday.context.support.mail.MailAuthenticationException in case of authentication failure
   * @throws cn.taketoday.context.support.mail.MailSendException in case of failure when sending the message
   */
  void send(MimeMessagePreparator mimeMessagePreparator) throws MailException;

  /**
   * Send the JavaMail MIME messages prepared by the given MimeMessagePreparators.
   * <p>Alternative way to prepare MimeMessage instances, instead of
   * {@link #createMimeMessage()} and {@link #send(MimeMessage[])} calls.
   * Takes care of proper exception conversion.
   *
   * @param mimeMessagePreparators the preparator to use
   * @throws cn.taketoday.context.support.mail.MailPreparationException in case of failure when preparing a message
   * @throws cn.taketoday.context.support.mail.MailParseException in case of failure when parsing a message
   * @throws cn.taketoday.context.support.mail.MailAuthenticationException in case of authentication failure
   * @throws cn.taketoday.context.support.mail.MailSendException in case of failure when sending a message
   */
  void send(MimeMessagePreparator... mimeMessagePreparators) throws MailException;

}

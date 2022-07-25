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

import jakarta.mail.internet.MimeMessage;

/**
 * Callback interface for the preparation of JavaMail MIME messages.
 *
 * <p>The corresponding {@code send} methods of {@link JavaMailSender}
 * will take care of the actual creation of a {@link MimeMessage} instance,
 * and of proper exception conversion.
 *
 * <p>It is often convenient to use a {@link MimeMessageHelper} for populating
 * the passed-in MimeMessage, in particular when working with attachments or
 * special character encodings.
 * See {@link MimeMessageHelper MimeMessageHelper's javadoc} for an example.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see JavaMailSender#send(MimeMessagePreparator)
 * @see JavaMailSender#send(MimeMessagePreparator[])
 * @see MimeMessageHelper
 * @since 4.0
 */
@FunctionalInterface
public interface MimeMessagePreparator {

  /**
   * Prepare the given new MimeMessage instance.
   *
   * @param mimeMessage the message to prepare
   * @throws jakarta.mail.MessagingException passing any exceptions thrown by MimeMessage
   * methods through for automatic conversion to the MailException hierarchy
   * @throws java.io.IOException passing any exceptions thrown by MimeMessage methods
   * through for automatic conversion to the MailException hierarchy
   * @throws Exception if mail preparation failed, for example when a
   * FreeMarker template cannot be rendered for the mail text
   */
  void prepare(MimeMessage mimeMessage) throws Exception;

}

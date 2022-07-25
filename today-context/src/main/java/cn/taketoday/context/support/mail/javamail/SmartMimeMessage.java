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

import cn.taketoday.lang.Nullable;
import jakarta.activation.FileTypeMap;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

/**
 * Special subclass of the standard JavaMail {@link MimeMessage}, carrying a
 * default encoding to be used when populating the message and a default Java
 * Activation {@link FileTypeMap} to be used for resolving attachment types.
 *
 * <p>Created by {@link JavaMailSenderImpl} in case of a specified default encoding
 * and/or default FileTypeMap. Autodetected by {@link MimeMessageHelper}, which
 * will use the carried encoding and FileTypeMap unless explicitly overridden.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see JavaMailSenderImpl#createMimeMessage()
 * @see MimeMessageHelper#getDefaultEncoding(MimeMessage)
 * @see MimeMessageHelper#getDefaultFileTypeMap(MimeMessage)
 * @since 4.0
 */
class SmartMimeMessage extends MimeMessage {

  @Nullable
  private final String defaultEncoding;

  @Nullable
  private final FileTypeMap defaultFileTypeMap;

  /**
   * Create a new SmartMimeMessage.
   *
   * @param session the JavaMail Session to create the message for
   * @param defaultEncoding the default encoding, or {@code null} if none
   * @param defaultFileTypeMap the default FileTypeMap, or {@code null} if none
   */
  public SmartMimeMessage(
          Session session, @Nullable String defaultEncoding, @Nullable FileTypeMap defaultFileTypeMap) {

    super(session);
    this.defaultEncoding = defaultEncoding;
    this.defaultFileTypeMap = defaultFileTypeMap;
  }

  /**
   * Return the default encoding of this message, or {@code null} if none.
   */
  @Nullable
  public final String getDefaultEncoding() {
    return this.defaultEncoding;
  }

  /**
   * Return the default FileTypeMap of this message, or {@code null} if none.
   */
  @Nullable
  public final FileTypeMap getDefaultFileTypeMap() {
    return this.defaultFileTypeMap;
  }

}

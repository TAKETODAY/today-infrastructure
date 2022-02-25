/*
 * Copyright 2002-2017 the original author or authors.
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

/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.oxm.mime;

import cn.taketoday.lang.Nullable;
import jakarta.activation.DataHandler;

/**
 * Represents a container for MIME attachments
 * Concrete implementations might adapt a SOAPMessage or an email message.
 *
 * @author Arjen Poutsma
 * @see <a href="https://www.w3.org/TR/2005/REC-xop10-20050125/">XML-binary Optimized Packaging</a>
 * @since 4.0
 */
public interface MimeContainer {

  /**
   * Indicate whether this container is a XOP package.
   *
   * @return {@code true} when the constraints specified in
   * <a href="https://www.w3.org/TR/2005/REC-xop10-20050125/#identifying_xop_documents">Identifying XOP Documents</a>
   * are met
   * @see <a href="https://www.w3.org/TR/2005/REC-xop10-20050125/#xop_packages">XOP Packages</a>
   */
  boolean isXopPackage();

  /**
   * Turn this message into a XOP package.
   *
   * @return {@code true} when the message actually is a XOP package
   * @see <a href="https://www.w3.org/TR/2005/REC-xop10-20050125/#xop_packages">XOP Packages</a>
   */
  boolean convertToXopPackage();

  /**
   * Add the given data handler as an attachment to this container.
   *
   * @param contentId the content id of the attachment
   * @param dataHandler the data handler containing the data of the attachment
   */
  void addAttachment(String contentId, DataHandler dataHandler);

  /**
   * Return the attachment with the given content id, or {@code null} if not found.
   *
   * @param contentId the content id
   * @return the attachment, as a data handler
   */
  @Nullable
  DataHandler getAttachment(String contentId);

}

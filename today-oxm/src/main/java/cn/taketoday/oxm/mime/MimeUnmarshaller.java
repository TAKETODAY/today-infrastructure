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

import java.io.IOException;

import javax.xml.transform.Source;

import cn.taketoday.lang.Nullable;
import cn.taketoday.oxm.Unmarshaller;
import cn.taketoday.oxm.XmlMappingException;

/**
 * Subinterface of {@link Unmarshaller} that can use MIME attachments
 * to optimize storage of binary data. Attachments can be added as MTOM, XOP, or SwA.
 *
 * @author Arjen Poutsma
 * @see <a href="https://www.w3.org/TR/2004/WD-soap12-mtom-20040608/">SOAP Message Transmission Optimization Mechanism</a>
 * @see <a href="https://www.w3.org/TR/2005/REC-xop10-20050125/">XML-binary Optimized Packaging</a>
 * @since 4.0
 */
public interface MimeUnmarshaller extends Unmarshaller {

  /**
   * Unmarshals the given provided {@link Source} into an object graph,
   * reading binary attachments from a {@link MimeContainer}.
   *
   * @param source the source to marshal from
   * @param mimeContainer the MIME container to read extracted binary content from
   * @return the object graph
   * @throws XmlMappingException if the given source cannot be mapped to an object
   * @throws IOException if an I/O Exception occurs
   */
  Object unmarshal(Source source, @Nullable MimeContainer mimeContainer) throws XmlMappingException, IOException;

}

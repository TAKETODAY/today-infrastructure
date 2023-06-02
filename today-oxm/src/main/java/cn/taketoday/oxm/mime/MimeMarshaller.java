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

import javax.xml.transform.Result;

import cn.taketoday.lang.Nullable;
import cn.taketoday.oxm.Marshaller;
import cn.taketoday.oxm.XmlMappingException;

/**
 * Subinterface of {@link Marshaller} that can use MIME attachments to optimize
 * storage of binary data. Attachments can be added as MTOM, XOP, or SwA.
 *
 * @author Arjen Poutsma
 * @see <a href="https://www.w3.org/TR/2004/WD-soap12-mtom-20040608/">SOAP Message Transmission Optimization Mechanism</a>
 * @see <a href="https://www.w3.org/TR/2005/REC-xop10-20050125/">XML-binary Optimized Packaging</a>
 * @since 4.0
 */
public interface MimeMarshaller extends Marshaller {

  /**
   * Marshals the object graph with the given root into the provided {@link Result},
   * writing binary data to a {@link MimeContainer}.
   *
   * @param graph the root of the object graph to marshal
   * @param result the result to marshal to
   * @param mimeContainer the MIME container to write extracted binary content to
   * @throws XmlMappingException if the given object cannot be marshalled to the result
   * @throws IOException if an I/O exception occurs
   */
  void marshal(Object graph, Result result, @Nullable MimeContainer mimeContainer) throws XmlMappingException, IOException;

}

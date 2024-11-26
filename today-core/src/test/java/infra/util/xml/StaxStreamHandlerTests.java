/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.util.xml;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;

/**
 * @author Arjen Poutsma
 */
class StaxStreamHandlerTests extends AbstractStaxHandlerTests {

  @Override
  protected AbstractStaxHandler createStaxHandler(Result result) throws XMLStreamException {
    XMLOutputFactory outputFactory = XMLOutputFactory.newFactory();
    XMLStreamWriter streamWriter = outputFactory.createXMLStreamWriter(result);
    return new StaxStreamHandler(streamWriter);
  }

}

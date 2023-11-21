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

package cn.taketoday.util.xml;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.XMLEvent;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Implementation of {@code XMLEventReader} based on a {@link List}
 * of {@link XMLEvent} elements.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 4.0
 */
class ListBasedXMLEventReader extends AbstractXMLEventReader {

  private final List<XMLEvent> events;

  @Nullable
  private XMLEvent currentEvent;

  private int cursor = 0;

  public ListBasedXMLEventReader(List<XMLEvent> events) {
    Assert.notNull(events, "XMLEvent List is required");
    this.events = new ArrayList<>(events);
  }

  @Override
  public boolean hasNext() {
    return (this.cursor < this.events.size());
  }

  @Override
  public XMLEvent nextEvent() {
    if (hasNext()) {
      this.currentEvent = this.events.get(this.cursor);
      this.cursor++;
      return this.currentEvent;
    }
    else {
      throw new NoSuchElementException();
    }
  }

  @Override
  @Nullable
  public XMLEvent peek() {
    if (hasNext()) {
      return this.events.get(this.cursor);
    }
    else {
      return null;
    }
  }

  @Override
  public String getElementText() throws XMLStreamException {
    checkIfClosed();
    if (this.currentEvent == null || !this.currentEvent.isStartElement()) {
      throw new XMLStreamException("Not at START_ELEMENT: " + this.currentEvent);
    }

    StringBuilder builder = new StringBuilder();
    while (true) {
      XMLEvent event = nextEvent();
      if (event.isEndElement()) {
        break;
      }
      else if (!event.isCharacters()) {
        throw new XMLStreamException("Unexpected non-text event: " + event);
      }
      Characters characters = event.asCharacters();
      if (!characters.isIgnorableWhiteSpace()) {
        builder.append(event.asCharacters().getData());
      }
    }
    return builder.toString();
  }

  @Override
  @Nullable
  public XMLEvent nextTag() throws XMLStreamException {
    checkIfClosed();

    while (true) {
      XMLEvent event = nextEvent();
      switch (event.getEventType()) {
        case XMLStreamConstants.START_ELEMENT:
        case XMLStreamConstants.END_ELEMENT:
          return event;
        case XMLStreamConstants.END_DOCUMENT:
          return null;
        case XMLStreamConstants.SPACE:
        case XMLStreamConstants.COMMENT:
        case XMLStreamConstants.PROCESSING_INSTRUCTION:
          continue;
        case XMLStreamConstants.CDATA:
        case XMLStreamConstants.CHARACTERS:
          if (!event.asCharacters().isWhiteSpace()) {
            throw new XMLStreamException(
                    "Non-ignorable whitespace CDATA or CHARACTERS event: " + event);
          }
          break;
        default:
          throw new XMLStreamException("Expected START_ELEMENT or END_ELEMENT: " + event);
      }
    }
  }

  @Override
  public void close() {
    super.close();
    this.events.clear();
  }

}

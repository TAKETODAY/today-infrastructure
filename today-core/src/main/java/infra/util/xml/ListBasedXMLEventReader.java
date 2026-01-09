/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.util.xml;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.XMLEvent;

import infra.lang.Assert;

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

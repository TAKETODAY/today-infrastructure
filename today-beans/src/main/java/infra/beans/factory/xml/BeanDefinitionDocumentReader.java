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

package infra.beans.factory.xml;

import org.w3c.dom.Document;

import infra.beans.factory.BeanDefinitionStoreException;

/**
 * SPI for parsing an XML document that contains Framework bean definitions.
 * Used by {@link XmlBeanDefinitionReader} for actually parsing a DOM document.
 *
 * <p>Instantiated per document to parse: implementations can hold
 * state in instance variables during the execution of the
 * {@code registerBeanDefinitions} method &mdash; for example, global
 * settings that are defined for all bean definitions in the document.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @see XmlBeanDefinitionReader#setDocumentReaderClass
 * @since 4.0
 */
public interface BeanDefinitionDocumentReader {

  /**
   * Read bean definitions from the given DOM document and
   * register them with the registry in the given reader context.
   *
   * @param doc the DOM document
   * @param readerContext the current context of the reader
   * (includes the target registry and the resource being parsed)
   * @throws BeanDefinitionStoreException in case of parsing errors
   */
  void registerBeanDefinitions(Document doc, XmlReaderContext readerContext)
          throws BeanDefinitionStoreException;

}

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

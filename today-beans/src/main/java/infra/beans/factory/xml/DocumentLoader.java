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
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;

import infra.util.xml.XmlValidationModeDetector;

/**
 * Strategy interface for loading an XML {@link Document}.
 *
 * @author Rob Harrop
 * @see DefaultDocumentLoader
 * @since 4.0
 */
public interface DocumentLoader {

  /**
   * Load a {@link Document document} from the supplied {@link InputSource source}.
   *
   * @param inputSource the source of the document that is to be loaded
   * @param entityResolver the resolver that is to be used to resolve any entities
   * @param errorHandler used to report any errors during document loading
   * @param validationMode the type of validation
   * {@link XmlValidationModeDetector#VALIDATION_DTD DTD}
   * or {@link XmlValidationModeDetector#VALIDATION_XSD XSD})
   * @param namespaceAware {@code true} if support for XML namespaces is to be provided
   * @return the loaded {@link Document document}
   * @throws Exception if an error occurs
   */
  Document loadDocument(InputSource inputSource, EntityResolver entityResolver,
          ErrorHandler errorHandler, int validationMode, boolean namespaceAware) throws Exception;

}

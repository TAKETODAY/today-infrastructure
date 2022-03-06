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

package cn.taketoday.beans.factory.xml;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import cn.taketoday.beans.factory.BeanDefinitionStoreException;

/**
 * XML-specific BeanDefinitionStoreException subclass that wraps a
 * {@link SAXException}, typically a {@link SAXParseException}
 * which contains information about the error location.
 *
 * @author Juergen Hoeller
 * @see #getLineNumber()
 * @see SAXParseException
 * @since 4.0
 */
@SuppressWarnings("serial")
public class XmlBeanDefinitionStoreException extends BeanDefinitionStoreException {

  /**
   * Create a new XmlBeanDefinitionStoreException.
   *
   * @param resourceDescription description of the resource that the bean definition came from
   * @param msg the detail message (used as exception message as-is)
   * @param cause the SAXException (typically a SAXParseException) root cause
   * @see SAXParseException
   */
  public XmlBeanDefinitionStoreException(String resourceDescription, String msg, SAXException cause) {
    super(resourceDescription, msg, cause);
  }

  /**
   * Return the line number in the XML resource that failed.
   *
   * @return the line number if available (in case of a SAXParseException); -1 else
   * @see SAXParseException#getLineNumber()
   */
  public int getLineNumber() {
    Throwable cause = getCause();
    if (cause instanceof SAXParseException) {
      return ((SAXParseException) cause).getLineNumber();
    }
    return -1;
  }

}

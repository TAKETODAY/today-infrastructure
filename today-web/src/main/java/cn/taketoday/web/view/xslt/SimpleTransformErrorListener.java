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

package cn.taketoday.web.view.xslt;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;

import cn.taketoday.logging.Logger;

/**
 * Simple {@code javax.xml.transform.ErrorListener} implementation:
 * logs warnings using the given Commons Logging logger instance,
 * and rethrows errors to discontinue the XML transformation.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
public class SimpleTransformErrorListener implements ErrorListener {

  private final Logger logger;

  /**
   * Create a new SimpleTransformErrorListener for the given
   * Commons Logging logger instance.
   */
  public SimpleTransformErrorListener(Logger logger) {
    this.logger = logger;
  }

  @Override
  public void warning(TransformerException ex) throws TransformerException {
    logger.warn("XSLT transformation warning", ex);
  }

  @Override
  public void error(TransformerException ex) throws TransformerException {
    logger.error("XSLT transformation error", ex);
  }

  @Override
  public void fatalError(TransformerException ex) throws TransformerException {
    throw ex;
  }

}

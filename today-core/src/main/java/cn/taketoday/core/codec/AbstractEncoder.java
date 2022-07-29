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

package cn.taketoday.core.codec;

import java.util.Arrays;
import java.util.List;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.MimeType;

/**
 * Abstract base class for {@link Encoder} implementations.
 *
 * @param <T> the element type
 * @author Sebastien Deleuze
 * @author Arjen Poutsma
 * @since 4.0
 */
public abstract class AbstractEncoder<T> implements Encoder<T> {

  private final List<MimeType> encodableMimeTypes;

  protected Logger logger = LoggerFactory.getLogger(getClass());

  protected AbstractEncoder(MimeType... supportedMimeTypes) {
    this.encodableMimeTypes = Arrays.asList(supportedMimeTypes);
  }

  /**
   * Set an alternative logger to use than the one based on the class name.
   *
   * @param logger the logger to use
   */
  public void setLogger(Logger logger) {
    this.logger = logger;
  }

  /**
   * Return the currently configured Logger.
   */
  public Logger getLogger() {
    return logger;
  }

  @Override
  public List<MimeType> getEncodableMimeTypes() {
    return this.encodableMimeTypes;
  }

  @Override
  public boolean canEncode(ResolvableType elementType, @Nullable MimeType mimeType) {
    if (mimeType == null) {
      return true;
    }
    for (MimeType candidate : this.encodableMimeTypes) {
      if (candidate.isCompatibleWith(mimeType)) {
        return true;
      }
    }
    return false;
  }

}

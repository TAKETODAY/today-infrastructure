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
package cn.taketoday.http;

import cn.taketoday.util.InvalidMimeTypeException;

/**
 * Exception thrown from {@link MediaType#parseMediaType(String)} in case of
 * encountering an invalid media type specification String.
 *
 * @author Juergen Hoeller
 * @author TODAY
 * @since 2019-12-08 20:03
 */
@SuppressWarnings("serial")
public class InvalidMediaTypeException extends IllegalArgumentException {

  private final String mediaType;

  /**
   * Create a new InvalidMediaTypeException for the given media type.
   *
   * @param mediaType the offending media type
   * @param message a detail message indicating the invalid part
   */
  public InvalidMediaTypeException(String mediaType, String message) {
    super("Invalid media type \"" + mediaType + "\": " + message);
    this.mediaType = mediaType;
  }

  /**
   * Constructor that allows wrapping {@link InvalidMimeTypeException}.
   */
  InvalidMediaTypeException(InvalidMimeTypeException ex) {
    super(ex.getMessage(), ex);
    this.mediaType = ex.getMimeType();
  }

  /**
   * Return the offending media type.
   */
  public String getMediaType() {
    return this.mediaType;
  }

}

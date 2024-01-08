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

package cn.taketoday.http.codec.protobuf;

import java.util.List;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.MimeType;

/**
 * Base class providing support methods for Protobuf encoding and decoding.
 *
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class ProtobufCodecSupport {

  static final List<MimeType> MIME_TYPES = List.of(
          new MimeType("application", "x-protobuf"),
          new MimeType("application", "octet-stream"),
          new MimeType("application", "vnd.google.protobuf")
  );

  static final String DELIMITED_VALUE = "true";
  static final String DELIMITED_KEY = "delimited";

  protected boolean supportsMimeType(@Nullable MimeType mimeType) {
    return (mimeType == null || MIME_TYPES.stream().anyMatch(m -> m.isCompatibleWith(mimeType)));
  }

  protected List<MimeType> getMimeTypes() {
    return MIME_TYPES;
  }

}

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

package cn.taketoday.web.resource;

import java.io.IOException;

import cn.taketoday.core.io.ByteArrayResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.lang.Nullable;

/**
 * An extension of {@link ByteArrayResource} that a {@link ResourceTransformer}
 * can use to represent an original resource preserving all other information
 * except the content.
 *
 * @author Jeremy Grelle
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class TransformedResource extends ByteArrayResource {

  @Nullable
  private final String filename;

  private final long lastModified;

  public TransformedResource(Resource original, byte[] transformedContent) {
    super(transformedContent);
    this.filename = original.getFilename();
    try {
      this.lastModified = original.lastModified();
    }
    catch (IOException ex) {
      // should never happen
      throw new IllegalArgumentException(ex);
    }
  }

  @Override
  @Nullable
  public String getFilename() {
    return this.filename;
  }

  @Override
  public long lastModified() throws IOException {
    return this.lastModified;
  }

}

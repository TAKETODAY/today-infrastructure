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

package infra.web.resource;

import java.io.IOException;

import infra.core.io.ByteArrayResource;
import infra.core.io.Resource;
import infra.lang.Nullable;

/**
 * An extension of {@link ByteArrayResource} that a {@link ResourceTransformer}
 * can use to represent an original resource preserving all other information
 * except the content.
 *
 * @author Jeremy Grelle
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class TransformedResource extends ByteArrayResource {

  @Nullable
  private final String filename;

  private final long lastModified;

  public TransformedResource(Resource original, byte[] transformedContent) {
    super(transformedContent);
    this.filename = original.getName();
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
  public String getName() {
    return this.filename;
  }

  @Override
  public long lastModified() throws IOException {
    return this.lastModified;
  }

}

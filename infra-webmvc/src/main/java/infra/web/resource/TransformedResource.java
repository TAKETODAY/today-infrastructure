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

package infra.web.resource;

import org.jspecify.annotations.Nullable;

import java.io.IOException;

import infra.core.io.ByteArrayResource;
import infra.core.io.Resource;

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

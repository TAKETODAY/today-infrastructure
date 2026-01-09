/*
 * Copyright 2012-present the original author or authors.
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

package infra.app.io;

import org.jspecify.annotations.Nullable;

import infra.core.io.ByteArrayResource;
import infra.core.io.ProtocolResolver;
import infra.core.io.Resource;
import infra.core.io.ResourceLoader;
import infra.util.Base64Utils;

/**
 * {@link ProtocolResolver} for resources containing base 64 encoded text.
 *
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class Base64ProtocolResolver implements ProtocolResolver {

  private static final String BASE64_PREFIX = "base64:";

  @Nullable
  @Override
  public Resource resolve(String location, ResourceLoader resourceLoader) {
    if (location.startsWith(BASE64_PREFIX)) {
      String value = location.substring(BASE64_PREFIX.length());
      return new ByteArrayResource(Base64Utils.decodeFromString(value));
    }
    return null;
  }

}

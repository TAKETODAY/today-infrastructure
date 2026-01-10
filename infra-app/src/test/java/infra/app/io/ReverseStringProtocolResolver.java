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

import java.nio.charset.StandardCharsets;

import infra.core.io.ByteArrayResource;
import infra.core.io.ProtocolResolver;
import infra.core.io.Resource;
import infra.core.io.ResourceLoader;

/**
 * Test {@link ProtocolResolver} that reverses a String.
 *
 * @author Phillip Webb
 */
class ReverseStringProtocolResolver implements ProtocolResolver {

  private static final String PREFIX = "reverse:";

  @Override
  public Resource resolve(String location, ResourceLoader resourceLoader) {
    if (!location.startsWith(PREFIX)) {
      return null;
    }
    return new ByteArrayResource(reverse(location.substring(PREFIX.length())));
  }

  private byte[] reverse(String substring) {
    return new StringBuilder(substring).reverse().toString().getBytes(StandardCharsets.UTF_8);
  }

}

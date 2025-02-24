/*
 * Copyright 2017 - 2025 the original author or authors.
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

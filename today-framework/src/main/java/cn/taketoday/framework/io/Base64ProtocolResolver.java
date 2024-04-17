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

package cn.taketoday.framework.io;

import java.util.Base64;

import cn.taketoday.core.io.ByteArrayResource;
import cn.taketoday.core.io.ProtocolResolver;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceLoader;

/**
 * {@link ProtocolResolver} for resources containing base 64 encoded text.
 *
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class Base64ProtocolResolver implements ProtocolResolver {

  private static final String BASE64_PREFIX = "base64:";

  @Override
  public Resource resolve(String location, ResourceLoader resourceLoader) {
    if (location.startsWith(BASE64_PREFIX)) {
      String value = location.substring(BASE64_PREFIX.length());
      return new ByteArrayResource(decode(value));
    }
    return null;
  }

  private static byte[] decode(String location) {
    return Base64.getDecoder().decode(location);
  }

}

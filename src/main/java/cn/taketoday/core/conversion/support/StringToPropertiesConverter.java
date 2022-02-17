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

package cn.taketoday.core.conversion.support;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import cn.taketoday.core.conversion.Converter;

/**
 * Converts a String to a Properties by calling Properties#load(java.io.InputStream).
 * Uses ISO-8559-1 encoding required by Properties.
 *
 * @author Keith Donald
 * @since 3.0
 */
final class StringToPropertiesConverter implements Converter<String, Properties> {

  @Override
  public Properties convert(String source) {
    try {
      Properties props = new Properties();
      // Must use the ISO-8859-1 encoding because Properties.load(stream) expects it.
      props.load(new ByteArrayInputStream(source.getBytes(StandardCharsets.ISO_8859_1)));
      return props;
    }
    catch (Exception ex) {
      // Should never happen.
      throw new IllegalArgumentException("Failed to parse [" + source + "] into Properties", ex);
    }
  }

}

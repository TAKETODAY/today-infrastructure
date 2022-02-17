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

package cn.taketoday.core.conversion.support.annotation;

import java.io.IOException;

import cn.taketoday.core.conversion.Converter;
import cn.taketoday.core.io.InputStreamSource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.util.FileCopyUtils;

/**
 * {@link Converter} to convert from an {@link InputStreamSource} to a {@code byte[]}.
 *
 * @author Phillip Webb
 */
class InputStreamSourceToByteArrayConverter implements Converter<InputStreamSource, byte[]> {

  @Override
  public byte[] convert(InputStreamSource source) {
    try {
      return FileCopyUtils.copyToByteArray(source.getInputStream());
    }
    catch (IOException ex) {
      throw new IllegalStateException("Unable to read from " + getName(source), ex);
    }
  }

  private String getName(InputStreamSource source) {
    if (source instanceof Resource) {
      return source.toString();
    }
    return "input stream source";
  }

}

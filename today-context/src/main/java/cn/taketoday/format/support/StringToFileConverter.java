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

package cn.taketoday.format.support;

import java.io.File;
import java.io.IOException;

import cn.taketoday.core.conversion.Converter;
import cn.taketoday.core.io.DefaultResourceLoader;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.util.ResourceUtils;

/**
 * {@link Converter} to convert from a {@link String} to a {@link File}. Supports basic
 * file conversion as well as file URLs.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class StringToFileConverter implements Converter<String, File> {

  private static final ResourceLoader resourceLoader = new DefaultResourceLoader(null);

  @Override
  public File convert(String source) {
    if (ResourceUtils.isUrl(source)) {
      return getFile(resourceLoader.getResource(source));
    }
    File file = new File(source);
    if (file.exists()) {
      return file;
    }
    Resource resource = resourceLoader.getResource(source);
    if (resource.exists()) {
      return getFile(resource);
    }
    return file;
  }

  private File getFile(Resource resource) {
    try {
      return resource.getFile();
    }
    catch (IOException ex) {
      throw new IllegalStateException("Could not retrieve file for " + resource + ": " + ex.getMessage());
    }
  }

}

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

package cn.taketoday.framework.test.json;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.util.FileCopyUtils;

/**
 * Internal helper used to load JSON from various sources.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class JsonLoader {

  private final Class<?> resourceLoadClass;

  private final Charset charset;

  JsonLoader(Class<?> resourceLoadClass, Charset charset) {
    this.resourceLoadClass = resourceLoadClass;
    this.charset = (charset != null) ? charset : StandardCharsets.UTF_8;
  }

  Class<?> getResourceLoadClass() {
    return this.resourceLoadClass;
  }

  String getJson(CharSequence source) {
    if (source == null) {
      return null;
    }
    if (source.toString().endsWith(".json")) {
      return getJson(new ClassPathResource(source.toString(), this.resourceLoadClass));
    }
    return source.toString();
  }

  String getJson(String path, Class<?> resourceLoadClass) {
    return getJson(new ClassPathResource(path, resourceLoadClass));
  }

  String getJson(byte[] source) {
    return getJson(new ByteArrayInputStream(source));
  }

  String getJson(File source) {
    try {
      return getJson(new FileInputStream(source));
    }
    catch (IOException ex) {
      throw new IllegalStateException("Unable to load JSON from " + source, ex);
    }
  }

  String getJson(Resource source) {
    try {
      return getJson(source.getInputStream());
    }
    catch (IOException ex) {
      throw new IllegalStateException("Unable to load JSON from " + source, ex);
    }
  }

  String getJson(InputStream source) {
    try {
      return FileCopyUtils.copyToString(new InputStreamReader(source, this.charset));
    }
    catch (IOException ex) {
      throw new IllegalStateException("Unable to load JSON from InputStream", ex);
    }
  }

}

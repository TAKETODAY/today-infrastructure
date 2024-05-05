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
package cn.taketoday.mock.api.fileupload.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import cn.taketoday.mock.api.fileupload.FileItemHeaders;

/**
 * Default implementation of the {@link FileItemHeaders} interface.
 *
 * @since FileUpload 1.2.1
 */
public class FileItemHeadersImpl implements FileItemHeaders, Serializable {

  /**
   * Serial version UID, being used, if serialized.
   */
  private static final long serialVersionUID = -4455695752627032559L;

  /**
   * Map of {@code String} keys to a {@code List} of
   * {@code String} instances.
   */
  private final Map<String, List<String>> headerNameToValueListMap = new LinkedHashMap<>();

  @Override
  public String getHeader(final String name) {
    final String nameLower = name.toLowerCase(Locale.ENGLISH);
    final List<String> headerValueList = headerNameToValueListMap.get(nameLower);
    if (null == headerValueList) {
      return null;
    }
    return headerValueList.get(0);
  }

  @Override
  public Iterator<String> getHeaderNames() {
    return headerNameToValueListMap.keySet().iterator();
  }

  @Override
  public Iterator<String> getHeaders(final String name) {
    final String nameLower = name.toLowerCase(Locale.ENGLISH);
    List<String> headerValueList = headerNameToValueListMap.get(nameLower);
    if (null == headerValueList) {
      headerValueList = Collections.emptyList();
    }
    return headerValueList.iterator();
  }

  /**
   * Method to add header values to this instance.
   *
   * @param name name of this header
   * @param value value of this header
   */
  public synchronized void addHeader(final String name, final String value) {
    final String nameLower = name.toLowerCase(Locale.ENGLISH);
    final List<String> headerValueList = headerNameToValueListMap.
            computeIfAbsent(nameLower, k -> new ArrayList<>());
    headerValueList.add(value);
  }

}

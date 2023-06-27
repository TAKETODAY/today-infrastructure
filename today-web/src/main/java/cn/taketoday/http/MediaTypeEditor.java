/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.http;

import java.beans.PropertyEditorSupport;

import cn.taketoday.util.StringUtils;

/**
 * {@link java.beans.PropertyEditor Editor} for {@link MediaType}
 * descriptors, to automatically convert {@code String} specifications
 * (e.g. {@code "text/html"}) to {@code MediaType} properties.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see MediaType
 * @since 4.0
 */
public class MediaTypeEditor extends PropertyEditorSupport {

  @Override
  public void setAsText(String text) {
    if (StringUtils.hasText(text)) {
      setValue(MediaType.parseMediaType(text));
    }
    else {
      setValue(null);
    }
  }

  @Override
  public String getAsText() {
    MediaType mediaType = (MediaType) getValue();
    return (mediaType != null ? mediaType.toString() : "");
  }

}

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

package cn.taketoday.beans.propertyeditors;

import java.beans.PropertyEditorSupport;
import java.io.IOException;

import cn.taketoday.core.io.EncodedResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceEditor;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * One-way PropertyEditor which can convert from a text String to a
 * {@code java.io.Reader}, interpreting the given String as a Framework
 * resource location (e.g. a URL String).
 *
 * <p>Supports Framework-style URL notation: any fully qualified standard URL
 * ("file:", "http:", etc.) and Framework's special "classpath:" pseudo-URL.
 *
 * <p>Note that such readers usually do not get closed by Framework itself!
 *
 * @author Juergen Hoeller
 * @see java.io.Reader
 * @see cn.taketoday.core.io.ResourceEditor
 * @see cn.taketoday.core.io.ResourceLoader
 * @see InputStreamEditor
 * @since 4.0
 */
public class ReaderEditor extends PropertyEditorSupport {

  private final ResourceEditor resourceEditor;

  /**
   * Create a new ReaderEditor, using the default ResourceEditor underneath.
   */
  public ReaderEditor() {
    this.resourceEditor = new ResourceEditor();
  }

  /**
   * Create a new ReaderEditor, using the given ResourceEditor underneath.
   *
   * @param resourceEditor the ResourceEditor to use
   */
  public ReaderEditor(ResourceEditor resourceEditor) {
    Assert.notNull(resourceEditor, "ResourceEditor must not be null");
    this.resourceEditor = resourceEditor;
  }

  @Override
  public void setAsText(String text) throws IllegalArgumentException {
    this.resourceEditor.setAsText(text);
    Resource resource = (Resource) this.resourceEditor.getValue();
    try {
      setValue(resource != null ? new EncodedResource(resource).getReader() : null);
    }
    catch (IOException ex) {
      throw new IllegalArgumentException("Failed to retrieve Reader for " + resource, ex);
    }
  }

  /**
   * This implementation returns {@code null} to indicate that
   * there is no appropriate text representation.
   */
  @Override
  @Nullable
  public String getAsText() {
    return null;
  }

}

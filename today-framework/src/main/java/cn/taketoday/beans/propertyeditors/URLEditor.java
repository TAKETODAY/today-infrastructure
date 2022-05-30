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
import java.net.URL;

import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceEditor;
import cn.taketoday.lang.Assert;

/**
 * Editor for {@code java.net.URL}, to directly populate a URL property
 * instead of using a String property as bridge.
 *
 * <p>Supports Framework-style URL notation: any fully qualified standard URL
 * ("file:", "http:", etc) and Framework's special "classpath:" pseudo-URL,
 * as well as Framework's context-specific relative file paths.
 *
 * <p>Note: A URL must specify a valid protocol, else it will be rejected
 * upfront. However, the target resource does not necessarily have to exist
 * at the time of URL creation; this depends on the specific resource type.
 *
 * @author Juergen Hoeller
 * @see URL
 * @see cn.taketoday.core.io.ResourceEditor
 * @see cn.taketoday.core.io.ResourceLoader
 * @see FileEditor
 * @see InputStreamEditor
 * @since 4.0
 */
public class URLEditor extends PropertyEditorSupport {

  private final ResourceEditor resourceEditor;

  /**
   * Create a new URLEditor, using a default ResourceEditor underneath.
   */
  public URLEditor() {
    this.resourceEditor = new ResourceEditor();
  }

  /**
   * Create a new URLEditor, using the given ResourceEditor underneath.
   *
   * @param resourceEditor the ResourceEditor to use
   */
  public URLEditor(ResourceEditor resourceEditor) {
    Assert.notNull(resourceEditor, "ResourceEditor must not be null");
    this.resourceEditor = resourceEditor;
  }

  @Override
  public void setAsText(String text) throws IllegalArgumentException {
    resourceEditor.setAsText(text);
    if (resourceEditor.getValue() instanceof Resource resource) {
      try {
        setValue(resource.getURL());
      }
      catch (Exception ex) {
        throw new IllegalArgumentException("Could not retrieve URL for " + resource + ": " + ex.getMessage());
      }
    }
    else {
      setValue(null);
    }
  }

  @Override
  public String getAsText() {
    if (getValue() instanceof URL value) {
      return value.toExternalForm();
    }
    return "";
  }

}

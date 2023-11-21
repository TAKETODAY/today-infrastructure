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

import org.xml.sax.InputSource;

import java.beans.PropertyEditorSupport;
import java.io.IOException;

import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceEditor;
import cn.taketoday.lang.Assert;

/**
 * Editor for {@code org.xml.sax.InputSource}, converting from a
 * Framework resource location String to a SAX InputSource object.
 *
 * <p>Supports Framework-style URL notation: any fully qualified standard URL
 * ("file:", "http:", etc) and Framework's special "classpath:" pseudo-URL.
 *
 * @author Juergen Hoeller
 * @see InputSource
 * @see cn.taketoday.core.io.ResourceEditor
 * @see cn.taketoday.core.io.ResourceLoader
 * @see URLEditor
 * @see FileEditor
 * @since 4.0
 */
public class InputSourceEditor extends PropertyEditorSupport {

  private final ResourceEditor resourceEditor;

  /**
   * Create a new InputSourceEditor,
   * using the default ResourceEditor underneath.
   */
  public InputSourceEditor() {
    this.resourceEditor = new ResourceEditor();
  }

  /**
   * Create a new InputSourceEditor,
   * using the given ResourceEditor underneath.
   *
   * @param resourceEditor the ResourceEditor to use
   */
  public InputSourceEditor(ResourceEditor resourceEditor) {
    Assert.notNull(resourceEditor, "ResourceEditor is required");
    this.resourceEditor = resourceEditor;
  }

  @Override
  public void setAsText(String text) throws IllegalArgumentException {
    this.resourceEditor.setAsText(text);
    Resource resource = (Resource) this.resourceEditor.getValue();
    try {
      setValue(resource != null ? new InputSource(resource.getURL().toString()) : null);
    }
    catch (IOException ex) {
      throw new IllegalArgumentException(
              "Could not retrieve URL for " + resource + ": " + ex.getMessage());
    }
  }

  @Override
  public String getAsText() {
    InputSource value = (InputSource) getValue();
    return (value != null ? value.getSystemId() : "");
  }

}

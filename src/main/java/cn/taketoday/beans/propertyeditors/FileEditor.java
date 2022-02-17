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
import java.io.File;
import java.io.IOException;

import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceEditor;
import cn.taketoday.util.Assert;
import cn.taketoday.util.ResourceUtils;
import cn.taketoday.util.StringUtils;

/**
 * Editor for {@code java.io.File}, to directly populate a File property
 * from a Spring resource location.
 *
 * <p>Supports Spring-style URL notation: any fully qualified standard URL
 * ("file:", "http:", etc) and Spring's special "classpath:" pseudo-URL.
 *
 * <p><b>NOTE:</b> The behavior of this editor has changed in Spring 2.0.
 * Previously, it created a File instance directly from a filename.
 * As of Spring 2.0, it takes a standard Spring resource location as input;
 * this is consistent with URLEditor and InputStreamEditor now.
 *
 * <p><b>NOTE:</b> In Spring 2.5 the following modification was made.
 * If a file name is specified without a URL prefix or without an absolute path
 * then we try to locate the file using standard ResourceLoader semantics.
 * If the file was not found, then a File instance is created assuming the file
 * name refers to a relative file location.
 *
 * @author Juergen Hoeller
 * @author Thomas Risberg
 * @see File
 * @see cn.taketoday.core.io.ResourceEditor
 * @see cn.taketoday.core.io.ResourceLoader
 * @see URLEditor
 * @see InputStreamEditor
 * @since 09.12.2003
 */
public class FileEditor extends PropertyEditorSupport {

  private final ResourceEditor resourceEditor;

  /**
   * Create a new FileEditor, using a default ResourceEditor underneath.
   */
  public FileEditor() {
    this.resourceEditor = new ResourceEditor();
  }

  /**
   * Create a new FileEditor, using the given ResourceEditor underneath.
   *
   * @param resourceEditor the ResourceEditor to use
   */
  public FileEditor(ResourceEditor resourceEditor) {
    Assert.notNull(resourceEditor, "ResourceEditor must not be null");
    this.resourceEditor = resourceEditor;
  }

  @Override
  public void setAsText(String text) throws IllegalArgumentException {
    if (!StringUtils.hasText(text)) {
      setValue(null);
      return;
    }

    // Check whether we got an absolute file path without "file:" prefix.
    // For backwards compatibility, we'll consider those as straight file path.
    File file = null;
    if (!ResourceUtils.isUrl(text)) {
      file = new File(text);
      if (file.isAbsolute()) {
        setValue(file);
        return;
      }
    }

    // Proceed with standard resource location parsing.
    this.resourceEditor.setAsText(text);
    Resource resource = (Resource) this.resourceEditor.getValue();

    // If it's a URL or a path pointing to an existing resource, use it as-is.
    if (file == null || resource.exists()) {
      try {
        setValue(resource.getFile());
      }
      catch (IOException ex) {
        throw new IllegalArgumentException(
                "Could not retrieve file for " + resource + ": " + ex.getMessage());
      }
    }
    else {
      // Set a relative File reference and hope for the best.
      setValue(file);
    }
  }

  @Override
  public String getAsText() {
    File value = (File) getValue();
    return (value != null ? value.getPath() : "");
  }

}

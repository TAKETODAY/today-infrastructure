/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.beans.propertyeditors;

import java.beans.PropertyEditorSupport;
import java.io.File;
import java.io.IOException;

import infra.core.io.Resource;
import infra.core.io.ResourceEditor;
import infra.core.io.ResourceLoader;
import infra.lang.Assert;
import infra.util.ResourceUtils;
import infra.util.StringUtils;

/**
 * Editor for {@code java.io.File}, to directly populate a File property
 * from a Framework resource location.
 *
 * <p>Supports Framework-style URL notation: any fully qualified standard URL
 * ("file:", "http:", etc) and Framework's special "classpath:" pseudo-URL.
 *
 * <p><b>NOTE:</b> it takes a standard Framework resource location as input;
 * this is consistent with URLEditor and InputStreamEditor now.
 *
 * <p><b>NOTE:</b> the following modification was made.
 * If a file name is specified without a URL prefix or without an absolute path
 * then we try to locate the file using standard ResourceLoader semantics.
 * If the file was not found, then a File instance is created assuming the file
 * name refers to a relative file location.
 *
 * @author Juergen Hoeller
 * @author Thomas Risberg
 * @see File
 * @see ResourceEditor
 * @see ResourceLoader
 * @see URLEditor
 * @see InputStreamEditor
 * @since 4.0
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
    Assert.notNull(resourceEditor, "ResourceEditor is required");
    this.resourceEditor = resourceEditor;
  }

  @Override
  public void setAsText(String text) throws IllegalArgumentException {
    if (StringUtils.isBlank(text)) {
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

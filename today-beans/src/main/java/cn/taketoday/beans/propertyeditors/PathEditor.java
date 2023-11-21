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
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;

import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceEditor;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.ResourceUtils;

/**
 * Editor for {@code java.nio.file.Path}, to directly populate a Path
 * property instead of using a String property as bridge.
 *
 * <p>Based on {@link Paths#get(URI)}'s resolution algorithm, checking
 * registered NIO file system providers, including the default file system
 * for "file:..." paths. Also supports Framework-style URL notation: any fully
 * qualified standard URL and Framework's special "classpath:" pseudo-URL, as
 * well as Framework's context-specific relative file paths. As a fallback, a
 * path will be resolved in the file system via {@code Paths#get(String)}
 * if no existing context-relative resource could be found.
 *
 * @author Juergen Hoeller
 * @see Path
 * @see Paths#get(URI)
 * @see ResourceEditor
 * @see cn.taketoday.core.io.ResourceLoader
 * @see FileEditor
 * @see URLEditor
 * @since 4.0
 */
public class PathEditor extends PropertyEditorSupport {

  private final ResourceEditor resourceEditor;

  /**
   * Create a new PathEditor, using the default ResourceEditor underneath.
   */
  public PathEditor() {
    this.resourceEditor = new ResourceEditor();
  }

  /**
   * Create a new PathEditor, using the given ResourceEditor underneath.
   *
   * @param resourceEditor the ResourceEditor to use
   */
  public PathEditor(ResourceEditor resourceEditor) {
    Assert.notNull(resourceEditor, "ResourceEditor is required");
    this.resourceEditor = resourceEditor;
  }

  @Override
  public void setAsText(String text) throws IllegalArgumentException {
    boolean nioPathCandidate = !text.startsWith(ResourceLoader.CLASSPATH_URL_PREFIX);
    if (nioPathCandidate && !text.startsWith("/")) {
      try {
        URI uri = ResourceUtils.toURI(text);
        if (uri.getScheme() != null) {
          nioPathCandidate = false;
          // Let's try NIO file system providers via Paths.get(URI)
          setValue(Paths.get(uri).normalize());
          return;
        }
      }
      catch (URISyntaxException ex) {
        // Not a valid URI; potentially a Windows-style path after
        // a file prefix (let's try as Framework resource location)
        nioPathCandidate = !text.startsWith(ResourceUtils.FILE_URL_PREFIX);
      }
      catch (FileSystemNotFoundException ex) {
        // URI scheme not registered for NIO (let's try URL
        // protocol handlers via Framework's resource mechanism).
      }
    }

    this.resourceEditor.setAsText(text);
    Resource resource = (Resource) this.resourceEditor.getValue();
    if (resource == null) {
      setValue(null);
    }
    else if (nioPathCandidate && !resource.exists()) {
      setValue(Paths.get(text).normalize());
    }
    else {
      try {
        setValue(resource.getFile().toPath());
      }
      catch (IOException ex) {
        throw new IllegalArgumentException("Failed to retrieve file for " + resource, ex);
      }
    }
  }

  @Override
  public String getAsText() {
    Path value = (Path) getValue();
    return (value != null ? value.toString() : "");
  }

}

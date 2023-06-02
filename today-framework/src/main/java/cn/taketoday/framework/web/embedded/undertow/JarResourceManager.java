/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.framework.web.embedded.undertow;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;
import io.undertow.UndertowMessages;
import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.ResourceChangeListener;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.server.handlers.resource.URLResource;

/**
 * {@link ResourceManager} for JAR resources.
 *
 * @author Ivan Sopov
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Andy Wilkinson
 */
class JarResourceManager implements ResourceManager {

  private final String jarPath;

  JarResourceManager(File jarFile) {
    try {
      this.jarPath = jarFile.getAbsoluteFile().toURI().toURL().toString();
    }
    catch (MalformedURLException ex) {
      throw new IllegalArgumentException(ex);
    }
  }

  @Override
  @Nullable
  public Resource getResource(String path) throws IOException {
    URL url = new URL("jar:" + this.jarPath + "!" + (path.startsWith("/") ? path : "/" + path));
    URLResource resource = new URLResource(url, path);
    if (StringUtils.hasText(path) && !"/".equals(path) && resource.getContentLength() < 0) {
      return null;
    }
    return resource;
  }

  @Override
  public boolean isResourceChangeListenerSupported() {
    return false;
  }

  @Override
  public void registerResourceChangeListener(ResourceChangeListener listener) {
    throw UndertowMessages.MESSAGES.resourceChangeListenerNotSupported();

  }

  @Override
  public void removeResourceChangeListener(ResourceChangeListener listener) {
    throw UndertowMessages.MESSAGES.resourceChangeListenerNotSupported();
  }

  @Override
  public void close() throws IOException {

  }

}

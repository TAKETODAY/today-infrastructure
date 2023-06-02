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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import cn.taketoday.lang.Nullable;
import io.undertow.UndertowMessages;
import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.ResourceChangeListener;
import io.undertow.server.handlers.resource.ResourceManager;

/**
 * A {@link ResourceManager} that delegates to multiple {@code ResourceManager} instances.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
class CompositeResourceManager implements ResourceManager {

  private final List<ResourceManager> resourceManagers;

  CompositeResourceManager(ResourceManager... resourceManagers) {
    this.resourceManagers = Arrays.asList(resourceManagers);
  }

  @Override
  public void close() throws IOException {
    for (ResourceManager resourceManager : this.resourceManagers) {
      resourceManager.close();
    }
  }

  @Override
  @Nullable
  public Resource getResource(String path) throws IOException {
    for (ResourceManager resourceManager : this.resourceManagers) {
      Resource resource = resourceManager.getResource(path);
      if (resource != null) {
        return resource;
      }
    }
    return null;
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

}

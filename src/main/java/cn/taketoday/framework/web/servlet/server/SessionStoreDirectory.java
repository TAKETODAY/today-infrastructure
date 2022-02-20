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

package cn.taketoday.framework.web.servlet.server;

import java.io.File;

import cn.taketoday.framework.ApplicationHome;
import cn.taketoday.framework.ApplicationUtils;
import cn.taketoday.lang.Assert;

/**
 * Manages a session store directory.
 *
 * @author Phillip Webb
 * @see AbstractServletWebServerFactory
 */
class SessionStoreDirectory {

  private File directory;

  File getDirectory() {
    return this.directory;
  }

  void setDirectory(File directory) {
    this.directory = directory;
  }

  File getValidDirectory(boolean mkdirs) {
    File dir = getDirectory();
    if (dir == null) {
      return ApplicationUtils.getTemporalDirectory(null, "servlet-sessions");
    }
    if (!dir.isAbsolute()) {
      dir = new File(new ApplicationHome().getDir(), dir.getPath());
    }
    if (!dir.exists() && mkdirs) {
      dir.mkdirs();
    }
    assertDirectory(mkdirs, dir);
    return dir;
  }

  private void assertDirectory(boolean mkdirs, File dir) {
    Assert.state(!mkdirs || dir.exists(), () -> "Session dir " + dir + " does not exist");
    Assert.state(!dir.isFile(), () -> "Session dir " + dir + " points to a file");
  }

}

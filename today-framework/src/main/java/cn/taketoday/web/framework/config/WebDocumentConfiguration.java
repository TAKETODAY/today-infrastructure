/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.framework.config;

import java.io.File;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.CodeSource;
import java.util.Locale;

import cn.taketoday.context.properties.Props;
import cn.taketoday.core.io.Resource;
import cn.taketoday.framework.Application;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ResourceUtils;

/**
 * @author TODAY 2019-02-05 13:09
 */
@Props(prefix = "server.docs")
public class WebDocumentConfiguration {
  private static final Logger log = LoggerFactory.getLogger(WebDocumentConfiguration.class);

  private static final String[] COMMON_DOC_ROOTS = {
          "src/main/webapp", "src/main/resources", "public", "static", "assets"
  };

  private Resource directory;

  private final Class<?> startupClass;

  public WebDocumentConfiguration(Application application) {
    this.startupClass = application.getMainApplicationClass();
  }

  public Resource getDirectory() {
    return this.directory;
  }

  public void setDirectory(Resource directory) {
    this.directory = directory;
  }

  /**
   * Returns the absolute document root when it points to a valid directory,
   * logging a warning and returning {@code null} otherwise.
   *
   * @return the valid document root
   */
  public Resource getValidDocumentDirectory() {
    Resource resource = this.directory;
    if (resource == null) {
      resource = getJarFileDocBase();
    }
    if (resource == null) {
      resource = getExplodedJarFileDocBase();
    }
    if (resource == null) {
      resource = getCommonDocBase();
    }

    if (resource == null) {
      log.warn("There is no document root directory");
    }
    else {
      log.debug("Document root: [{}]", resource);
    }
    return resource;
  }

  protected Resource getJarFileDocBase() {
    final File archiveFileDocumentRoot = getArchiveFileDocumentRoot(".jar");
    if (archiveFileDocumentRoot == null) {
      return null;
    }
    return ResourceUtils.getResource(archiveFileDocumentRoot);
  }

  protected File getArchiveFileDocumentRoot(String extension) {

    File file = getCodeSourceArchive();
    log.debug("Code archive: [{}]", file);
    if (file != null && file.exists() && !file.isDirectory() && file.getName().toLowerCase(Locale.ENGLISH).endsWith(extension)) {
      return file.getAbsoluteFile();
    }
    return null;
  }

  protected Resource getExplodedJarFileDocBase() {// /WEB-INF
    final File explodedJarFileDocBase = getExplodedJarFileDocBase(getCodeSourceArchive());
    if (explodedJarFileDocBase == null) {
      return null;
    }
    return ResourceUtils.getResource(explodedJarFileDocBase);
  }

  protected File getCodeSourceArchive() {
    return getCodeSourceArchive(startupClass.getProtectionDomain().getCodeSource());
  }

  protected File getCodeSourceArchive(CodeSource codeSource) {
    try {
      if (codeSource == null) {
        return null;
      }

      final URL location = codeSource.getLocation();
      if (location == null) {
        return null;
      }
      String path;
      URLConnection connection = location.openConnection();
      if (connection instanceof JarURLConnection) {
        path = ((JarURLConnection) connection).getJarFile().getName();
      }
      else {
        path = location.getPath();
      }
      int index = path.indexOf("!/");
      if (index != -1) {
        path = path.substring(0, index);
      }
      return new File(path);
    }
    catch (Exception ex) {
      return null;
    }
  }

  public final File getExplodedJarFileDocBase(File codeSourceFile) {

    if (codeSourceFile != null && codeSourceFile.exists()) {
      String path = codeSourceFile.getAbsolutePath();
      int webInfPathIndex = path.indexOf(File.separatorChar + "WEB-INF" + File.separatorChar);
      if (webInfPathIndex >= 0) {
        path = path.substring(0, webInfPathIndex);
        return new File(path);
      }
    }
    return null;
  }

  protected Resource getCommonDocBase() {

    for (String commonDocRoot : COMMON_DOC_ROOTS) {
      File root = new File(commonDocRoot);
      if (root.exists() && root.isDirectory()) {
        return ResourceUtils.getResource(root.getAbsoluteFile());
      }
    }
    return null;
  }

}

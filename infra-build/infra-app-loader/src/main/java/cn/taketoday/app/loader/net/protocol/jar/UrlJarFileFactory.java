/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.app.loader.net.protocol.jar;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Runtime.Version;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Consumer;
import java.util.jar.JarFile;

import cn.taketoday.app.loader.net.protocol.nested.NestedLocation;
import cn.taketoday.app.loader.net.util.UrlDecoder;

/**
 * Factory used by {@link UrlJarFiles} to create {@link JarFile} instances.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see UrlJarFile
 * @see UrlNestedJarFile
 * @since 5.0
 */
class UrlJarFileFactory {

  /**
   * Create a new {@link UrlJarFile} or {@link UrlNestedJarFile} instance.
   *
   * @param jarFileUrl the jar file URL
   * @param closeAction the action to call when the file is closed
   * @return a new {@link JarFile} instance
   * @throws IOException on I/O error
   */
  JarFile createJarFile(URL jarFileUrl, Consumer<JarFile> closeAction) throws IOException {
    Version version = getVersion(jarFileUrl);
    if (isLocalFileUrl(jarFileUrl)) {
      return createJarFileForLocalFile(jarFileUrl, version, closeAction);
    }
    if (isNestedUrl(jarFileUrl)) {
      return createJarFileForNested(jarFileUrl, version, closeAction);
    }
    return createJarFileForStream(jarFileUrl, version, closeAction);
  }

  private Version getVersion(URL url) {
    // The standard JDK handler uses #runtime to indicate that the runtime version
    // should be used. This unfortunately doesn't work for us as
    // jdk.internal.loader.URLClassPath only adds the runtime fragment when the URL
    // is using the internal JDK handler. We need to flip the default to use
    // the runtime version. See gh-38050
    return "base".equals(url.getRef()) ? JarFile.baseVersion() : JarFile.runtimeVersion();
  }

  private boolean isLocalFileUrl(URL url) {
    return url.getProtocol().equalsIgnoreCase("file") && isLocal(url.getHost());
  }

  private boolean isLocal(String host) {
    return host == null || host.isEmpty() || host.equals("~") || host.equalsIgnoreCase("localhost");
  }

  private JarFile createJarFileForLocalFile(URL url, Version version, Consumer<JarFile> closeAction)
          throws IOException {
    String path = UrlDecoder.decode(url.getPath());
    return new UrlJarFile(new File(path), version, closeAction);
  }

  private JarFile createJarFileForNested(URL url, Version version, Consumer<JarFile> closeAction)
          throws IOException {
    NestedLocation location = NestedLocation.fromUrl(url);
    return new UrlNestedJarFile(location.path().toFile(), location.nestedEntryName(), version, closeAction);
  }

  private JarFile createJarFileForStream(URL url, Version version, Consumer<JarFile> closeAction) throws IOException {
    try (InputStream in = url.openStream()) {
      return createJarFileForStream(in, version, closeAction);
    }
  }

  private JarFile createJarFileForStream(InputStream in, Version version, Consumer<JarFile> closeAction)
          throws IOException {
    Path local = Files.createTempFile("jar_cache", null);
    try {
      Files.copy(in, local, StandardCopyOption.REPLACE_EXISTING);
      JarFile jarFile = new UrlJarFile(local.toFile(), version, closeAction);
      local.toFile().deleteOnExit();
      return jarFile;
    }
    catch (Throwable ex) {
      deleteIfPossible(local, ex);
      throw ex;
    }
  }

  private void deleteIfPossible(Path local, Throwable cause) {
    try {
      Files.delete(local);
    }
    catch (IOException ex) {
      cause.addSuppressed(ex);
    }
  }

  static boolean isNestedUrl(URL url) {
    return url.getProtocol().equalsIgnoreCase("nested");
  }

}

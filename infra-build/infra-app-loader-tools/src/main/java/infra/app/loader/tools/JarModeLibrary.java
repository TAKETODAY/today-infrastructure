/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.app.loader.tools;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import infra.lang.Assert;
import infra.util.StringUtils;

/**
 * {@link Library} implementation for internal jarmode jars.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class JarModeLibrary extends Library {

  /**
   * {@link JarModeLibrary} for layer tools.
   */
  public static final JarModeLibrary LAYER_TOOLS = new JarModeLibrary("infra-jarmode-layertools");

  JarModeLibrary(String artifactId) {
    this(createCoordinates(artifactId));
  }

  public JarModeLibrary(LibraryCoordinates coordinates) {
    super(getJarName(coordinates), null, LibraryScope.RUNTIME, coordinates, false, false, true);
  }

  private static LibraryCoordinates createCoordinates(String artifactId) {
    String version = JarModeLibrary.class.getPackage().getImplementationVersion();
    return LibraryCoordinates.of("cn.taketoday", artifactId, version);
  }

  private static String getJarName(LibraryCoordinates coordinates) {
    String version = coordinates.getVersion();
    StringBuilder jarName = new StringBuilder(coordinates.getArtifactId());
    if (StringUtils.hasText(version)) {
      jarName.append('-');
      jarName.append(version);
    }
    jarName.append(".jar");
    return jarName.toString();
  }

  @Override
  public InputStream openStream() throws IOException {
    LibraryCoordinates coordinates = getCoordinates();
    Assert.state(coordinates != null, "'coordinates' is required");
    String path = "META-INF/jarmode/" + coordinates.getArtifactId() + ".jar";
    URL resource = getClass().getClassLoader().getResource(path);
    Assert.state(resource != null, () -> "Unable to find resource " + path);
    return resource.openStream();
  }

  @Override
  long getLastModified() {
    return 0L;
  }

  @Override
  public File getFile() {
    throw new UnsupportedOperationException("Unable to access jar mode library file");
  }

}

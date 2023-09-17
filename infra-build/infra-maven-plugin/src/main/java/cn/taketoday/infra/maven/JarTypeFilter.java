/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.infra.maven;

import org.apache.maven.artifact.Artifact;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * A {@link DependencyFilter} that filters dependencies based on the jar type declared in
 * their manifest.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class JarTypeFilter extends DependencyFilter {

  private static final Set<String> EXCLUDED_JAR_TYPES = Set.of("annotation-processor", "dependencies-starter");

  JarTypeFilter() {
    super(Collections.emptyList());
  }

  @Override
  protected boolean filter(Artifact artifact) {
    try (JarFile jarFile = new JarFile(artifact.getFile())) {
      Manifest manifest = jarFile.getManifest();
      if (manifest != null) {
        String jarType = manifest.getMainAttributes().getValue("Infra-App-Jar-Type");
        if (jarType != null && EXCLUDED_JAR_TYPES.contains(jarType)) {
          return true;
        }
      }
    }
    catch (IOException ex) {
      // Continue
    }
    return false;
  }

}

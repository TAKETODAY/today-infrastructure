/*
 * Copyright 2012 - 2023 the original author or authors.
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

package cn.taketoday.gradle.plugin;

import org.gradle.api.file.FileCollection;
import org.gradle.api.specs.Spec;

import java.io.File;
import java.util.Collections;
import java.util.Set;
import java.util.jar.JarFile;

/**
 * A {@link Spec} for {@link FileCollection#filter(Spec) filtering} {@code FileCollection}
 * to remove jar files based on their {@code Infra-App-Jar-Type} as defined in the
 * manifest. Jars of type {@code dependencies-starter} are excluded.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class JarTypeFileSpec implements Spec<File> {

  private static final Set<String> EXCLUDED_JAR_TYPES = Collections.singleton("dependencies-starter");

  @Override
  public boolean isSatisfiedBy(File file) {
    try (JarFile jar = new JarFile(file)) {
      String jarType = jar.getManifest().getMainAttributes().getValue("Infra-App-Jar-Type");
      if (jarType != null && EXCLUDED_JAR_TYPES.contains(jarType)) {
        return false;
      }
    }
    catch (Exception ex) {
      // Continue
    }
    return true;
  }

}

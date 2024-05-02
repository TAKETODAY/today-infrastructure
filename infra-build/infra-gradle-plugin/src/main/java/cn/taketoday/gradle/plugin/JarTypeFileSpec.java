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

package cn.taketoday.gradle.plugin;

import org.gradle.api.file.FileCollection;
import org.gradle.api.specs.Spec;

import java.io.File;
import java.util.jar.Attributes;
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
  static final Attributes.Name JarTypeName = new Attributes.Name("Infra-App-Jar-Type");

  @Override
  public boolean isSatisfiedBy(File file) {
    try (JarFile jar = new JarFile(file)) {
      String jarType = jar.getManifest().getMainAttributes().getValue(JarTypeName);
      if ("dependencies-starter".equals(jarType)) {
        return false;
      }
    }
    catch (Exception ex) {
      // Continue
    }
    return true;
  }

}

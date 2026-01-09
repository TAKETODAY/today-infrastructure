/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.gradle.plugin;

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

  static JarTypeFileSpec include() {
    return new JarTypeFileSpec();
  }

  static Spec<File> exclude() {
    JarTypeFileSpec include = include();
    return file -> !include.isSatisfiedBy(file);
  }

}

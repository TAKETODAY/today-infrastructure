/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.infra.maven;

import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Find a single Infra Application class match based on directory.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see MainClassFinder
 * @since 4.0
 */
abstract class InfraApplicationClassFinder {

  private static final String INFRA_APPLICATION_CLASS_NAME = "cn.taketoday.framework.InfraApplication";

  static String findSingleClass(File classesDirectory) throws MojoExecutionException {
    return findSingleClass(List.of(classesDirectory));
  }

  static String findSingleClass(List<File> classesDirectories) throws MojoExecutionException {
    try {
      for (File classesDirectory : classesDirectories) {
        String mainClass = MainClassFinder.findSingleMainClass(classesDirectory,
                INFRA_APPLICATION_CLASS_NAME);
        if (mainClass != null) {
          return mainClass;
        }
      }
      throw new MojoExecutionException("Unable to find a suitable main class, please add a 'mainClass' property");
    }
    catch (IOException ex) {
      throw new MojoExecutionException(ex.getMessage(), ex);
    }
  }

}

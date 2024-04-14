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

package cn.taketoday.build.antora;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.RegularFile;

public class GenerateAntoraYmlPlugin implements Plugin<Project> {

  @Override
  public void apply(Project project) {
    project.getTasks().register("generateAntoraYml", GenerateAntoraYmlTask.class, generateAntoraYmlTask -> {
      generateAntoraYmlTask.setGroup("Documentation");
      generateAntoraYmlTask.setDescription("Generates an antora.yml file with information from the build");
      String name = project.getName();
      generateAntoraYmlTask.getComponentName().convention(name);
      String projectVersion = project.getVersion().toString();
      if (!Project.DEFAULT_VERSION.equals(projectVersion)) {
        generateAntoraYmlTask.getVersion().convention(projectVersion);
      }
      RegularFile defaultBaseAntoraYmlFile = project.getLayout().getProjectDirectory().file("antora.yml");
      if (defaultBaseAntoraYmlFile.getAsFile().exists()) {
        generateAntoraYmlTask.getBaseAntoraYmlFile().convention(defaultBaseAntoraYmlFile);
      }
      generateAntoraYmlTask.getOutputFile()
              .convention(project.getLayout().getBuildDirectory().file("generated-antora-resources/antora.yml"));
    });

  }
}
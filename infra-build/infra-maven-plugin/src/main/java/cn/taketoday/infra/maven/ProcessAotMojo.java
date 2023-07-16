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

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import cn.taketoday.util.ObjectUtils;

/**
 * Invoke the AOT engine on the application.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@Mojo(name = "process-aot", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, threadSafe = true,
      requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
      requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class ProcessAotMojo extends AbstractAotMojo {

  private static final String AOT_PROCESSOR_CLASS_NAME = "cn.taketoday.framework.ApplicationAotProcessor";

  /**
   * Directory containing the classes and resource files that should be packaged into
   * the archive.
   */
  @Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
  private File classesDirectory;

  /**
   * Directory containing the generated sources.
   */
  @Parameter(defaultValue = "${project.build.directory}/infra-aot/main/sources", required = true)
  private File generatedSources;

  /**
   * Directory containing the generated resources.
   */
  @Parameter(defaultValue = "${project.build.directory}/infra-aot/main/resources", required = true)
  private File generatedResources;

  /**
   * Directory containing the generated classes.
   */
  @Parameter(defaultValue = "${project.build.directory}/infra-aot/main/classes", required = true)
  private File generatedClasses;

  /**
   * Name of the main class to use as the source for the AOT process. If not specified
   * the first compiled class found that contains a 'main' method will be used.
   */
  @Parameter(property = "infra.aot.main-class")
  private String mainClass;

  /**
   * Application arguments that should be taken into account for AOT processing.
   */
  @Parameter
  private String[] arguments;

  /**
   * Infra profiles to take into account for AOT processing.
   */
  @Parameter
  private String[] profiles;

  @Override
  protected void executeAot() throws Exception {
    String applicationClass = this.mainClass != null
                              ? this.mainClass
                              : InfraApplicationClassFinder.findSingleClass(this.classesDirectory);
    URL[] classPath = getClassPath();
    generateAotAssets(classPath, AOT_PROCESSOR_CLASS_NAME, getAotArguments(applicationClass));
    compileSourceFiles(classPath, this.generatedSources, this.classesDirectory);
    copyAll(this.generatedResources.toPath(), this.classesDirectory.toPath());
    copyAll(this.generatedClasses.toPath(), this.classesDirectory.toPath());
  }

  private String[] getAotArguments(String applicationClass) {
    List<String> aotArguments = new ArrayList<>();
    aotArguments.add(applicationClass);
    aotArguments.add(this.generatedSources.toString());
    aotArguments.add(this.generatedResources.toString());
    aotArguments.add(this.generatedClasses.toString());
    aotArguments.add(this.project.getGroupId());
    aotArguments.add(this.project.getArtifactId());
    aotArguments.addAll(resolveArguments().getArgs());
    return aotArguments.toArray(String[]::new);
  }

  private URL[] getClassPath() throws Exception {
    File[] directories = new File[] { this.classesDirectory, this.generatedClasses };
    return getClassPath(directories, new ExcludeTestScopeArtifactFilter());
  }

  private RunArguments resolveArguments() {
    RunArguments runArguments = new RunArguments(this.arguments);
    if (ObjectUtils.isNotEmpty(this.profiles)) {
      runArguments.getArgs().addFirst("--infra.profiles.active=" + String.join(",", this.profiles));
    }
    return runArguments;
  }

}

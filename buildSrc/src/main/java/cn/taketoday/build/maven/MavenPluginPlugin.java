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

package cn.taketoday.build.maven;

import org.gradle.api.DefaultTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.CopySpec;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.Sync;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.external.javadoc.StandardJavadocDocletOptions;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import cn.taketoday.build.IntegrationTestPlugin;
import io.spring.javaformat.formatter.FileEdit;
import io.spring.javaformat.formatter.FileFormatter;

/**
 * Plugin for building Infra Maven Plugin.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class MavenPluginPlugin implements Plugin<Project> {

  @Override
  public void apply(Project project) {
    project.getPlugins().apply(JavaLibraryPlugin.class);
    project.getPlugins().apply(MavenPublishPlugin.class);
    project.getPlugins().apply(IntegrationTestPlugin.class);

    Jar jarTask = (Jar) project.getTasks().getByName(JavaPlugin.JAR_TASK_NAME);
    configurePomPackaging(project);
    MavenExec generateHelpMojoTask = addGenerateHelpMojoTask(project, jarTask);
    MavenExec generatePluginDescriptorTask = addGeneratePluginDescriptorTask(
            project, jarTask, generateHelpMojoTask);
    addDocumentPluginGoalsTask(project, generatePluginDescriptorTask);
  }

  private void configurePomPackaging(Project project) {
    PublishingExtension publishing = project.getExtensions().getByType(PublishingExtension.class);
    publishing.getPublications().withType(MavenPublication.class, this::setPackaging);
  }

  private void setPackaging(MavenPublication mavenPublication) {
    mavenPublication.pom(pom -> pom.setPackaging("maven-plugin"));
  }

  private void addDocumentPluginGoalsTask(Project project, MavenExec generatePluginDescriptorTask) {
    DocumentPluginGoals task = project.getTasks().create("documentPluginGoals", DocumentPluginGoals.class);
    File pluginXml = new File(generatePluginDescriptorTask.getOutputs().getFiles().getSingleFile(), "plugin.xml");
    task.setPluginXml(pluginXml);
    task.setOutputDir(new File(project.getBuildDir(), "docs/generated/goals/"));
    task.dependsOn(generatePluginDescriptorTask);
  }

  private MavenExec addGenerateHelpMojoTask(Project project, Jar jarTask) {
    File helpMojoDir = new File(project.getBuildDir(), "help-mojo");
    MavenExec task = createGenerateHelpMojoTask(project, helpMojoDir);
    task.dependsOn(createSyncHelpMojoInputsTask(project, helpMojoDir));
    includeHelpMojoInJar(jarTask, task);
    return task;
  }

  private MavenExec createGenerateHelpMojoTask(Project project, File helpMojoDir) {
    MavenExec task = project.getTasks().create("generateHelpMojo", MavenExec.class);
    task.setProjectDir(helpMojoDir);
    task.args("org.apache.maven.plugins:maven-plugin-plugin:3.6.1:helpmojo");
    task.getOutputs().dir(new File(helpMojoDir, "target/generated-sources/plugin"));
    return task;
  }

  private Sync createSyncHelpMojoInputsTask(Project project, File helpMojoDir) {
    Sync task = project.getTasks().create("syncHelpMojoInputs", Sync.class);
    task.setDestinationDir(helpMojoDir);
    File pomFile = new File(project.getProjectDir(), "src/maven/resources/pom.xml");
    task.from(pomFile, copy -> replaceVersionPlaceholder(copy, project));
    return task;
  }

  private void includeHelpMojoInJar(Jar jarTask, JavaExec generateHelpMojoTask) {
    jarTask.from(generateHelpMojoTask).exclude("**/*.java");
    jarTask.dependsOn(generateHelpMojoTask);
  }

  private MavenExec addGeneratePluginDescriptorTask(Project project, Jar jarTask, MavenExec generateHelpMojoTask) {
    File pluginDescriptorDir = new File(project.getBuildDir(), "plugin-descriptor");
    File generatedHelpMojoDir = new File(project.getBuildDir(), "generated/sources/helpMojo");
    SourceSet mainSourceSet = getMainSourceSet(project);
    project.getTasks().withType(Javadoc.class, this::setJavadocOptions);

    FormatHelpMojoSource formattedHelpMojoSource = createFormatHelpMojoSource(project,
            generateHelpMojoTask, generatedHelpMojoDir);

    project.getTasks().getByName(mainSourceSet.getCompileJavaTaskName()).dependsOn(formattedHelpMojoSource);

    mainSourceSet.java(javaSources -> javaSources.srcDir(formattedHelpMojoSource));
    Sync pluginDescriptorInputs = createSyncPluginDescriptorInputs(project, pluginDescriptorDir, mainSourceSet);
    pluginDescriptorInputs.dependsOn(mainSourceSet.getClassesTaskName());
    MavenExec task = createGeneratePluginDescriptorTask(project, pluginDescriptorDir);
    task.dependsOn(pluginDescriptorInputs);
    includeDescriptorInJar(jarTask, task);
    return task;
  }

  private SourceSet getMainSourceSet(Project project) {
    SourceSetContainer sourceSets = project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets();
    return sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
  }

  private void setJavadocOptions(Javadoc javadoc) {
    StandardJavadocDocletOptions options = (StandardJavadocDocletOptions) javadoc.getOptions();
    options.addMultilineStringsOption("tag").setValue(Arrays.asList("goal:X", "requiresProject:X", "threadSafe:X"));
  }

  private FormatHelpMojoSource createFormatHelpMojoSource(Project project,
          MavenExec generateHelpMojoTask, File generatedHelpMojoDir) {
    FormatHelpMojoSource formatHelpMojoSource = project.getTasks()
            .create("formatHelpMojoSource", FormatHelpMojoSource.class);
    formatHelpMojoSource.setGenerator(generateHelpMojoTask);
    formatHelpMojoSource.setOutputDir(generatedHelpMojoDir);
    return formatHelpMojoSource;
  }

  private Sync createSyncPluginDescriptorInputs(Project project, File destination, SourceSet sourceSet) {
    Sync pluginDescriptorInputs = project.getTasks().create("syncPluginDescriptorInputs", Sync.class);
    pluginDescriptorInputs.setDestinationDir(destination);
    File pomFile = new File(project.getProjectDir(), "src/maven/resources/pom.xml");
    pluginDescriptorInputs.from(pomFile, copy -> replaceVersionPlaceholder(copy, project));
    pluginDescriptorInputs.from(sourceSet.getOutput().getClassesDirs(), (sync) -> sync.into("target/classes"));
    pluginDescriptorInputs.from(sourceSet.getAllJava().getSrcDirs(), (sync) -> sync.into("src/main/java"));
    pluginDescriptorInputs.getInputs().property("version", project.getVersion());
    return pluginDescriptorInputs;
  }

  private MavenExec createGeneratePluginDescriptorTask(Project project, File mavenDir) {
    MavenExec generatePluginDescriptor = project.getTasks().create("generatePluginDescriptor", MavenExec.class);
    generatePluginDescriptor.args("org.apache.maven.plugins:maven-plugin-plugin:3.6.1:descriptor");
    generatePluginDescriptor.getOutputs().dir(new File(mavenDir, "target/classes/META-INF/maven"));
    generatePluginDescriptor.getInputs()
            .dir(new File(mavenDir, "target/classes/cn"))
            .withPathSensitivity(PathSensitivity.RELATIVE)
            .withPropertyName("plugin classes");
    generatePluginDescriptor.setProjectDir(mavenDir);
    return generatePluginDescriptor;
  }

  private void includeDescriptorInJar(Jar jar, JavaExec generatePluginDescriptorTask) {
    jar.from(generatePluginDescriptorTask, copy -> copy.into("META-INF/maven/"));
    jar.dependsOn(generatePluginDescriptorTask);
  }

  private void replaceVersionPlaceholder(CopySpec copy, Project project) {
    copy.filter(input -> replaceVersionPlaceholder(project, input));
  }

  private String replaceVersionPlaceholder(Project project, String input) {
    return input.replace("{{version}}", project.getVersion().toString());
  }

  public static class FormatHelpMojoSource extends DefaultTask {

    private Task generator;

    private File outputDir;

    void setGenerator(Task generator) {
      this.generator = generator;
      getInputs().files(this.generator)
              .withPathSensitivity(PathSensitivity.RELATIVE)
              .withPropertyName("generated source");
    }

    @OutputDirectory
    public File getOutputDir() {
      return this.outputDir;
    }

    void setOutputDir(File outputDir) {
      this.outputDir = outputDir;
    }

    @TaskAction
    void syncAndFormat() {
      FileFormatter formatter = new FileFormatter();
      for (File output : this.generator.getOutputs().getFiles()) {
        formatter.formatFiles(getProject().fileTree(output), StandardCharsets.UTF_8)
                .forEach(edit -> save(output, edit));
      }
    }

    private void save(File output, FileEdit edit) {
      Path relativePath = output.toPath().relativize(edit.getFile().toPath());
      Path outputLocation = this.outputDir.toPath().resolve(relativePath);
      try {
        Files.createDirectories(outputLocation.getParent());
        Files.writeString(outputLocation, edit.getFormattedContent());
      }
      catch (Exception ex) {
        throw new TaskExecutionException(this, ex);
      }
    }

  }

}

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

package cn.taketoday.build.maven;

import org.gradle.api.DefaultTask;
import org.gradle.api.Task;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import cn.taketoday.build.maven.PluginXmlParser.Mojo;
import cn.taketoday.build.maven.PluginXmlParser.Parameter;
import cn.taketoday.build.maven.PluginXmlParser.Plugin;

/**
 * A {@link Task} to document the plugin's goals.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class DocumentPluginGoals extends DefaultTask {

  private final PluginXmlParser parser = new PluginXmlParser();

  private File pluginXml;

  private File outputDir;

  @OutputDirectory
  public File getOutputDir() {
    return this.outputDir;
  }

  public void setOutputDir(File outputDir) {
    this.outputDir = outputDir;
  }

  @InputFile
  public File getPluginXml() {
    return this.pluginXml;
  }

  public void setPluginXml(File pluginXml) {
    this.pluginXml = pluginXml;
  }

  @TaskAction
  public void documentPluginGoals() throws IOException {
    Plugin plugin = this.parser.parse(this.pluginXml);
    writeOverview(plugin);
    for (Mojo mojo : plugin.getMojos()) {
      documentMojo(plugin, mojo);
    }
  }

  private void writeOverview(Plugin plugin) throws IOException {
    try (PrintWriter writer = new PrintWriter(new FileWriter(new File(this.outputDir, "overview.adoc")))) {
      writer.println("[cols=\"1,3\"]");
      writer.println("|===");
      writer.println("| Goal | Description");
      writer.println();
      for (Mojo mojo : plugin.getMojos()) {
        writer.printf("| <<goals-%s,%s:%s>>%n", mojo.getGoal(), plugin.getGoalPrefix(), mojo.getGoal());
        writer.printf("| %s%n", mojo.getDescription());
        writer.println();
      }
      writer.println("|===");
    }
  }

  private void documentMojo(Plugin plugin, Mojo mojo) throws IOException {
    try (PrintWriter writer = new PrintWriter(new FileWriter(new File(this.outputDir, mojo.getGoal() + ".adoc")))) {
      String sectionId = "goals-" + mojo.getGoal();
      writer.println();
      writer.println();
      writer.printf("[[%s]]%n", sectionId);
      writer.printf("= `%s:%s`%n", plugin.getGoalPrefix(), mojo.getGoal());
      writer.printf("`%s:%s:%s`%n", plugin.getGroupId(), plugin.getArtifactId(), plugin.getVersion());
      writer.println();
      writer.println(mojo.getDescription());
      List<Parameter> parameters = mojo.getParameters().stream().filter(Parameter::isEditable).toList();
      List<Parameter> requiredParameters = parameters.stream().filter(Parameter::isRequired).toList();
      String parametersSectionId = sectionId + "-parameters";
      String detailsSectionId = parametersSectionId + "-details";
      if (!requiredParameters.isEmpty()) {
        writer.println();
        writer.println();
        writer.printf("[[%s-required]]%n", parametersSectionId);
        writer.println("== Required parameters");
        writeParametersTable(writer, detailsSectionId, requiredParameters);
      }
      List<Parameter> optionalParameters = parameters.stream()
              .filter((parameter) -> !parameter.isRequired())
              .toList();
      if (!optionalParameters.isEmpty()) {
        writer.println();
        writer.println();
        writer.printf("[[%s-optional]]%n", parametersSectionId);
        writer.println("== Optional parameters");
        writeParametersTable(writer, detailsSectionId, optionalParameters);
      }
      writer.println();
      writer.println();
      writer.printf("[[%s]]%n", detailsSectionId);
      writer.println("== Parameter details");
      writeParameterDetails(writer, parameters, detailsSectionId);
    }
  }

  private void writeParametersTable(PrintWriter writer, String detailsSectionId, List<Parameter> parameters) {
    writer.println("[cols=\"3,2,3\"]");
    writer.println("|===");
    writer.println("| Name | Type | Default");
    writer.println();
    for (Parameter parameter : parameters) {
      String name = parameter.getName();
      writer.printf("| <<%s-%s,%s>>%n", detailsSectionId, name, name);
      writer.printf("| `%s`%n", typeNameToJavadocLink(shortTypeName(parameter.getType()), parameter.getType()));
      String defaultValue = parameter.getDefaultValue();
      if (defaultValue != null) {
        writer.printf("| `%s`%n", defaultValue);
      }
      else {
        writer.println("|");
      }
      writer.println();
    }
    writer.println("|===");
  }

  private void writeParameterDetails(PrintWriter writer, List<Parameter> parameters, String sectionId) {
    for (Parameter parameter : parameters) {
      String name = parameter.getName();
      writer.println();
      writer.println();
      writer.printf("[[%s-%s]]%n", sectionId, name);
      writer.printf("=== `%s`%n", name);
      writer.println(parameter.getDescription());
      writer.println();
      writer.println("[cols=\"10h,90\"]");
      writer.println("|===");
      writer.println();
      writeDetail(writer, "Name", name);
      writeDetail(writer, "Type", typeNameToJavadocLink(parameter.getType()));
      writeOptionalDetail(writer, "Default value", parameter.getDefaultValue());
      writeOptionalDetail(writer, "User property", parameter.getUserProperty());
      writeOptionalDetail(writer, "Since", parameter.getSince());
      writer.println("|===");
    }
  }

  private void writeDetail(PrintWriter writer, String name, String value) {
    writer.printf("| %s%n", name);
    writer.printf("| `%s`%n", value);
    writer.println();
  }

  private void writeOptionalDetail(PrintWriter writer, String name, String value) {
    writer.printf("| %s%n", name);
    if (value != null) {
      writer.printf("| `%s`%n", value);
    }
    else {
      writer.println("|");
    }
    writer.println();
  }

  private String shortTypeName(String name) {
    if (name.lastIndexOf('.') >= 0) {
      name = name.substring(name.lastIndexOf('.') + 1);
    }
    if (name.lastIndexOf('$') >= 0) {
      name = name.substring(name.lastIndexOf('$') + 1);
    }
    return name;
  }

  private String typeNameToJavadocLink(String name) {
    return typeNameToJavadocLink(name, name);
  }

  private String typeNameToJavadocLink(String shortName, String name) {
    if (name.startsWith("cn.taketoday.maven")) {
      return "{infra-docs}/maven-plugin/api/" + typeNameToJavadocPath(name) + ".html[" + shortName + "]";
    }
    if (name.startsWith("cn.taketoday.infra")) {
      return "{infra-docs}/api/" + typeNameToJavadocPath(name) + ".html[" + shortName + "]";
    }
    return shortName;
  }

  private String typeNameToJavadocPath(String name) {
    return name.replace(".", "/").replace("$", ".");
  }

}

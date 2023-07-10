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

package cn.taketoday.infra.maven;

import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.util.Arrays;

/**
 * Provides access to the Maven Java Compiler plugin configuration.
 *
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class JavaCompilerPluginConfiguration {

  private final MavenProject project;

  JavaCompilerPluginConfiguration(MavenProject project) {
    this.project = project;
  }

  String getSourceMajorVersion() {
    String version = getConfigurationValue("source");

    if (version == null) {
      version = getPropertyValue("maven.compiler.source");
    }

    return majorVersionFor(version);
  }

  String getTargetMajorVersion() {
    String version = getConfigurationValue("target");

    if (version == null) {
      version = getPropertyValue("maven.compiler.target");
    }

    return majorVersionFor(version);
  }

  String getReleaseVersion() {
    String version = getConfigurationValue("release");

    if (version == null) {
      version = getPropertyValue("maven.compiler.release");
    }

    return majorVersionFor(version);
  }

  private String getConfigurationValue(String propertyName) {
    Plugin plugin = this.project.getPlugin("org.apache.maven.plugins:maven-compiler-plugin");
    if (plugin != null) {
      Object pluginConfiguration = plugin.getConfiguration();
      if (pluginConfiguration instanceof Xpp3Dom dom) {
        return getNodeValue(dom, propertyName);
      }
    }
    return null;
  }

  private String getPropertyValue(String propertyName) {
    if (this.project.getProperties().containsKey(propertyName)) {
      return this.project.getProperties().get(propertyName).toString();
    }
    return null;
  }

  private String getNodeValue(Xpp3Dom dom, String... childNames) {
    Xpp3Dom childNode = dom.getChild(childNames[0]);

    if (childNode == null) {
      return null;
    }

    if (childNames.length > 1) {
      return getNodeValue(childNode, Arrays.copyOfRange(childNames, 1, childNames.length));
    }

    return childNode.getValue();
  }

  private String majorVersionFor(String version) {
    if (version != null && version.startsWith("1.")) {
      return version.substring("1.".length());
    }
    return version;
  }

}

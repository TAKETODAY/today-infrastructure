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
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JavaCompilerPluginConfiguration}.
 *
 * @author Scott Frederick
 */
class JavaCompilerPluginConfigurationTests {

  private MavenProject project;

  private Plugin plugin;

  @BeforeEach
  void setUp() {
    this.project = mock(MavenProject.class);
    this.plugin = mock(Plugin.class);
    given(this.project.getPlugin(anyString())).willReturn(this.plugin);
  }

  @Test
  void versionsAreNullWithNoConfiguration() {
    given(this.plugin.getConfiguration()).willReturn(null);
    given(this.project.getProperties()).willReturn(new Properties());
    JavaCompilerPluginConfiguration configuration = new JavaCompilerPluginConfiguration(this.project);
    assertThat(configuration.getSourceMajorVersion()).isNull();
    assertThat(configuration.getTargetMajorVersion()).isNull();
    assertThat(configuration.getReleaseVersion()).isNull();
  }

  @Test
  void versionsAreReturnedFromConfiguration() throws IOException, XmlPullParserException {
    Xpp3Dom dom = buildConfigurationDom("<source>1.9</source>", "<target>11</target>", "<release>12</release>");
    given(this.plugin.getConfiguration()).willReturn(dom);
    Properties properties = new Properties();
    properties.setProperty("maven.compiler.source", "1.8");
    properties.setProperty("maven.compiler.target", "10");
    properties.setProperty("maven.compiler.release", "11");
    given(this.project.getProperties()).willReturn(properties);
    JavaCompilerPluginConfiguration configuration = new JavaCompilerPluginConfiguration(this.project);
    assertThat(configuration.getSourceMajorVersion()).isEqualTo("9");
    assertThat(configuration.getTargetMajorVersion()).isEqualTo("11");
    assertThat(configuration.getReleaseVersion()).isEqualTo("12");
  }

  @Test
  void versionsAreReturnedFromProperties() {
    given(this.plugin.getConfiguration()).willReturn(null);
    Properties properties = new Properties();
    properties.setProperty("maven.compiler.source", "1.8");
    properties.setProperty("maven.compiler.target", "11");
    properties.setProperty("maven.compiler.release", "12");
    given(this.project.getProperties()).willReturn(properties);
    JavaCompilerPluginConfiguration configuration = new JavaCompilerPluginConfiguration(this.project);
    assertThat(configuration.getSourceMajorVersion()).isEqualTo("8");
    assertThat(configuration.getTargetMajorVersion()).isEqualTo("11");
    assertThat(configuration.getReleaseVersion()).isEqualTo("12");
  }

  private Xpp3Dom buildConfigurationDom(String... properties) throws IOException, XmlPullParserException {
    return Xpp3DomBuilder
            .build(new StringReader("<configuration>" + Arrays.toString(properties) + "</configuration>"));
  }

}

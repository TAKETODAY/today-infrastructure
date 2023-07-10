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

package cn.taketoday.gradle.tasks.bundling;

import org.gradle.api.Project;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link LaunchScriptConfiguration}.
 *
 * @author Andy Wilkinson
 */
class LaunchScriptConfigurationTests {

  private final AbstractArchiveTask task = mock(AbstractArchiveTask.class);

  private final Project project = mock(Project.class);

  @BeforeEach
  void setUp() {
    given(this.task.getProject()).willReturn(this.project);
  }

  @Test
  void initInfoProvidesUsesArchiveBaseNameByDefault() {
    Property<String> baseName = stringProperty("base-name");
    given(this.task.getArchiveBaseName()).willReturn(baseName);
    assertThat(new LaunchScriptConfiguration(this.task).getProperties()).containsEntry("initInfoProvides",
            "base-name");
  }

  @Test
  void initInfoShortDescriptionUsesDescriptionByDefault() {
    given(this.project.getDescription()).willReturn("Project description");
    Property<String> baseName = stringProperty("base-name");
    given(this.task.getArchiveBaseName()).willReturn(baseName);
    assertThat(new LaunchScriptConfiguration(this.task).getProperties()).containsEntry("initInfoShortDescription",
            "Project description");
  }

  @Test
  void initInfoShortDescriptionUsesArchiveBaseNameWhenDescriptionIsNull() {
    Property<String> baseName = stringProperty("base-name");
    given(this.task.getArchiveBaseName()).willReturn(baseName);
    assertThat(new LaunchScriptConfiguration(this.task).getProperties()).containsEntry("initInfoShortDescription",
            "base-name");
  }

  @Test
  void initInfoShortDescriptionUsesSingleLineVersionOfMultiLineProjectDescription() {
    given(this.project.getDescription()).willReturn("Project\ndescription");
    Property<String> baseName = stringProperty("base-name");
    given(this.task.getArchiveBaseName()).willReturn(baseName);
    assertThat(new LaunchScriptConfiguration(this.task).getProperties()).containsEntry("initInfoShortDescription",
            "Project description");
  }

  @Test
  void initInfoDescriptionUsesArchiveBaseNameWhenDescriptionIsNull() {
    Property<String> baseName = stringProperty("base-name");
    given(this.task.getArchiveBaseName()).willReturn(baseName);
    assertThat(new LaunchScriptConfiguration(this.task).getProperties()).containsEntry("initInfoDescription",
            "base-name");
  }

  @Test
  void initInfoDescriptionUsesProjectDescriptionByDefault() {
    given(this.project.getDescription()).willReturn("Project description");
    Property<String> baseName = stringProperty("base-name");
    given(this.task.getArchiveBaseName()).willReturn(baseName);
    assertThat(new LaunchScriptConfiguration(this.task).getProperties()).containsEntry("initInfoDescription",
            "Project description");
  }

  @Test
  void initInfoDescriptionUsesCorrectlyFormattedMultiLineProjectDescription() {
    given(this.project.getDescription()).willReturn("The\nproject\ndescription");
    Property<String> baseName = stringProperty("base-name");
    given(this.task.getArchiveBaseName()).willReturn(baseName);
    assertThat(new LaunchScriptConfiguration(this.task).getProperties()).containsEntry("initInfoDescription",
            "The\n#  project\n#  description");
  }

  @SuppressWarnings("unchecked")
  private Property<String> stringProperty(String value) {
    Property<String> property = mock(Property.class);
    given(property.get()).willReturn(value);
    return property;
  }

}

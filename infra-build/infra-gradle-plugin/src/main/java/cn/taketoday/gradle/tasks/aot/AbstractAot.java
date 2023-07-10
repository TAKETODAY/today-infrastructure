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

package cn.taketoday.gradle.tasks.aot;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.work.DisableCachingByDefault;

import java.util.ArrayList;
import java.util.List;

/**
 * Specialization of {@link JavaExec} to be used as a base class for tasks that perform
 * ahead-of-time processing.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@DisableCachingByDefault(because = "Cacheability can only be determined by a concrete implementation")
public abstract class AbstractAot extends JavaExec {

  private final DirectoryProperty sourcesDir;

  private final DirectoryProperty resourcesDir;

  private final DirectoryProperty classesDir;

  private final Property<String> groupId;

  private final Property<String> artifactId;

  protected AbstractAot() {
    this.sourcesDir = getProject().getObjects().directoryProperty();
    this.resourcesDir = getProject().getObjects().directoryProperty();
    this.classesDir = getProject().getObjects().directoryProperty();
    this.groupId = getProject().getObjects().property(String.class);
    this.artifactId = getProject().getObjects().property(String.class);
  }

  /**
   * The group ID of the application that is to be processed ahead-of-time.
   *
   * @return the group ID property
   */
  @Input
  public final Property<String> getGroupId() {
    return this.groupId;
  }

  /**
   * The artifact ID of the application that is to be processed ahead-of-time.
   *
   * @return the artifact ID property
   */
  @Input
  public final Property<String> getArtifactId() {
    return this.artifactId;
  }

  /**
   * The directory to which AOT-generated sources should be written.
   *
   * @return the sources directory property
   */
  @OutputDirectory
  public final DirectoryProperty getSourcesOutput() {
    return this.sourcesDir;
  }

  /**
   * The directory to which AOT-generated resources should be written.
   *
   * @return the resources directory property
   */
  @OutputDirectory
  public final DirectoryProperty getResourcesOutput() {
    return this.resourcesDir;
  }

  /**
   * The directory to which AOT-generated classes should be written.
   *
   * @return the classes directory property
   */
  @OutputDirectory
  public final DirectoryProperty getClassesOutput() {
    return this.classesDir;
  }

  List<String> processorArgs() {
    List<String> args = new ArrayList<>();
    args.add(getSourcesOutput().getAsFile().get().getAbsolutePath());
    args.add(getResourcesOutput().getAsFile().get().getAbsolutePath());
    args.add(getClassesOutput().getAsFile().get().getAbsolutePath());
    args.add(getGroupId().get());
    args.add(getArtifactId().get());
    args.addAll(super.getArgs());
    return args;
  }

}

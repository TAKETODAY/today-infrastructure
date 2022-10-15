/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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
package cn.taketoday.maven;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;

/**
 * Same as <code>prepare-agent</code>, but provides default values suitable for
 * integration-tests:
 * <ul>
 * <li>bound to <code>pre-integration-test</code> phase</li>
 * <li>different <code>destFile</code></li>
 * </ul>
 *
 * @since 0.6.4
 */
@Mojo(name = "prepare-agent-integration", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST, requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
public class AgentITMojo extends AbstractAgentMojo {

  /**
   * Path to the output file for execution data.
   */
  @Parameter(property = "jacoco.destFile", defaultValue = "${project.build.directory}/jacoco-it.exec")
  private File destFile;

  /**
   * @return the destFile
   */
  @Override
  File getDestFile() {
    return destFile;
  }

}

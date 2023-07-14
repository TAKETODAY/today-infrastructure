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

package cn.taketoday.build.hint;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.process.CommandLineArgumentProvider;

import java.util.Collections;

/**
 * Argument provider for registering the runtime hints agent with a Java process.
 */
public interface RuntimeHintsAgentArgumentProvider extends CommandLineArgumentProvider {

  @Classpath
  ConfigurableFileCollection getAgentJar();

  @Input
  SetProperty<String> getIncludedPackages();

  @Input
  SetProperty<String> getExcludedPackages();

  @Override
  default Iterable<String> asArguments() {
    StringBuilder packages = new StringBuilder();
    getIncludedPackages().get().forEach(packageName -> packages.append('+').append(packageName).append(','));
    getExcludedPackages().get().forEach(packageName -> packages.append('-').append(packageName).append(','));
    return Collections.singleton("-javaagent:" + getAgentJar().getSingleFile() + "=" + packages);
  }
}
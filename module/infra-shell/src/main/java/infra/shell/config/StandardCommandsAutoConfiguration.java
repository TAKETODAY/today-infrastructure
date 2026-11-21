/*
 * Copyright 2017-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.shell.config;

import infra.app.info.BuildProperties;
import infra.app.info.GitProperties;
import infra.beans.factory.ObjectProvider;
import infra.context.annotation.Bean;
import infra.context.annotation.config.AutoConfiguration;
import infra.context.condition.ConditionalOnProperty;
import infra.context.properties.EnableConfigurationProperties;
import infra.shell.command.Clear;
import infra.shell.command.Command;
import infra.shell.command.CommandRegistry;
import infra.shell.command.Help;
import infra.shell.command.Script;
import infra.shell.command.Version;

/**
 * Creates beans for standard commands.
 *
 * @author Eric Bottard
 * @author Mahmoud Ben Hassine
 * @author Piotr Olaszewski
 */
@AutoConfiguration
@EnableConfigurationProperties(InfraShellProperties.class)
public class StandardCommandsAutoConfiguration {

  @Bean
  @ConditionalOnProperty(value = "shell.command.help.enabled", havingValue = "true", matchIfMissing = true)
  public Command helpCommand() {
    return new Help();
  }

  @Bean
  @ConditionalOnProperty(value = "shell.command.clear.enabled", havingValue = "true", matchIfMissing = true)
  public Command clearCommand() {
    return new Clear();
  }

  @Bean
  @ConditionalOnProperty(value = "shell.command.version.enabled", havingValue = "true", matchIfMissing = true)
  public Command versionCommand(InfraShellProperties shellProperties,
          ObjectProvider<BuildProperties> buildProperties, ObjectProvider<GitProperties> gitProperties) {
    InfraShellProperties.VersionCommand properties = shellProperties.getCommand().getVersion();
    Version version = new Version();

    buildProperties.ifAvailable(props -> version
            .setBuildProperties(new Version.BuildProperties(properties.isShowBuildGroup() ? props.getGroup() : null,
                    properties.isShowBuildArtifact() ? props.getArtifact() : null,
                    properties.isShowBuildName() ? props.getName() : null,
                    properties.isShowBuildVersion() ? props.getVersion() : null,
                    properties.isShowBuildTime() ? props.getTime() : null)));

    gitProperties.ifAvailable(props -> version
            .setGitProperties(new Version.GitProperties(properties.isShowGitBranch() ? props.getBranch() : null,
                    properties.isShowGitCommitId() ? props.getCommitId() : null,
                    properties.isShowGitShortCommitId() ? props.getShortCommitId() : null,
                    properties.isShowGitCommitTime() ? props.getCommitTime() : null)));

    return version;
  }

  @Bean
  @ConditionalOnProperty(value = "shell.command.script.enabled", havingValue = "true", matchIfMissing = true)
  public Command scriptCommand(CommandRegistry commandRegistry) {
    return new Script(commandRegistry);
  }

}

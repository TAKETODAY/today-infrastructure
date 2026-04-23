/*
 * Copyright 2012-present the original author or authors.
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

package infra.app.config.info;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Properties;

import infra.app.info.BuildProperties;
import infra.app.info.GitProperties;
import infra.context.annotation.ConditionContext;
import infra.context.annotation.Conditional;
import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.annotation.config.EnableAutoConfiguration;
import infra.context.condition.ConditionMessage;
import infra.context.condition.ConditionOutcome;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.condition.ConditionalOnResource;
import infra.context.condition.InfraCondition;
import infra.context.properties.EnableConfigurationProperties;
import infra.core.env.Environment;
import infra.core.io.EncodedResource;
import infra.core.io.PropertiesUtils;
import infra.core.io.Resource;
import infra.core.io.ResourceLoader;
import infra.core.type.AnnotatedTypeMetadata;
import infra.stereotype.Component;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for various project information.
 *
 * @author Stephane Nicoll
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
@DisableDIAutoConfiguration
@EnableConfigurationProperties(ProjectInfoProperties.class)
public class ProjectInfoAutoConfiguration {

  @Component
  @Conditional(GitResourceAvailableCondition.class)
  @ConditionalOnMissingBean
  public static GitProperties gitProperties(ProjectInfoProperties properties) throws Exception {
    return new GitProperties(
            loadFrom(properties.git.location, "git", properties.git.encoding));
  }

  @Component
  @ConditionalOnResource("${project.info.build.location:classpath:META-INF/build-info.properties}")
  @ConditionalOnMissingBean
  public static BuildProperties buildProperties(ProjectInfoProperties properties) throws Exception {
    return new BuildProperties(
            loadFrom(properties.build.location, "build", properties.build.encoding));
  }

  private static Properties loadFrom(Resource location, String prefix, Charset encoding) throws IOException {
    prefix = prefix.endsWith(".") ? prefix : prefix + ".";
    Properties source = loadSource(location, encoding);
    Properties target = new Properties();
    for (String key : source.stringPropertyNames()) {
      if (key.startsWith(prefix)) {
        target.put(key.substring(prefix.length()), source.get(key));
      }
    }
    return target;
  }

  private static Properties loadSource(Resource location, @Nullable Charset encoding) throws IOException {
    if (encoding != null) {
      return PropertiesUtils.loadProperties(new EncodedResource(location, encoding));
    }
    return PropertiesUtils.loadProperties(location);
  }

  static class GitResourceAvailableCondition extends InfraCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
      ResourceLoader loader = context.getResourceLoader();
      Environment environment = context.getEnvironment();
      String location = environment.getProperty("project.info.git.location");
      if (location == null) {
        location = "classpath:git.properties";
      }
      ConditionMessage.Builder message = ConditionMessage.forCondition("GitResource");
      if (loader.getResource(location).exists()) {
        return ConditionOutcome.match(message.found("git info at").items(location));
      }
      return ConditionOutcome.noMatch(message.didNotFind("git info at").items(location));
    }

  }

}

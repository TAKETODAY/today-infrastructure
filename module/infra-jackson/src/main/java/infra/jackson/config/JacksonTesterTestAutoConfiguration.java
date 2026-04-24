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

package infra.jackson.config;

import infra.app.test.config.json.ConditionalOnJsonTesters;
import infra.app.test.config.json.JsonMarshalTesterRuntimeHints;
import infra.app.test.config.json.JsonTesterFactoryBean;
import infra.app.test.json.JacksonTester;
import infra.beans.factory.FactoryBean;
import infra.context.annotation.ImportRuntimeHints;
import infra.context.annotation.config.AutoConfiguration;
import infra.context.annotation.config.EnableAutoConfiguration;
import infra.context.condition.ConditionalOnBean;
import infra.stereotype.Prototype;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link JacksonTester}.
 *
 * @author Phillip Webb
 */
@AutoConfiguration(after = JacksonAutoConfiguration.class)
@ConditionalOnJsonTesters
final class JacksonTesterTestAutoConfiguration {

  @Prototype
  @ConditionalOnBean(JsonMapper.class)
  @ImportRuntimeHints(JacksonTesterRuntimeHints.class)
  static FactoryBean<JacksonTester<?>> jacksonTesterFactoryBean(JsonMapper mapper) {
    return new JsonTesterFactoryBean<>(JacksonTester.class, mapper);
  }

  static class JacksonTesterRuntimeHints extends JsonMarshalTesterRuntimeHints {

    JacksonTesterRuntimeHints() {
      super(JacksonTester.class);
    }

  }

}

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

package infra.app.test.config.json;

import org.junit.jupiter.api.Test;
import infra.beans.factory.annotation.Autowired;
import infra.app.json.test.config.app.ExampleJsonApplication;
import infra.app.test.context.InfraTest;
import infra.app.test.json.BasicJsonTester;
import infra.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link InfraTest @InfraTest} with
 * {@link AutoConfigureJsonTesters @AutoConfigureJsonTesters}.
 *
 * @author Andy Wilkinson
 */
@InfraTest
@AutoConfigureJsonTesters
@ContextConfiguration(classes = ExampleJsonApplication.class)
class InfraTestWithAutoConfigureJsonTestersTests {

	@Autowired
	private BasicJsonTester basicJson;

	@Test
	void contextLoads() {
		assertThat(this.basicJson).isNotNull();
	}

}

/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.app.logging.structured;

import org.junit.jupiter.api.Test;

import infra.app.json.JsonWriter;
import infra.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ElasticCommonSchemaService}.
 *
 * @author Phillip Webb
 * @author Moritz Halbritter
 */
class ElasticCommonSchemaServiceTests {

	@Test
	void getBindsFromEnvironment() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("logging.structured.ecs.service.name", "spring");
		environment.setProperty("logging.structured.ecs.service.version", "1.2.3");
		environment.setProperty("logging.structured.ecs.service.environment", "prod");
		environment.setProperty("logging.structured.ecs.service.node-name", "boot");
		ElasticCommonSchemaService service = ElasticCommonSchemaService.get(environment);
		assertThat(service).isEqualTo(new ElasticCommonSchemaService("spring", "1.2.3", "prod", "boot"));
	}

	@Test
	void getWhenNoServiceNameUsesApplicationName() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("app.name", "infra");
		ElasticCommonSchemaService service = ElasticCommonSchemaService.get(environment);
		assertThat(service).isEqualTo(new ElasticCommonSchemaService("infra", null, null, null));
	}

	@Test
	void getWhenNoServiceVersionUsesApplicationVersion() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("app.version", "1.2.3");
		ElasticCommonSchemaService service = ElasticCommonSchemaService.get(environment);
		assertThat(service).isEqualTo(new ElasticCommonSchemaService(null, "1.2.3", null, null));
	}

	@Test
	void getWhenNoPropertiesToBind() {
		MockEnvironment environment = new MockEnvironment();
		ElasticCommonSchemaService service = ElasticCommonSchemaService.get(environment);
		assertThat(service).isEqualTo(new ElasticCommonSchemaService(null, null, null, null));
	}

	@Test
	void addToJsonMembersCreatesValidJson() {
		ElasticCommonSchemaService service = new ElasticCommonSchemaService("infra", "1.2.3", "prod", "boot");
		JsonWriter<ElasticCommonSchemaService> writer = JsonWriter.of(service::jsonMembers);
		assertThat(writer.writeToString(service))
			.isEqualTo("{\"service.name\":\"infra\",\"service.version\":\"1.2.3\","
					+ "\"service.environment\":\"prod\",\"service.node.name\":\"boot\"}");
	}

}

/*
 * Copyright 2017 - 2026 the original author or authors.
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

package infra.app.json;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.spy;

/**
 * Tests for {@link JacksonJsonParser}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 */
class JacksonJsonParserTests extends AbstractJsonParserTests {

  @Override
  protected JsonParser getParser() {
    return new JacksonJsonParser();
  }

  @Test
  @SuppressWarnings("unchecked")
  void instanceWithSpecificObjectMapper() {
    JsonMapper jsonMapper = spy(new JsonMapper());
    new JacksonJsonParser(jsonMapper).parseMap("{}");
    then(jsonMapper).should().readValue(eq("{}"), any(TypeReference.class));
  }

  @Override
  @Disabled("Jackson's array handling is no longer stack bound so protection has been removed.")
    // https://github.com/FasterXML/jackson-databind/commit/8238ab41d0350fb915797c89d46777b4496b74fd
  void listWithRepeatedOpenArray(String input) {

  }

}

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

package cn.taketoday.framework.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.io.IOException;

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
	void instanceWithSpecificObjectMapper() throws IOException {
		ObjectMapper objectMapper = spy(new ObjectMapper());
		new JacksonJsonParser(objectMapper).parseMap("{}");
		then(objectMapper).should().readValue(eq("{}"), any(TypeReference.class));
	}

}

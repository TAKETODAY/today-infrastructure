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

package cn.taketoday.buildpack.platform.json;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for JSON based tests.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
public abstract class AbstractJsonTests {

  protected final ObjectMapper getObjectMapper() {
    return SharedObjectMapper.get();
  }

  protected final InputStream getContent(String name) {
    InputStream result = getClass().getResourceAsStream(name);
    assertThat(result).as("JSON source " + name).isNotNull();
    return result;
  }

  protected final String getContentAsString(String name) {
    return new BufferedReader(new InputStreamReader(getContent(name), StandardCharsets.UTF_8)).lines()
            .collect(Collectors.joining("\n"));
  }

}

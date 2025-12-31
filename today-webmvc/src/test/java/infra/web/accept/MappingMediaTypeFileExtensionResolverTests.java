/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web.accept;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import infra.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test fixture for {@link MappingMediaTypeFileExtensionResolver}.
 *
 * @author Rossen Stoyanchev
 * @author Melissa Hartsock
 */
class MappingMediaTypeFileExtensionResolverTests {

  private static final Map<String, MediaType> DEFAULT_MAPPINGS =
          Collections.singletonMap("json", MediaType.APPLICATION_JSON);

  @Test
  void resolveExtensions() {
    List<String> extensions = new MappingMediaTypeFileExtensionResolver(DEFAULT_MAPPINGS)
            .resolveFileExtensions(MediaType.APPLICATION_JSON);

    assertThat(extensions).containsExactly("json");
  }

  @Test
  void resolveExtensionsNoMatch() {
    assertThat(new MappingMediaTypeFileExtensionResolver(DEFAULT_MAPPINGS)
            .resolveFileExtensions(MediaType.TEXT_HTML)).isEmpty();
  }

  @Test
  void resolveExtensionsWithQualityParameter() {
    List<String> extensions = new MappingMediaTypeFileExtensionResolver(DEFAULT_MAPPINGS)
            .resolveFileExtensions(MediaType.parseMediaType("application/json;q=0.9"));

    assertThat(extensions).containsExactly("json");
  }

  @Test
  public void lookupMediaTypeCaseInsensitive() {
    assertThat(new MappingMediaTypeFileExtensionResolver(DEFAULT_MAPPINGS).lookupMediaType("JSON"))
            .isEqualTo(MediaType.APPLICATION_JSON);
  }

  @Test
  void allFileExtensions() {
    Map<String, MediaType> mappings = new HashMap<>();
    mappings.put("json", MediaType.APPLICATION_JSON);
    mappings.put("JsOn", MediaType.APPLICATION_JSON);
    mappings.put("jSoN", MediaType.APPLICATION_JSON);

    MappingMediaTypeFileExtensionResolver resolver = new MappingMediaTypeFileExtensionResolver(mappings);
    assertThat(resolver.getAllFileExtensions()).containsExactly("json");
  }
}

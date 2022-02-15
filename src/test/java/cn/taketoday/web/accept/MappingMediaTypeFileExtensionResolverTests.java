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

package cn.taketoday.web.accept;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test fixture for {@link MappingMediaTypeFileExtensionResolver}.
 *
 * @author Rossen Stoyanchev
 * @author Melissa Hartsock
 */
public class MappingMediaTypeFileExtensionResolverTests {

  private static final Map<String, MediaType> DEFAULT_MAPPINGS =
          Collections.singletonMap("json", MediaType.APPLICATION_JSON);

  @Test
  public void resolveExtensions() {
    List<String> extensions = new MappingMediaTypeFileExtensionResolver(DEFAULT_MAPPINGS)
            .resolveFileExtensions(MediaType.APPLICATION_JSON);

    assertThat(extensions).hasSize(1);
    assertThat(extensions.get(0)).isEqualTo("json");
  }

  @Test
  public void resolveExtensionsNoMatch() {
    assertThat(new MappingMediaTypeFileExtensionResolver(DEFAULT_MAPPINGS)
            .resolveFileExtensions(MediaType.TEXT_HTML)).isEmpty();
  }

  @Test // SPR-13747
  public void lookupMediaTypeCaseInsensitive() {
    assertThat(new MappingMediaTypeFileExtensionResolver(DEFAULT_MAPPINGS).lookupMediaType("JSON"))
            .isEqualTo(MediaType.APPLICATION_JSON);
  }

  @Test
  public void allFileExtensions() {
    Map<String, MediaType> mappings = new HashMap<>();
    mappings.put("json", MediaType.APPLICATION_JSON);
    mappings.put("JsOn", MediaType.APPLICATION_JSON);
    mappings.put("jSoN", MediaType.APPLICATION_JSON);

    MappingMediaTypeFileExtensionResolver resolver = new MappingMediaTypeFileExtensionResolver(mappings);
    assertThat(resolver.getAllFileExtensions()).containsExactly("json");
  }
}

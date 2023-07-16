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

package cn.taketoday.buildpack.platform.docker.configuration;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import cn.taketoday.buildpack.platform.json.AbstractJsonTests;
import cn.taketoday.util.StreamUtils;

/**
 * Tests for {@link DockerRegistryTokenAuthentication}.
 *
 * @author Scott Frederick
 */
class DockerRegistryTokenAuthenticationTests extends AbstractJsonTests {

  @Test
  void createAuthHeaderReturnsEncodedHeader() throws IOException, JSONException {
    DockerRegistryTokenAuthentication auth = new DockerRegistryTokenAuthentication("tokenvalue");
    String header = auth.getAuthHeader();
    String expectedJson = StreamUtils.copyToString(getContent("auth-token.json"), StandardCharsets.UTF_8);
    JSONAssert.assertEquals(expectedJson, new String(Base64.getUrlDecoder().decode(header)), false);
  }

}

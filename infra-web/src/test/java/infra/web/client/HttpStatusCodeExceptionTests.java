/*
 * Copyright 2002-present the original author or authors.
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

package infra.web.client;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;

import infra.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link HttpStatusCodeException} and subclasses.
 *
 * @author Chris Beams
 */
public class HttpStatusCodeExceptionTests {

  @Test
  public void testSerializability() throws IOException, ClassNotFoundException {
    HttpStatusCodeException ex1 = new HttpClientErrorException(
            HttpStatus.BAD_REQUEST, null, null, StandardCharsets.US_ASCII);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    new ObjectOutputStream(out).writeObject(ex1);
    ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
    HttpStatusCodeException ex2 =
            (HttpStatusCodeException) new ObjectInputStream(in).readObject();
    assertThat(ex2.getResponseBodyAsString()).isEqualTo(ex1.getResponseBodyAsString());
  }

  @Test
  public void emptyStatusText() {
    HttpStatusCodeException ex = new HttpClientErrorException(HttpStatus.NOT_FOUND, "");

    assertThat(ex.getMessage()).isEqualTo("404 Not Found");
  }

}

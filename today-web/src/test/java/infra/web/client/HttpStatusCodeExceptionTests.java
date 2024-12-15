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

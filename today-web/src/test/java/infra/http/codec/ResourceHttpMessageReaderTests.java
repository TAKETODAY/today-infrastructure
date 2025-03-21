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

package infra.http.codec;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import infra.core.ResolvableType;
import infra.core.io.ByteArrayResource;
import infra.core.io.Resource;
import infra.core.io.buffer.DataBuffer;
import infra.core.testfixture.io.buffer.AbstractLeakCheckingTests;
import infra.http.ContentDisposition;
import infra.http.HttpStatus;
import infra.http.MediaType;
import infra.web.testfixture.http.client.reactive.MockClientHttpResponse;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ResourceHttpMessageReader}.
 *
 * @author Rossen Stoyanchev
 */
public class ResourceHttpMessageReaderTests extends AbstractLeakCheckingTests {

  private final ResourceHttpMessageReader reader = new ResourceHttpMessageReader();

  @Test
  void readResourceAsMono() throws IOException {
    String filename = "test.txt";
    String body = "Test resource content";

    ContentDisposition contentDisposition =
            ContentDisposition.attachment().name("file").filename(filename).build();

    MockClientHttpResponse response = new MockClientHttpResponse(HttpStatus.OK);
    response.getHeaders().setContentType(MediaType.TEXT_PLAIN);
    response.getHeaders().setContentDisposition(contentDisposition);
    response.setBody(Mono.just(stringBuffer(body)));

    Resource resource = reader.readMono(
            ResolvableType.forClass(ByteArrayResource.class), response, Collections.emptyMap()).block();

    assertThat(resource).isNotNull();
    assertThat(resource.getName()).isEqualTo(filename);
    assertThat(resource.getInputStream()).hasContent(body);
  }

  private DataBuffer stringBuffer(String value) {
    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    DataBuffer buffer = this.bufferFactory.allocateBuffer(bytes.length);
    buffer.write(bytes);
    return buffer;
  }

}

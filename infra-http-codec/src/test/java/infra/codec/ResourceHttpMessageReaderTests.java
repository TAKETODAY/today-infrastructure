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

package infra.codec;

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

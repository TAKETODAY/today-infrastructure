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

package cn.taketoday.http.codec.multipart;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import cn.taketoday.core.MultiValueMap;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.codec.StringDecoder;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.codec.json.AbstractLeakCheckingTests;
import cn.taketoday.http.server.reactive.MockServerHttpResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/11/3 14:11
 */
public class PartEventHttpMessageWriterTests extends AbstractLeakCheckingTests {

  private final PartEventHttpMessageWriter writer = new PartEventHttpMessageWriter();

  private final MockServerHttpResponse response = new MockServerHttpResponse(this.bufferFactory);

  @Test
  public void canWrite() {
    assertThat(this.writer.canWrite(ResolvableType.fromClass(PartEvent.class), MediaType.MULTIPART_FORM_DATA)).isTrue();
    assertThat(this.writer.canWrite(ResolvableType.fromClass(FilePartEvent.class), MediaType.MULTIPART_FORM_DATA)).isTrue();
    assertThat(this.writer.canWrite(ResolvableType.fromClass(FormPartEvent.class), MediaType.MULTIPART_FORM_DATA)).isTrue();
  }

  @Test
  void write() {
    HttpHeaders headers = HttpHeaders.create();
    headers.setContentType(MediaType.TEXT_PLAIN);
    Mono<FormPartEvent> formPartEvent = FormPartEvent.create("text part", "text");

    Flux<FilePartEvent> filePartEvents =
            FilePartEvent.create("file part", "file.txt", MediaType.APPLICATION_OCTET_STREAM,
                    Flux.just(
                            this.bufferFactory.wrap("Aa".getBytes(StandardCharsets.UTF_8)),
                            this.bufferFactory.wrap("Bb".getBytes(StandardCharsets.UTF_8)),
                            this.bufferFactory.wrap("Cc".getBytes(StandardCharsets.UTF_8))
                    ));

    Flux<PartEvent> partEvents = Flux.concat(
            formPartEvent,
            filePartEvents
    );

    Map<String, Object> hints = Collections.emptyMap();
    this.writer.write(partEvents, null, MediaType.MULTIPART_FORM_DATA, this.response, hints)
            .block(Duration.ofSeconds(5));

    MultiValueMap<String, Part> requestParts = MultipartHttpMessageWriterTests.parse(this.response, hints);
    assertThat(requestParts.size()).isEqualTo(2);

    Part part = requestParts.getFirst("text part");
    assertThat(part.name()).isEqualTo("text part");
    assertThat(part.headers().getContentType().isCompatibleWith(MediaType.TEXT_PLAIN)).isTrue();
    String value = decodeToString(part);
    assertThat(value).isEqualTo("text");

    part = requestParts.getFirst("file part");
    assertThat(part.name()).isEqualTo("file part");
    assertThat(((FilePart) part).filename()).isEqualTo("file.txt");
    assertThat(decodeToString(part)).isEqualTo("AaBbCc");
  }

  @SuppressWarnings("ConstantConditions")
  private String decodeToString(Part part) {
    return StringDecoder.textPlainOnly().decodeToMono(part.content(),
            ResolvableType.fromClass(String.class), MediaType.TEXT_PLAIN,
            Collections.emptyMap()).block(Duration.ZERO);
  }

}

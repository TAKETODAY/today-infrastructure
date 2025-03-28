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

package infra.core.codec;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscription;

import java.util.Collections;
import java.util.function.Consumer;

import infra.core.ResolvableType;
import infra.core.io.ClassPathResource;
import infra.core.io.Resource;
import infra.core.io.ResourceRegion;
import infra.core.io.buffer.DataBuffer;
import infra.core.testfixture.io.buffer.AbstractLeakCheckingTests;
import infra.util.MimeType;
import infra.util.MimeTypeUtils;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test cases for {@link ResourceRegionEncoder} class.
 *
 * @author Brian Clozel
 */
class ResourceRegionEncoderTests extends AbstractLeakCheckingTests {

  private ResourceRegionEncoder encoder = new ResourceRegionEncoder();

  @Test
  void canEncode() {
    ResolvableType resourceRegion = ResolvableType.forClass(ResourceRegion.class);
    MimeType allMimeType = MimeType.valueOf("*/*");

    assertThat(this.encoder.canEncode(ResolvableType.forClass(Resource.class), MimeType.APPLICATION_OCTET_STREAM)).isFalse();
    assertThat(this.encoder.canEncode(ResolvableType.forClass(Resource.class), allMimeType)).isFalse();
    assertThat(this.encoder.canEncode(resourceRegion, MimeType.APPLICATION_OCTET_STREAM)).isTrue();
    assertThat(this.encoder.canEncode(resourceRegion, allMimeType)).isTrue();
    assertThat(this.encoder.canEncode(ResolvableType.NONE, null)).isFalse();
  }

  @Test
  void shouldEncodeResourceRegionFileResource() throws Exception {
    ResourceRegion region = new ResourceRegion(
            new ClassPathResource("ResourceRegionEncoderTests.txt", getClass()), 0, 6);
    Flux<DataBuffer> result = this.encoder.encode(Mono.just(region), this.bufferFactory,
            ResolvableType.forClass(ResourceRegion.class), MimeType.APPLICATION_OCTET_STREAM, Collections.emptyMap());

    StepVerifier.create(result)
            .consumeNextWith(stringConsumer("Spring"))
            .expectComplete()
            .verify();
  }

  @Test
  void shouldEncodeMultipleResourceRegionsFileResource() {
    Resource resource = new ClassPathResource("ResourceRegionEncoderTests.txt", getClass());
    Flux<ResourceRegion> regions = Flux.just(
            new ResourceRegion(resource, 0, 6),
            new ResourceRegion(resource, 7, 9),
            new ResourceRegion(resource, 17, 4),
            new ResourceRegion(resource, 22, 17)
    );
    String boundary = MimeTypeUtils.generateMultipartBoundaryString();

    Flux<DataBuffer> result = this.encoder.encode(
            regions, this.bufferFactory,
            ResolvableType.forClass(ResourceRegion.class),
            MimeType.valueOf("text/plain"),
            Collections.singletonMap(ResourceRegionEncoder.BOUNDARY_STRING_HINT, boundary)
    );

    StepVerifier.create(result)
            .consumeNextWith(stringConsumer("\r\n--" + boundary + "\r\n"))
            .consumeNextWith(stringConsumer("Content-Type: text/plain\r\n"))
            .consumeNextWith(stringConsumer("Content-Range: bytes 0-5/39\r\n\r\n"))
            .consumeNextWith(stringConsumer("Spring"))
            .consumeNextWith(stringConsumer("\r\n--" + boundary + "\r\n"))
            .consumeNextWith(stringConsumer("Content-Type: text/plain\r\n"))
            .consumeNextWith(stringConsumer("Content-Range: bytes 7-15/39\r\n\r\n"))
            .consumeNextWith(stringConsumer("Framework"))
            .consumeNextWith(stringConsumer("\r\n--" + boundary + "\r\n"))
            .consumeNextWith(stringConsumer("Content-Type: text/plain\r\n"))
            .consumeNextWith(stringConsumer("Content-Range: bytes 17-20/39\r\n\r\n"))
            .consumeNextWith(stringConsumer("test"))
            .consumeNextWith(stringConsumer("\r\n--" + boundary + "\r\n"))
            .consumeNextWith(stringConsumer("Content-Type: text/plain\r\n"))
            .consumeNextWith(stringConsumer("Content-Range: bytes 22-38/39\r\n\r\n"))
            .consumeNextWith(stringConsumer("resource content."))
            .consumeNextWith(stringConsumer("\r\n--" + boundary + "--"))
            .expectComplete()
            .verify();
  }

  @Test
    // gh-22107
  void cancelWithoutDemandForMultipleResourceRegions() {
    Resource resource = new ClassPathResource("ResourceRegionEncoderTests.txt", getClass());
    Flux<ResourceRegion> regions = Flux.just(
            new ResourceRegion(resource, 0, 6),
            new ResourceRegion(resource, 7, 9),
            new ResourceRegion(resource, 17, 4),
            new ResourceRegion(resource, 22, 17)
    );
    String boundary = MimeTypeUtils.generateMultipartBoundaryString();

    Flux<DataBuffer> flux = this.encoder.encode(
            regions, this.bufferFactory,
            ResolvableType.forClass(ResourceRegion.class),
            MimeType.valueOf("text/plain"),
            Collections.singletonMap(ResourceRegionEncoder.BOUNDARY_STRING_HINT, boundary)
    );

    ZeroDemandSubscriber subscriber = new ZeroDemandSubscriber();
    flux.subscribe(subscriber);
    subscriber.cancel();
  }

  @Test
    // gh-22107
  void cancelWithoutDemandForSingleResourceRegion() {
    Resource resource = new ClassPathResource("ResourceRegionEncoderTests.txt", getClass());
    Mono<ResourceRegion> regions = Mono.just(new ResourceRegion(resource, 0, 6));
    String boundary = MimeTypeUtils.generateMultipartBoundaryString();

    Flux<DataBuffer> flux = this.encoder.encode(
            regions, this.bufferFactory,
            ResolvableType.forClass(ResourceRegion.class),
            MimeType.valueOf("text/plain"),
            Collections.singletonMap(ResourceRegionEncoder.BOUNDARY_STRING_HINT, boundary)
    );

    ZeroDemandSubscriber subscriber = new ZeroDemandSubscriber();
    flux.subscribe(subscriber);
    subscriber.cancel();
  }

  @Test
  void nonExisting() {
    Resource resource = new ClassPathResource("ResourceRegionEncoderTests.txt", getClass());
    Resource nonExisting = new ClassPathResource("does not exist", getClass());
    Flux<ResourceRegion> regions = Flux.just(
            new ResourceRegion(resource, 0, 6),
            new ResourceRegion(nonExisting, 0, 6));

    String boundary = MimeTypeUtils.generateMultipartBoundaryString();

    Flux<DataBuffer> result = this.encoder.encode(
            regions, this.bufferFactory,
            ResolvableType.forClass(ResourceRegion.class),
            MimeType.valueOf("text/plain"),
            Collections.singletonMap(ResourceRegionEncoder.BOUNDARY_STRING_HINT, boundary));

    StepVerifier.create(result)
            .consumeNextWith(stringConsumer("\r\n--" + boundary + "\r\n"))
            .consumeNextWith(stringConsumer("Content-Type: text/plain\r\n"))
            .consumeNextWith(stringConsumer("Content-Range: bytes 0-5/39\r\n\r\n"))
            .consumeNextWith(stringConsumer("Spring"))
            .expectError(EncodingException.class)
            .verify();
  }

  protected Consumer<DataBuffer> stringConsumer(String expected) {
    return dataBuffer -> {
      String value = dataBuffer.toString(UTF_8);
      dataBuffer.release();
      assertThat(value).isEqualTo(expected);
    };
  }

  private static class ZeroDemandSubscriber extends BaseSubscriber<DataBuffer> {

    @Override
    protected void hookOnSubscribe(Subscription subscription) {
      // Just subscribe without requesting
    }
  }

}

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

package cn.taketoday.http.codec.protobuf;

import org.junit.jupiter.api.Test;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.codec.DecodingException;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.testfixture.codec.AbstractDecoderTests;
import cn.taketoday.http.MediaType;
import cn.taketoday.protobuf.Msg;
import cn.taketoday.protobuf.SecondMsg;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ProtobufJsonDecoder}.
 * @author Brian Clozel
 */
public class ProtobufJsonDecoderTests extends AbstractDecoderTests<ProtobufJsonDecoder> {

	private Msg msg1 = Msg.newBuilder().setFoo("Foo").setBlah(SecondMsg.newBuilder().setBlah(123).build()).build();

	public ProtobufJsonDecoderTests() {
		super(new ProtobufJsonDecoder());
	}

	@Test
	@Override
	protected void canDecode() throws Exception {
		ResolvableType msgType = ResolvableType.forClass(Msg.class);
		assertThat(this.decoder.canDecode(msgType, null)).isFalse();
		assertThat(this.decoder.canDecode(msgType, MediaType.APPLICATION_JSON)).isTrue();
		assertThat(this.decoder.canDecode(msgType, MediaType.APPLICATION_PROTOBUF)).isFalse();
		assertThat(this.decoder.canDecode(ResolvableType.forClass(Object.class), MediaType.APPLICATION_JSON)).isFalse();
	}

	@Test
	@Override
	protected void decode() throws Exception {
		ResolvableType msgType = ResolvableType.forClass(Msg.class);
		Flux<DataBuffer> input = Flux.just(dataBuffer("[{\"foo\":\"Foo\",\"blah\":{\"blah\":123}}"),
				dataBuffer(",{\"foo\":\"Bar\",\"blah\":{\"blah\":456}}"),
				dataBuffer("]"));

		testDecode(input, msgType, step -> step.consumeErrorWith(error -> assertThat(error).isInstanceOf(UnsupportedOperationException.class)),
				MediaType.APPLICATION_JSON, null);
	}

	@Test
	@Override
	protected void decodeToMono() throws Exception {
		DataBuffer dataBuffer = dataBuffer("{\"foo\":\"Foo\",\"blah\":{\"blah\":123}}");
		testDecodeToMonoAll(Mono.just(dataBuffer), Msg.class, step -> step
				.expectNext(this.msg1)
				.verifyComplete());
	}

	@Test
	void exceedMaxSize() {
		this.decoder.setMaxMessageSize(1);
		DataBuffer first = dataBuffer("{\"foo\":\"Foo\",");
		DataBuffer second = dataBuffer("\"blah\":{\"blah\":123}}");

		testDecodeToMono(Flux.just(first, second), Msg.class, step -> step.verifyError(DecodingException.class));
	}

	private DataBuffer dataBuffer(String json) {
		return this.bufferFactory.wrap(json.getBytes());
	}

}

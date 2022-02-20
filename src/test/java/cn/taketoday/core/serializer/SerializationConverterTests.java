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

package cn.taketoday.core.serializer;

import org.junit.jupiter.api.Test;

import java.io.NotSerializableException;
import java.io.Serializable;

import cn.taketoday.core.serializer.support.DeserializingConverter;
import cn.taketoday.core.serializer.support.SerializationFailedException;
import cn.taketoday.core.serializer.support.SerializingConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Gary Russell
 * @author Mark Fisher
 * @since 4.0
 */
class SerializationConverterTests {

	@Test
	void serializeAndDeserializeString() {
		SerializingConverter toBytes = new SerializingConverter();
		byte[] bytes = toBytes.convert("Testing");
		DeserializingConverter fromBytes = new DeserializingConverter();
		assertThat(fromBytes.convert(bytes)).isEqualTo("Testing");
	}

	@Test
	void nonSerializableObject() {
		SerializingConverter toBytes = new SerializingConverter();
		assertThatExceptionOfType(SerializationFailedException.class).isThrownBy(() ->
				toBytes.convert(new Object()))
			.withCauseInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void nonSerializableField() {
		SerializingConverter toBytes = new SerializingConverter();
		assertThatExceptionOfType(SerializationFailedException.class).isThrownBy(() ->
				toBytes.convert(new UnSerializable()))
			.withCauseInstanceOf(NotSerializableException.class);
	}

	@Test
	void deserializationFailure() {
		DeserializingConverter fromBytes = new DeserializingConverter();
		assertThatExceptionOfType(SerializationFailedException.class).isThrownBy(() ->
				fromBytes.convert("Junk".getBytes()));
	}


	class UnSerializable implements Serializable {

		private static final long serialVersionUID = 1L;

		@SuppressWarnings("unused")
		private Object object;
	}

}

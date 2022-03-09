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

package cn.taketoday.cache.interceptor;

import org.junit.jupiter.api.Test;

import cn.taketoday.core.testfixture.io.SerializationTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SimpleKeyGenerator} and {@link SimpleKey}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 */
public class SimpleKeyGeneratorTests {

	private final SimpleKeyGenerator generator = new SimpleKeyGenerator();


	@Test
	public void noValues() {
		Object k1 = generateKey(new Object[] {});
		Object k2 = generateKey(new Object[] {});
		Object k3 = generateKey(new Object[] { "different" });
		assertThat(k1.hashCode()).isEqualTo(k2.hashCode());
		assertThat(k1.hashCode()).isNotEqualTo(k3.hashCode());
		assertThat(k1).isEqualTo(k2);
		assertThat(k1).isNotEqualTo(k3);
	}

	@Test
	public void singleValue() {
		Object k1 = generateKey(new Object[] { "a" });
		Object k2 = generateKey(new Object[] { "a" });
		Object k3 = generateKey(new Object[] { "different" });
		assertThat(k1.hashCode()).isEqualTo(k2.hashCode());
		assertThat(k1.hashCode()).isNotEqualTo(k3.hashCode());
		assertThat(k1).isEqualTo(k2);
		assertThat(k1).isNotEqualTo(k3);
		assertThat(k1).isEqualTo("a");
	}

	@Test
	public void multipleValues() {
		Object k1 = generateKey(new Object[] { "a", 1, "b" });
		Object k2 = generateKey(new Object[] { "a", 1, "b" });
		Object k3 = generateKey(new Object[] { "b", 1, "a" });
		assertThat(k1.hashCode()).isEqualTo(k2.hashCode());
		assertThat(k1.hashCode()).isNotEqualTo(k3.hashCode());
		assertThat(k1).isEqualTo(k2);
		assertThat(k1).isNotEqualTo(k3);
	}

	@Test
	public void singleNullValue() {
		Object k1 = generateKey(new Object[] { null });
		Object k2 = generateKey(new Object[] { null });
		Object k3 = generateKey(new Object[] { "different" });
		assertThat(k1.hashCode()).isEqualTo(k2.hashCode());
		assertThat(k1.hashCode()).isNotEqualTo(k3.hashCode());
		assertThat(k1).isEqualTo(k2);
		assertThat(k1).isNotEqualTo(k3);
		assertThat(k1).isInstanceOf(SimpleKey.class);
	}

	@Test
	public void multipleNullValues() {
		Object k1 = generateKey(new Object[] { "a", null, "b", null });
		Object k2 = generateKey(new Object[] { "a", null, "b", null });
		Object k3 = generateKey(new Object[] { "a", null, "b" });
		assertThat(k1.hashCode()).isEqualTo(k2.hashCode());
		assertThat(k1.hashCode()).isNotEqualTo(k3.hashCode());
		assertThat(k1).isEqualTo(k2);
		assertThat(k1).isNotEqualTo(k3);
	}

	@Test
	public void plainArray() {
		Object k1 = generateKey(new Object[] { new String[]{"a", "b"} });
		Object k2 = generateKey(new Object[] { new String[]{"a", "b"} });
		Object k3 = generateKey(new Object[] { new String[]{"b", "a"} });
		assertThat(k1.hashCode()).isEqualTo(k2.hashCode());
		assertThat(k1.hashCode()).isNotEqualTo(k3.hashCode());
		assertThat(k1).isEqualTo(k2);
		assertThat(k1).isNotEqualTo(k3);
	}

	@Test
	public void arrayWithExtraParameter() {
		Object k1 = generateKey(new Object[] { new String[]{"a", "b"}, "c" });
		Object k2 = generateKey(new Object[] { new String[]{"a", "b"}, "c" });
		Object k3 = generateKey(new Object[] { new String[]{"b", "a"}, "c" });
		assertThat(k1.hashCode()).isEqualTo(k2.hashCode());
		assertThat(k1.hashCode()).isNotEqualTo(k3.hashCode());
		assertThat(k1).isEqualTo(k2);
		assertThat(k1).isNotEqualTo(k3);
	}

	@Test
	public void serializedKeys() throws Exception {
		Object k1 = SerializationTestUtils.serializeAndDeserialize(generateKey(new Object[] { "a", 1, "b" }));
		Object k2 = SerializationTestUtils.serializeAndDeserialize(generateKey(new Object[] { "a", 1, "b" }));
		Object k3 = SerializationTestUtils.serializeAndDeserialize(generateKey(new Object[] { "b", 1, "a" }));
		assertThat(k1.hashCode()).isEqualTo(k2.hashCode());
		assertThat(k1.hashCode()).isNotEqualTo(k3.hashCode());
		assertThat(k1).isEqualTo(k2);
		assertThat(k1).isNotEqualTo(k3);
	}


	private Object generateKey(Object[] arguments) {
		return this.generator.generate(null, null, arguments);
	}

}

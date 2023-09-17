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

package cn.taketoday.app.loader.tools;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link Layer}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
class LayerTests {

	@Test
	void createWhenNameIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new Layer(null)).withMessage("Name must not be empty");
	}

	@Test
	void createWhenNameIsEmptyThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new Layer("")).withMessage("Name must not be empty");
	}

	@Test
	void createWhenNameContainsBadCharsThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new Layer("bad!name"))
			.withMessage("Malformed layer name 'bad!name'");
	}

	@Test
	void equalsAndHashCode() {
		Layer layer1 = new Layer("testa");
		Layer layer2 = new Layer("testa");
		Layer layer3 = new Layer("testb");
		assertThat(layer1).hasSameHashCodeAs(layer2);
		assertThat(layer1).isEqualTo(layer1).isEqualTo(layer2).isNotEqualTo(layer3);
	}

	@Test
	void toStringReturnsName() {
		assertThat(new Layer("test")).hasToString("test");
	}

	@Test
	void createWhenUsingReservedNameThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new Layer("ext"))
			.withMessage("Layer name 'ext' is reserved");
		assertThatIllegalArgumentException().isThrownBy(() -> new Layer("ExT"))
			.withMessage("Layer name 'ExT' is reserved");
		assertThatIllegalArgumentException().isThrownBy(() -> new Layer("springbootloader"))
			.withMessage("Layer name 'springbootloader' is reserved");
	}

}

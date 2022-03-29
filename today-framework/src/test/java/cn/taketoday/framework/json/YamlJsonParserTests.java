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

package cn.taketoday.framework.json;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.constructor.ConstructorException;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link YamlJsonParser}.
 *
 * @author Dave Syer
 */
class YamlJsonParserTests extends AbstractJsonParserTests {

	@Override
	protected JsonParser getParser() {
		return new YamlJsonParser();
	}

	@Test
	void customTypesAreNotLoaded() {
		assertThatExceptionOfType(ConstructorException.class)
				.isThrownBy(() -> getParser().parseMap("{value: !!java.net.URL [\"http://localhost:9000/\"]}"))
				.withCauseInstanceOf(IllegalStateException.class);
	}

}

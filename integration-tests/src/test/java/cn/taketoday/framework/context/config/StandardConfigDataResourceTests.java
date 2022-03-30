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

package cn.taketoday.framework.context.config;

import org.junit.jupiter.api.Test;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link StandardConfigDataResource}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
class StandardConfigDataResourceTests {

	StandardConfigDataReference reference = mock(StandardConfigDataReference.class);

	private final Resource resource = mock(Resource.class);

	@Test
	void createWhenReferenceIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new StandardConfigDataResource(null, this.resource))
				.withMessage("Reference must not be null");
	}

	@Test
	void createWhenResourceIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new StandardConfigDataResource(this.reference, null))
				.withMessage("Resource must not be null");
	}

	@Test
	void equalsWhenResourceIsTheSameReturnsTrue() {
		Resource resource = new ClassPathResource("config/");
		StandardConfigDataResource location = new StandardConfigDataResource(this.reference, resource);
		StandardConfigDataResource other = new StandardConfigDataResource(this.reference, resource);
		assertThat(location).isEqualTo(other);
	}

	@Test
	void equalsWhenResourceIsDifferentReturnsFalse() {
		Resource resource1 = new ClassPathResource("config/");
		Resource resource2 = new ClassPathResource("configdata/");
		StandardConfigDataResource location = new StandardConfigDataResource(this.reference, resource1);
		StandardConfigDataResource other = new StandardConfigDataResource(this.reference, resource2);
		assertThat(location).isNotEqualTo(other);
	}

}

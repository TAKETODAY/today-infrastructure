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

package cn.taketoday.http;

import org.junit.jupiter.api.Test;

import cn.taketoday.core.io.Resource;
import cn.taketoday.util.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/15 14:44
 */
class MediaTypeFactoryTests {

	@Test
	public void getMediaType() {
		assertThat(MediaTypeFactory.getMediaType("file.xml").get()).isEqualTo(MediaType.APPLICATION_XML);
		assertThat(MediaTypeFactory.getMediaType("file.js").get()).isEqualTo(MediaType.parseMediaType("application/javascript"));
		assertThat(MediaTypeFactory.getMediaType("file.css").get()).isEqualTo(MediaType.parseMediaType("text/css"));
		assertThat(MediaTypeFactory.getMediaType("file.foobar").isPresent()).isFalse();
	}

	@Test
	public void nullParameter() {
		assertThat(MediaTypeFactory.getMediaType((String) null).isPresent()).isFalse();
		assertThat(MediaTypeFactory.getMediaType((Resource) null).isPresent()).isFalse();
		assertThat(MediaTypeFactory.getMediaTypes(null).isEmpty()).isTrue();
	}

}
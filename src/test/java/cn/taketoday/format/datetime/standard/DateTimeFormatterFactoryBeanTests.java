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

package cn.taketoday.format.datetime.standard;

import org.junit.jupiter.api.Test;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import static org.assertj.core.api.Assertions.assertThat;





/**
 * @author Phillip Webb
 * @author Sam Brannen
 */
public class DateTimeFormatterFactoryBeanTests {

	private final DateTimeFormatterFactoryBean factory = new DateTimeFormatterFactoryBean();


	@Test
	public void isSingleton() {
		assertThat(factory.isSingleton()).isTrue();
	}

	@Test
	public void getObjectType() {
		assertThat(factory.getObjectType()).isEqualTo(DateTimeFormatter.class);
	}

	@Test
	public void getObject() {
		factory.afterPropertiesSet();
		assertThat(factory.getObject().toString()).isEqualTo(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).toString());
	}

	@Test
	public void getObjectIsAlwaysSingleton() {
		factory.afterPropertiesSet();
		DateTimeFormatter formatter = factory.getObject();
		assertThat(formatter.toString()).isEqualTo(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).toString());
		factory.setStylePattern("LL");
		assertThat(factory.getObject()).isSameAs(formatter);
	}

}

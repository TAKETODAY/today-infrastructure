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

package cn.taketoday.jmx.export;

import org.junit.jupiter.api.Test;
import cn.taketoday.jmx.AbstractJmxTests;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.management.ObjectName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 */
class CustomEditorConfigurerTests extends AbstractJmxTests {

	private final SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd");

	@Override
	protected String getApplicationContextPath() {
		return "cn/taketoday/jmx/export/customConfigurer.xml";
	}

	@Test
	void datesInApplicationContext() throws Exception {
		DateRange dr = getContext().getBean("dateRange", DateRange.class);

		assertThat(dr.getStartDate()).as("startDate").isEqualTo(getStartDate());
		assertThat(dr.getEndDate()).as("endDate").isEqualTo(getEndDate());
	}

	@Test
	void datesInJmx() throws Exception {
		ObjectName oname = new ObjectName("bean:name=dateRange");

		Date startJmx = (Date) getServer().getAttribute(oname, "StartDate");
		Date endJmx = (Date) getServer().getAttribute(oname, "EndDate");

		assertThat(startJmx).as("startDate").isEqualTo(getStartDate());
		assertThat(endJmx).as("endDate").isEqualTo(getEndDate());
	}

	private Date getStartDate() throws ParseException {
		return df.parse("2004/10/12");
	}

	private Date getEndDate() throws ParseException {
		return df.parse("2004/11/13");
	}

}

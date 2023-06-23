/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.beans.testfixture.beans;

import cn.taketoday.lang.Nullable;

/**
 * Simple nested test bean used for testing bean factories, AOP framework etc.
 *
 * @author Trevor D. Cook
 * @since 30.09.2003
 */
public class NestedTestBean implements INestedTestBean {

	private String company = "";

	public NestedTestBean() {
	}

	public NestedTestBean(String company) {
		setCompany(company);
	}

	public void setCompany(String company) {
		this.company = (company != null ? company : "");
	}

	@Override
	public String getCompany() {
		return company;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (!(obj instanceof NestedTestBean ntb)) {
			return false;
		}
		return this.company.equals(ntb.company);
	}

	@Override
	public int hashCode() {
		return this.company.hashCode();
	}

	@Override
	public String toString() {
		return "NestedTestBean: " + this.company;
	}

}

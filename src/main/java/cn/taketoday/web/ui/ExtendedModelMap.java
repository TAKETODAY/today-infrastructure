/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Today & 2017 - 2018 All Rights Reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.ui;

import java.util.Map;

public class ExtendedModelMap extends ModelMap implements Model {

	private static final long serialVersionUID = 347423614962447380L;

	@Override
	public ExtendedModelMap addAttribute(String attributeName, Object attributeValue) {
		super.addAttribute(attributeName, attributeValue);
		return this;
	}

	@Override
	public ExtendedModelMap addAllAttributes(Map<String, ?> attributes) {
		super.addAllAttributes(attributes);
		return this;
	}

	@Override
	public ExtendedModelMap mergeAttributes(Map<String, ?> attributes) {
		super.mergeAttributes(attributes);
		return this;
	}

	@Override
	public Map<String, Object> asMap() {
		return this;
	}

}

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
package test.demo.domain;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author Today
 *
 */
@Setter
@Getter
@AllArgsConstructor
public final class Article implements Serializable {

	private static final long serialVersionUID = 1930544427904752617L;

	private Integer	id		= null;
	private String	title	= null;
	private String	content	= null;
	private User	author	= null;
	
	public Article() {

	}

	@Override
	public String toString() {
		return "{\n\t\"id\":\"" + id + "\",\n\t\"title\":\"" + title + "\",\n\t\"content\":\"" + content
				+ "\",\n\t\"author\":\"" + author + "\"\n}";
	}

}

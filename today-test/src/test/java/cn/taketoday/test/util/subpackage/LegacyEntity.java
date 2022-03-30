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

package cn.taketoday.test.util.subpackage;

import cn.taketoday.core.style.ToStringBuilder;

/**
 * A <em>legacy entity</em> whose {@link #toString()} method has side effects;
 * intended for use in unit tests.
 *
 * @author Sam Brannen
 * @since 4.0
 */
public class LegacyEntity {

	private Object collaborator = new Object() {

		@Override
		public String toString() {
			throw new LegacyEntityException(
				"Invoking toString() on the default collaborator causes an undesirable side effect");
		}
	};

	private Integer number;
	private String text;


	public void configure(Integer number, String text) {
		this.number = number;
		this.text = text;
	}

	public Integer getNumber() {
		return this.number;
	}

	public String getText() {
		return this.text;
	}

	public Object getCollaborator() {
		return this.collaborator;
	}

	public void setCollaborator(Object collaborator) {
		this.collaborator = collaborator;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this)//
				.append("collaborator", this.collaborator)//
				.toString();
	}

}

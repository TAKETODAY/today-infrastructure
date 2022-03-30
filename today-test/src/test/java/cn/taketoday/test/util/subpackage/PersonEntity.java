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
 * Concrete subclass of {@link PersistentEntity} representing a <em>person</em>
 * entity; intended for use in unit tests.
 *
 * @author Sam Brannen
 * @since 2.5
 */
public class PersonEntity extends PersistentEntity implements Person {

	protected String name;

	private int age;

	String eyeColor;

	boolean likesPets = false;

	private Number favoriteNumber;


	@Override
	public String getName() {
		return this.name;
	}

	@SuppressWarnings("unused")
	private void setName(final String name) {
		this.name = name;
	}

	@Override
	public int getAge() {
		return this.age;
	}

	protected void setAge(final int age) {
		this.age = age;
	}

	@Override
	public String getEyeColor() {
		return this.eyeColor;
	}

	void setEyeColor(final String eyeColor) {
		this.eyeColor = eyeColor;
	}

	@Override
	public boolean likesPets() {
		return this.likesPets;
	}

	protected void setLikesPets(final boolean likesPets) {
		this.likesPets = likesPets;
	}

	@Override
	public Number getFavoriteNumber() {
		return this.favoriteNumber;
	}

	protected void setFavoriteNumber(Number favoriteNumber) {
		this.favoriteNumber = favoriteNumber;
	}

	@Override
	public String toString() {
		// @formatter:off
		return new ToStringBuilder(this)
			.append("id", this.getId())
			.append("name", this.name)
			.append("age", this.age)
			.append("eyeColor", this.eyeColor)
			.append("likesPets", this.likesPets)
			.append("favoriteNumber", this.favoriteNumber)
			.toString();
		// @formatter:on
	}

}

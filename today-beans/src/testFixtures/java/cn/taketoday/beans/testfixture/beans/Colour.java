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

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
public class Colour {

	public static final Colour RED = new Colour("RED");
	public static final Colour BLUE = new Colour("BLUE");
	public static final Colour GREEN = new Colour("GREEN");
	public static final Colour PURPLE = new Colour("PURPLE");

	private final String name;

	public Colour(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return this.name;
	}

}

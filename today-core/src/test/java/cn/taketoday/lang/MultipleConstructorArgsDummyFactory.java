/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.lang;

/**
 * @author Madhura Bhave
 */
class MultipleConstructorArgsDummyFactory implements DummyFactory {

	private final String string;

	private final Integer age;

	MultipleConstructorArgsDummyFactory(String string) {
		this(string, null);
	}

	MultipleConstructorArgsDummyFactory(String string, Integer age) {
		this.string = string;
		this.age = age;
	}


	@Override
	public String getString() {
		return this.string + this.age;
	}

}

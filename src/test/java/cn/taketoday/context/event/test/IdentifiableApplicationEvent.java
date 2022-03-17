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

package cn.taketoday.context.event.test;

import cn.taketoday.context.ApplicationEvent;

import java.util.UUID;

/**
 * A basic test event that can be uniquely identified easily.
 *
 * @author Stephane Nicoll
 */
@SuppressWarnings("serial")
public abstract class IdentifiableApplicationEvent extends ApplicationEvent implements Identifiable {

	private final String id;

	protected IdentifiableApplicationEvent(Object source, String id) {
		super(source);
		this.id = id;
	}

	protected IdentifiableApplicationEvent(Object source) {
		this(source, UUID.randomUUID().toString());
	}

	protected IdentifiableApplicationEvent() {
		this(new Object());
	}

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		IdentifiableApplicationEvent that = (IdentifiableApplicationEvent) o;

		return this.id.equals(that.id);

	}

	@Override
	public int hashCode() {
		return this.id.hashCode();
	}

}

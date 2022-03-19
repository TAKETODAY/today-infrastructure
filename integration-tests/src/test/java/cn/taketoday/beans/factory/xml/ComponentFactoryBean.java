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

package cn.taketoday.beans.factory.xml;

import cn.taketoday.beans.factory.FactoryBean;

import java.util.List;

public class ComponentFactoryBean implements FactoryBean<Component> {

	private Component parent;
	private List<Component> children;

	public void setParent(Component parent) {
		this.parent = parent;
	}

	public void setChildren(List<Component> children) {
		this.children = children;
	}

	@Override
	public Component getObject() throws Exception {
		if (this.children != null && this.children.size() > 0) {
			for (Component child : children) {
				this.parent.addComponent(child);
			}
		}
		return this.parent;
	}

	@Override
	public Class<Component> getObjectType() {
		return Component.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}

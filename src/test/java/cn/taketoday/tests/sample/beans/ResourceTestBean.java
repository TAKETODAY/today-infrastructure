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

package cn.taketoday.tests.sample.beans;

import java.io.InputStream;
import java.util.Map;

import cn.taketoday.core.io.ContextResource;
import cn.taketoday.core.io.Resource;

/**
 * @author Juergen Hoeller
 * @since 01.04.2004
 */
public class ResourceTestBean {

	private Resource resource;

	private ContextResource contextResource;

	private InputStream inputStream;

	private Resource[] resourceArray;

	private Map<String, Resource> resourceMap;

	private Map<String, Resource[]> resourceArrayMap;


	public ResourceTestBean() {
	}

	public ResourceTestBean(Resource resource, InputStream inputStream) {
		this.resource = resource;
		this.inputStream = inputStream;
	}


	public Resource getResource() {
		return resource;
	}

	public void setResource(Resource resource) {
		this.resource = resource;
	}

	public ContextResource getContextResource() {
		return contextResource;
	}

	public void setContextResource(ContextResource contextResource) {
		this.contextResource = contextResource;
	}

	public InputStream getInputStream() {
		return inputStream;
	}

	public void setInputStream(InputStream inputStream) {
		this.inputStream = inputStream;
	}

	public Resource[] getResourceArray() {
		return resourceArray;
	}

	public void setResourceArray(Resource[] resourceArray) {
		this.resourceArray = resourceArray;
	}

	public Map<String, Resource> getResourceMap() {
		return resourceMap;
	}

	public void setResourceMap(Map<String, Resource> resourceMap) {
		this.resourceMap = resourceMap;
	}

	public Map<String, Resource[]> getResourceArrayMap() {
		return resourceArrayMap;
	}

	public void setResourceArrayMap(Map<String, Resource[]> resourceArrayMap) {
		this.resourceArrayMap = resourceArrayMap;
	}

}

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

package cn.taketoday.framework.web.reactive.context;

import cn.taketoday.core.io.AbstractResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.util.StringUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Resource implementation that replaces the
 * {@link cn.taketoday.web.context.support.ServletContextResource} in a reactive
 * web application.
 * <p>
 * {@link #exists()} always returns {@code false} in order to avoid exposing the whole
 * classpath in a non-servlet environment.
 *
 * @author Brian Clozel
 */
class FilteredReactiveWebContextResource extends AbstractResource {

	private final String path;

	FilteredReactiveWebContextResource(String path) {
		this.path = path;
	}

	@Override
	public boolean exists() {
		return false;
	}

	@Override
	public Resource createRelative(String relativePath) throws IOException {
		String pathToUse = StringUtils.applyRelativePath(this.path, relativePath);
		return new FilteredReactiveWebContextResource(pathToUse);
	}

	@Override
	public String getDescription() {
		return "ReactiveWebContext resource [" + this.path + "]";
	}

	@Override
	public InputStream getInputStream() throws IOException {
		throw new FileNotFoundException(getDescription() + " cannot be opened because it does not exist");
	}

}

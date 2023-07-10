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

package smoketest.layout;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import cn.taketoday.app.loader.tools.CustomLoaderLayout;
import cn.taketoday.app.loader.tools.Layouts;
import cn.taketoday.app.loader.tools.LoaderClassesWriter;

/**
 * An example layout.
 *
 * @author Phillip Webb
 */
public class SampleLayout extends Layouts.Jar implements CustomLoaderLayout {

	private String name;

	public SampleLayout(String name) {
		this.name = name;
	}

	@Override
	public void writeLoadedClasses(LoaderClassesWriter writer) throws IOException {
		writer.writeEntry(this.name, new ByteArrayInputStream("test".getBytes()));
	}

}

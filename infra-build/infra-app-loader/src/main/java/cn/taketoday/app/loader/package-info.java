/*
 * Copyright 2017 - 2023 the original author or authors.
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

/**
 * System that allows self-contained JAR/WAR archives to be launched using
 * {@code java -jar}. Archives can include nested packaged dependency JARs (there is no
 * need to create shade style jars) and are executed without unpacking. The only
 * constraint is that nested JARs must be stored in the archive uncompressed.
 *
 * @see cn.taketoday.app.loader.JarLauncher
 * @see cn.taketoday.app.loader.WarLauncher
 */
package cn.taketoday.app.loader;

/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.test.classpath.resources;

import java.nio.file.Path;

/**
 * A resource that is to be made available in tests.
 *
 * @param path the path of the resoure
 * @param additional whether the resource should be made available in addition to those
 * that already exist elsewhere
 * @author Andy Wilkinson
 * * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * * @since 5.0 2025/3/9 12:45
 */
record Resource(Path path, boolean additional) {

}

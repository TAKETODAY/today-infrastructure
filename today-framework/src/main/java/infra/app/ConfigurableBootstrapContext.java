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

package infra.app;

/**
 * A {@link BootstrapContext} that also provides configuration methods via the
 * {@link BootstrapRegistry} interface.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see BootstrapRegistry
 * @see BootstrapContext
 * @see DefaultBootstrapContext
 * @since 4.0 2022/3/29 10:50
 */
public interface ConfigurableBootstrapContext extends BootstrapRegistry, BootstrapContext {

}

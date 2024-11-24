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

package infra.beans.factory.support;

/**
 * Internal exception passed through to the initiating {@link DefaultSingletonBeanRegistry}
 * (without wrapping in a {@code BeanCreationException}).
 *
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
class ImplicitlyAppearedSingletonException extends IllegalStateException {

  public ImplicitlyAppearedSingletonException() {
    super("About-to-be-created singleton instance implicitly appeared through the " +
            "creation of the factory bean that its bean definition points to");
  }

}

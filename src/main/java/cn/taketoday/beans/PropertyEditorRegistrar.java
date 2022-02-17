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

package cn.taketoday.beans;

/**
 * Interface for strategies that register custom
 * {@link java.beans.PropertyEditor property editors} with a
 * {@link cn.taketoday.beans.PropertyEditorRegistry property editor registry}.
 *
 * <p>This is particularly useful when you need to use the same set of
 * property editors in several different situations: write a corresponding
 * registrar and reuse that in each case.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see PropertyEditorRegistry
 * @see java.beans.PropertyEditor
 * @since 4.0 2022/2/17 17:41
 */
public interface PropertyEditorRegistrar {

  /**
   * Register custom {@link java.beans.PropertyEditor PropertyEditors} with
   * the given {@code PropertyEditorRegistry}.
   * <p>The passed-in registry will usually be a {@link BeanWrapper} or a
   * {@link cn.taketoday.validation.DataBinder DataBinder}.
   * <p>It is expected that implementations will create brand new
   * {@code PropertyEditors} instances for each invocation of this
   * method (since {@code PropertyEditors} are not threadsafe).
   *
   * @param registry the {@code PropertyEditorRegistry} to register the
   * custom {@code PropertyEditors} with
   */
  void registerCustomEditors(PropertyEditorRegistry registry);

}

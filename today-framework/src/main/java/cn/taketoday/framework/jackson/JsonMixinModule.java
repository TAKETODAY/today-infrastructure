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

package cn.taketoday.framework.jackson;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Spring Bean and Jackson {@link Module} to find and
 * {@link SimpleModule#setMixInAnnotation(Class, Class) register}
 * {@link JsonMixin @JsonMixin}-annotated classes.
 *
 * @author Guirong Hu
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see JsonMixin
 * @since 4.0 2022/10/29 21:29
 */
public class JsonMixinModule extends SimpleModule {

  /**
   * Register the specified {@link JsonMixinModuleEntries entries}.
   *
   * @param entries the entries to register to this instance
   * @param classLoader the classloader to use
   */
  public void registerEntries(JsonMixinModuleEntries entries, ClassLoader classLoader) {
    entries.doWithEntry(classLoader, this::setMixInAnnotation);
  }

}

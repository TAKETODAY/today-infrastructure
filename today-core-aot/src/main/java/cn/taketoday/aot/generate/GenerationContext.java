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

package cn.taketoday.aot.generate;

import cn.taketoday.aot.hint.ProxyHints;
import cn.taketoday.aot.hint.ReflectionHints;
import cn.taketoday.aot.hint.ResourceHints;
import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.hint.SerializationHints;

/**
 * Central interface used for code generation.
 *
 * <p>A generation context provides:
 * <ul>
 * <li>Management of all {@linkplain #getGeneratedClasses() generated classes},
 * including naming convention support.</li>
 * <li>Central management of all {@linkplain #getGeneratedFiles() generated files}.</li>
 * <li>Support for recording {@linkplain #getRuntimeHints() runtime hints}.</li>
 * </ul>
 *
 * <p>If a dedicated round of code generation is required while processing, it
 * is possible to create a specialized context using {@link #withName(String)}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 4.0
 */
public interface GenerationContext {

  /**
   * Get the {@link GeneratedClasses} used by the context.
   * <p>All generated classes are written at the end of AOT processing.
   *
   * @return the generated classes
   */
  GeneratedClasses getGeneratedClasses();

  /**
   * Get the {@link GeneratedFiles} used by the context.
   * <p>Used to write resource, java source, or class bytecode files.
   *
   * @return the generated files
   */
  GeneratedFiles getGeneratedFiles();

  /**
   * Get the {@link RuntimeHints} used by the context.
   * <p>Used to record {@linkplain ReflectionHints reflection},
   * {@linkplain ResourceHints resource}, {@linkplain SerializationHints
   * serialization}, and {@linkplain ProxyHints proxy} hints so that the
   * application can run as a native image.
   *
   * @return the runtime hints
   */
  RuntimeHints getRuntimeHints();

  /**
   * Create a new {@link GenerationContext} instance using the specified
   * name to qualify generated assets for a dedicated round of code
   * generation.
   * <p>If the specified name is already in use, a unique sequence is added
   * to ensure the name is unique.
   *
   * @param name the name to use
   * @return a specialized {@link GenerationContext} for the specified name
   */
  GenerationContext withName(String name);

}

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

package infra.context.annotation.config;

import java.io.Serial;
import java.util.Collections;
import java.util.EventObject;
import java.util.List;
import java.util.Set;

/**
 * Event fired when auto-configuration classes are imported.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/5 23:22
 */
public class AutoConfigurationImportEvent extends EventObject {

  @Serial
  private static final long serialVersionUID = 1L;

  private final List<String> candidateConfigurations;

  private final Set<String> exclusions;

  public AutoConfigurationImportEvent(Object source, List<String> candidateConfigurations, Set<String> exclusions) {
    super(source);
    this.candidateConfigurations = Collections.unmodifiableList(candidateConfigurations);
    this.exclusions = Collections.unmodifiableSet(exclusions);
  }

  /**
   * Return the auto-configuration candidate configurations that are going to be
   * imported.
   *
   * @return the auto-configuration candidates
   */
  public List<String> getCandidateConfigurations() {
    return this.candidateConfigurations;
  }

  /**
   * Return the exclusions that were applied.
   *
   * @return the exclusions applied
   */
  public Set<String> getExclusions() {
    return this.exclusions;
  }

}

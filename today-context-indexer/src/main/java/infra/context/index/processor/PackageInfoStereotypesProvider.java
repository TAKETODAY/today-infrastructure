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

package infra.context.index.processor;

import java.util.HashSet;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;

/**
 * A {@link StereotypesProvider} implementation that provides the
 * {@value #STEREOTYPE} stereotype for each package-info.
 *
 * @author Stephane Nicoll
 * @since 4.0
 */
class PackageInfoStereotypesProvider implements StereotypesProvider {

  public static final String STEREOTYPE = "package-info";

  @Override
  public Set<String> getStereotypes(Element element) {
    Set<String> stereotypes = new HashSet<>();
    if (element.getKind() == ElementKind.PACKAGE) {
      stereotypes.add(STEREOTYPE);
    }
    return stereotypes;
  }

}

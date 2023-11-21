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

package cn.taketoday.beans.factory.parsing;

import cn.taketoday.beans.BeanMetadataElement;
import cn.taketoday.core.io.Resource;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Representation of an import that has been processed during the parsing process.
 *
 * @author Juergen Hoeller
 * @see ReaderEventListener#importProcessed(ImportDefinition)
 * @since 4.0
 */
public class ImportDefinition implements BeanMetadataElement {

  private final String importedResource;

  @Nullable
  private final Resource[] actualResources;

  @Nullable
  private final Object source;

  /**
   * Create a new ImportDefinition.
   *
   * @param importedResource the location of the imported resource
   */
  public ImportDefinition(String importedResource) {
    this(importedResource, null, null);
  }

  /**
   * Create a new ImportDefinition.
   *
   * @param importedResource the location of the imported resource
   * @param source the source object (may be {@code null})
   */
  public ImportDefinition(String importedResource, @Nullable Object source) {
    this(importedResource, null, source);
  }

  /**
   * Create a new ImportDefinition.
   *
   * @param importedResource the location of the imported resource
   * @param source the source object (may be {@code null})
   */
  public ImportDefinition(String importedResource, @Nullable Resource[] actualResources, @Nullable Object source) {
    Assert.notNull(importedResource, "Imported resource is required");
    this.importedResource = importedResource;
    this.actualResources = actualResources;
    this.source = source;
  }

  /**
   * Return the location of the imported resource.
   */
  public final String getImportedResource() {
    return this.importedResource;
  }

  @Nullable
  public final Resource[] getActualResources() {
    return this.actualResources;
  }

  @Override
  @Nullable
  public final Object getSource() {
    return this.source;
  }

}

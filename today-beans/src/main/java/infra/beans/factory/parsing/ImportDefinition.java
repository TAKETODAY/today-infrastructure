/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.beans.factory.parsing;

import org.jspecify.annotations.Nullable;

import infra.beans.BeanMetadataElement;
import infra.core.io.Resource;
import infra.lang.Assert;

/**
 * Representation of an import that has been processed during the parsing process.
 *
 * @author Juergen Hoeller
 * @see ReaderEventListener#importProcessed(ImportDefinition)
 * @since 4.0
 */
public class ImportDefinition implements BeanMetadataElement {

  private final String importedResource;

  private final Resource @Nullable [] actualResources;

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
  public ImportDefinition(String importedResource, Resource @Nullable [] actualResources, @Nullable Object source) {
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

  public final Resource @Nullable [] getActualResources() {
    return this.actualResources;
  }

  @Override
  @Nullable
  public final Object getSource() {
    return this.source;
  }

}

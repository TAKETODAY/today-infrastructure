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

package infra.core.type.filter;

import org.jspecify.annotations.Nullable;

import java.io.IOException;

import infra.core.type.ClassMetadata;
import infra.core.type.classreading.MetadataReader;
import infra.core.type.classreading.MetadataReaderFactory;
import infra.logging.Logger;
import infra.logging.LoggerFactory;

/**
 * Type filter that is aware of traversing over hierarchy.
 *
 * <p>This filter is useful when matching needs to be made based on potentially the
 * whole class/interface hierarchy. The algorithm employed uses a succeed-fast
 * strategy: if at any time a match is declared, no further processing is
 * carried out.
 *
 * @author Ramnivas Laddad
 * @author Mark Fisher
 * @since 4.0
 */
public abstract class AbstractTypeHierarchyTraversingFilter implements TypeFilter {

  private static final Logger log = LoggerFactory.getLogger(AbstractTypeHierarchyTraversingFilter.class);

  private final boolean considerInherited;

  private final boolean considerInterfaces;

  protected AbstractTypeHierarchyTraversingFilter(boolean considerInherited, boolean considerInterfaces) {
    this.considerInherited = considerInherited;
    this.considerInterfaces = considerInterfaces;
  }

  @Override
  public boolean match(MetadataReader metadataReader, MetadataReaderFactory factory) throws IOException {
    // This method optimizes avoiding unnecessary creation of ClassReaders
    // as well as visiting over those readers.
    if (matchSelf(metadataReader)) {
      return true;
    }
    ClassMetadata metadata = metadataReader.getClassMetadata();
    if (matchClassName(metadata.getClassName())) {
      return true;
    }

    if (this.considerInherited) {
      String superClassName = metadata.getSuperClassName();
      if (superClassName != null) {
        // Optimization to avoid creating ClassReader for superclass.
        Boolean superClassMatch = matchSuperClass(superClassName);
        if (superClassMatch != null) {
          if (superClassMatch) {
            return true;
          }
        }
        else {
          // Need to read superclass to determine a match...
          try {
            if (match(superClassName, factory)) {
              return true;
            }
          }
          catch (IOException ex) {
            if (log.isDebugEnabled()) {
              log.debug("Could not read superclass [{}] of type-filtered class [{}]", superClassName, metadata.getClassName());
            }
          }
        }
      }
    }

    if (this.considerInterfaces) {
      for (String ifc : metadata.getInterfaceNames()) {
        // Optimization to avoid creating ClassReader for super class
        Boolean interfaceMatch = matchInterface(ifc);
        if (interfaceMatch != null) {
          if (interfaceMatch) {
            return true;
          }
        }
        else {
          // Need to read interface to determine a match...
          try {
            if (match(ifc, factory)) {
              return true;
            }
          }
          catch (IOException ex) {
            if (log.isDebugEnabled()) {
              log.debug("Could not read interface [{}] for type-filtered class [{]]", ifc, metadata.getClassName());
            }
          }
        }
      }
    }

    return false;
  }

  private boolean match(String className, MetadataReaderFactory metadataReaderFactory) throws IOException {
    return match(metadataReaderFactory.getMetadataReader(className), metadataReaderFactory);
  }

  /**
   * Override this to match self characteristics alone. Typically,
   * the implementation will use a visitor to extract information
   * to perform matching.
   */
  protected boolean matchSelf(MetadataReader metadataReader) {
    return false;
  }

  /**
   * Override this to match on type name.
   */
  protected boolean matchClassName(String className) {
    return false;
  }

  /**
   * Override this to match on super type name.
   */
  @Nullable
  protected Boolean matchSuperClass(String superClassName) {
    return null;
  }

  /**
   * Override this to match on interface type name.
   */
  @Nullable
  protected Boolean matchInterface(String interfaceName) {
    return null;
  }

}

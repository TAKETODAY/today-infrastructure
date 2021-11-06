/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.core.type.filter;

import java.io.IOException;

import cn.taketoday.core.type.ClassMetadata;
import cn.taketoday.core.type.classreading.MetadataReader;
import cn.taketoday.core.type.classreading.MetadataReaderFactory;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

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
        // Optimization to avoid creating ClassReader for super class.
        Boolean superClassMatch = matchSuperClass(superClassName);
        if (superClassMatch != null) {
          if (superClassMatch) {
            return true;
          }
        }
        else {
          // Need to read super class to determine a match...
          try {
            if (match(metadata.getSuperClassName(), factory)) {
              return true;
            }
          }
          catch (IOException ex) {
            if (log.isDebugEnabled()) {
              log.debug("Could not read super class [{}] of type-filtered class [{]]",
                      metadata.getSuperClassName(), metadata.getClassName());
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

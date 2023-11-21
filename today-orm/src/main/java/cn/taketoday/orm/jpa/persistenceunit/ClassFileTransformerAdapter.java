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

package cn.taketoday.orm.jpa.persistenceunit;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import jakarta.persistence.spi.ClassTransformer;

/**
 * Simple adapter that implements the {@code java.lang.instrument.ClassFileTransformer}
 * interface based on a JPA {@code ClassTransformer} which a JPA PersistenceProvider
 * asks the {@code PersistenceUnitInfo} to install in the current runtime.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see jakarta.persistence.spi.PersistenceUnitInfo#addTransformer(ClassTransformer)
 * @since 4.0
 */
class ClassFileTransformerAdapter implements ClassFileTransformer {

  private static final Logger log = LoggerFactory.getLogger(ClassFileTransformerAdapter.class);

  private final ClassTransformer classTransformer;

  private boolean currentlyTransforming = false;

  public ClassFileTransformerAdapter(ClassTransformer classTransformer) {
    Assert.notNull(classTransformer, "ClassTransformer is required");
    this.classTransformer = classTransformer;
  }

  @Override
  @Nullable
  public byte[] transform(ClassLoader loader, String className,
          Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {

    synchronized(this) {
      if (this.currentlyTransforming) {
        // Defensively back out when called from within the transform delegate below:
        // in particular, for the over-eager transformer implementation in Hibernate 5.
        return null;
      }

      this.currentlyTransforming = true;
      try {
        byte[] transformed = classTransformer.transform(
                loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
        if (transformed != null && log.isDebugEnabled()) {
          log.debug("Transformer of class [{}] transformed class [{}]; bytes in={}; bytes out={}",
                  classTransformer.getClass().getName(), className, classfileBuffer.length, transformed.length);
        }
        return transformed;
      }
      catch (ClassCircularityError ex) {
        if (log.isErrorEnabled()) {
          log.error("Circularity error while weaving class [{}] with transformer of class [{}]",
                  className, classTransformer.getClass().getName(), ex);
        }
        throw new IllegalStateException("Failed to weave class [" + className + "]", ex);
      }
      catch (Throwable ex) {
        if (log.isWarnEnabled()) {
          log.warn("Error weaving class [{}] with transformer of class [{}]",
                  className, classTransformer.getClass().getName(), ex);
        }
        // The exception will be ignored by the class loader, anyway...
        throw new IllegalStateException("Could not weave class [" + className + "]", ex);
      }
      finally {
        this.currentlyTransforming = false;
      }
    }
  }

  @Override
  public String toString() {
    return "Standard ClassFileTransformer wrapping JPA transformer: " + classTransformer;
  }

}

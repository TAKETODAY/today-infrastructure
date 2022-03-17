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

package cn.taketoday.context.annotation;

import java.lang.annotation.Annotation;

import cn.taketoday.context.loader.ImportSelector;
import cn.taketoday.core.GenericTypeResolver;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Convenient base class for {@link ImportSelector} implementations that select imports
 * based on an {@link AdviceMode} value from an annotation (such as the {@code @Enable*}
 * annotations).
 *
 * @param <A> annotation containing {@linkplain #getAdviceModeAttributeName() AdviceMode attribute}
 * @author Chris Beams
 * @since 4.0
 */
public abstract class AdviceModeImportSelector<A extends Annotation> implements ImportSelector {

  /**
   * The default advice mode attribute name.
   */
  public static final String DEFAULT_ADVICE_MODE_ATTRIBUTE_NAME = "mode";

  /**
   * The name of the {@link AdviceMode} attribute for the annotation specified by the
   * generic type {@code A}. The default is {@value #DEFAULT_ADVICE_MODE_ATTRIBUTE_NAME},
   * but subclasses may override in order to customize.
   */
  protected String getAdviceModeAttributeName() {
    return DEFAULT_ADVICE_MODE_ATTRIBUTE_NAME;
  }

  /**
   * This implementation resolves the type of annotation from generic metadata and
   * validates that (a) the annotation is in fact present on the importing
   * {@code @Configuration} class and (b) that the given annotation has an
   * {@linkplain #getAdviceModeAttributeName() advice mode attribute} of type
   * {@link AdviceMode}.
   * <p>The {@link #selectImports(AdviceMode)} method is then invoked, allowing the
   * concrete implementation to choose imports in a safe and convenient fashion.
   *
   * @throws IllegalArgumentException if expected annotation {@code A} is not present
   * on the importing {@code @Configuration} class or if {@link #selectImports(AdviceMode)}
   * returns {@code null}
   */
  @Override
  public final String[] selectImports(AnnotationMetadata importingClassMetadata) {
    Class<?> annType = GenericTypeResolver.resolveTypeArgument(getClass(), AdviceModeImportSelector.class);
    Assert.state(annType != null, "Unresolvable type argument for AdviceModeImportSelector");

    MergedAnnotation<Annotation> attributes = importingClassMetadata.getAnnotation(annType.getName());
    if (!attributes.isPresent()) {
      throw new IllegalArgumentException(String.format(
              "@%s is not present on importing class '%s' as expected",
              annType.getSimpleName(), importingClassMetadata.getClassName()));
    }

    AdviceMode adviceMode = attributes.getEnum(getAdviceModeAttributeName(), AdviceMode.class);
    String[] imports = selectImports(adviceMode);
    if (imports == null) {
      throw new IllegalArgumentException("Unknown AdviceMode: " + adviceMode);
    }
    return imports;
  }

  /**
   * Determine which classes should be imported based on the given {@code AdviceMode}.
   * <p>Returning {@code null} from this method indicates that the {@code AdviceMode}
   * could not be handled or was unknown and that an {@code IllegalArgumentException}
   * should be thrown.
   *
   * @param adviceMode the value of the {@linkplain #getAdviceModeAttributeName()
   * advice mode attribute} for the annotation specified via generics.
   * @return array containing classes to import (empty array if none;
   * {@code null} if the given {@code AdviceMode} is unknown)
   */
  @Nullable
  protected abstract String[] selectImports(AdviceMode adviceMode);

}

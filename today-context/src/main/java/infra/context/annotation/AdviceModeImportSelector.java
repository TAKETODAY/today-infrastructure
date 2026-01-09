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

package infra.context.annotation;

import org.jspecify.annotations.Nullable;

import java.lang.annotation.Annotation;

import infra.core.GenericTypeResolver;
import infra.core.annotation.MergedAnnotation;
import infra.core.type.AnnotationMetadata;
import infra.lang.Assert;

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
  public final String[] selectImports(AnnotationMetadata importMetadata) {
    Class<?> annType = GenericTypeResolver.resolveTypeArgument(getClass(), AdviceModeImportSelector.class);
    Assert.state(annType != null, "Unresolvable type argument for AdviceModeImportSelector");

    MergedAnnotation<Annotation> attributes = importMetadata.getAnnotation(annType.getName());
    if (!attributes.isPresent()) {
      throw new IllegalArgumentException(String.format(
              "@%s is not present on importing class '%s' as expected",
              annType.getSimpleName(), importMetadata.getClassName()));
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
  protected abstract String @Nullable [] selectImports(AdviceMode adviceMode);

}

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

package cn.taketoday.core.annotation;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import cn.taketoday.lang.Nullable;

/**
 * An {@link AbstractMergedAnnotation} used as the implementation of
 * {@link MergedAnnotation#missing()}.
 *
 * @param <A> the annotation type
 * @author Phillip Webb
 * @author Juergen Hoeller
 * @since 4.0
 */
final class MissingMergedAnnotation<A extends Annotation> extends AbstractMergedAnnotation<A> {

  private static final MissingMergedAnnotation<?> INSTANCE = new MissingMergedAnnotation<>();

  private MissingMergedAnnotation() { }

  @Override
  public Class<A> getType() {
    throw new NoSuchElementException("Unable to get type for missing annotation");
  }

  @Override
  public boolean isPresent() {
    return false;
  }

  @Override
  @Nullable
  public Object getSource() {
    return null;
  }

  @Override
  @Nullable
  public MergedAnnotation<?> getMetaSource() {
    return null;
  }

  @Override
  public MergedAnnotation<?> getRoot() {
    return this;
  }

  @Override
  public List<Class<? extends Annotation>> getMetaTypes() {
    return Collections.emptyList();
  }

  @Override
  public int getDistance() {
    return -1;
  }

  @Override
  public int getAggregateIndex() {
    return -1;
  }

  @Override
  public boolean hasNonDefaultValue(String attributeName) {
    throw new NoSuchElementException(
            "Unable to check non-default value for missing annotation");
  }

  @Override
  public boolean hasDefaultValue(String attributeName) {
    throw new NoSuchElementException(
            "Unable to check default value for missing annotation");
  }

  @Override
  public <T> Optional<T> getValue(String attributeName, Class<T> type) {
    return Optional.empty();
  }

  @Override
  public <T> Optional<T> getDefaultValue(@Nullable String attributeName, Class<T> type) {
    return Optional.empty();
  }

  @Override
  public MergedAnnotation<A> filterAttributes(Predicate<String> predicate) {
    return this;
  }

  @Override
  public MergedAnnotation<A> withNonMergedAttributes() {
    return this;
  }

  @Override
  public AnnotationAttributes asAnnotationAttributes(MergedAnnotation.Adapt... adaptations) {
    return new AnnotationAttributes();
  }

  @Override
  public Map<String, Object> asMap(MergedAnnotation.Adapt... adaptations) {
    return Collections.emptyMap();
  }

  @Override
  public <T extends Map<String, Object>> T asMap(
          Function<MergedAnnotation<?>, T> factory, MergedAnnotation.Adapt... adaptations) {
    return factory.apply(this);
  }

  @Override
  public boolean isSynthesizable() {
    return false;
  }

  @Override
  public String toString() {
    return "(missing)";
  }

  @Override
  public <T extends Annotation> MergedAnnotation<T> getAnnotation(
          String attributeName, Class<T> type) throws NoSuchElementException {

    throw new NoSuchElementException(
            "Unable to get attribute value for missing annotation");
  }

  @Override
  public <T extends Annotation> MergedAnnotation<T>[] getAnnotationArray(
          String attributeName, Class<T> type) throws NoSuchElementException {
    throw new NoSuchElementException(
            "Unable to get attribute value for missing annotation");
  }

  @Override
  protected <T> T getAttributeValue(String attributeName, Class<T> type) {
    throw new NoSuchElementException(
            "Unable to get attribute '" + attributeName + "'s value "
                    + type.getName() + " for missing annotation from");
  }

  @Override
  protected A createSynthesizedAnnotation() {
    throw new NoSuchElementException("Unable to synthesize missing annotation");
  }

  @SuppressWarnings("unchecked")
  static <A extends Annotation> MergedAnnotation<A> getInstance() {
    return (MergedAnnotation<A>) INSTANCE;
  }

}

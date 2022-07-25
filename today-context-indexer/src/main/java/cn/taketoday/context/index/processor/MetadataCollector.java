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

package cn.taketoday.context.index.processor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

/**
 * Used by {@link CandidateComponentsIndexer} to collect {@link CandidateComponentsMetadata}.
 *
 * @author Stephane Nicoll
 * @since 4.0
 */
class MetadataCollector {

  private final List<ItemMetadata> metadataItems = new ArrayList<>();
  private final ProcessingEnvironment processingEnvironment;
  private final CandidateComponentsMetadata previousMetadata;

  private final TypeHelper typeHelper;
  private final Set<String> processedSourceTypes = new HashSet<>();

  /**
   * Create a new {@code MetadataProcessor} instance.
   *
   * @param processingEnvironment the processing environment of the build
   * @param previousMetadata any previous metadata or {@code null}
   */
  public MetadataCollector(
          ProcessingEnvironment processingEnvironment, CandidateComponentsMetadata previousMetadata) {

    this.processingEnvironment = processingEnvironment;
    this.previousMetadata = previousMetadata;
    this.typeHelper = new TypeHelper(processingEnvironment);
  }

  public void processing(RoundEnvironment roundEnv) {
    for (Element element : roundEnv.getRootElements()) {
      markAsProcessed(element);
    }
  }

  private void markAsProcessed(Element element) {
    if (element instanceof TypeElement) {
      this.processedSourceTypes.add(this.typeHelper.getType(element));
    }
  }

  public void add(ItemMetadata metadata) {
    this.metadataItems.add(metadata);
  }

  public CandidateComponentsMetadata getMetadata() {
    CandidateComponentsMetadata metadata = new CandidateComponentsMetadata();
    for (ItemMetadata item : this.metadataItems) {
      metadata.add(item);
    }
    if (this.previousMetadata != null) {
      List<ItemMetadata> items = this.previousMetadata.getItems();
      for (ItemMetadata item : items) {
        if (shouldBeMerged(item)) {
          metadata.add(item);
        }
      }
    }
    return metadata;
  }

  private boolean shouldBeMerged(ItemMetadata itemMetadata) {
    String sourceType = itemMetadata.getType();
    return (sourceType != null && !deletedInCurrentBuild(sourceType)
            && !processedInCurrentBuild(sourceType));
  }

  private boolean deletedInCurrentBuild(String sourceType) {
    return this.processingEnvironment.getElementUtils()
            .getTypeElement(sourceType) == null;
  }

  private boolean processedInCurrentBuild(String sourceType) {
    return this.processedSourceTypes.contains(sourceType);
  }

}

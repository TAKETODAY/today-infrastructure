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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.Completion;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

/**
 * Annotation {@link Processor} that writes a {@link CandidateComponentsMetadata}
 * file for Framework components.
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 4.0
 */
public class CandidateComponentsIndexer implements Processor {

  private MetadataStore metadataStore;

  private MetadataCollector metadataCollector;

  private TypeHelper typeHelper;

  private List<StereotypesProvider> stereotypesProviders;

  @Override
  public Set<String> getSupportedOptions() {
    return Collections.emptySet();
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return Collections.singleton("*");
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latest();
  }

  @Override
  public synchronized void init(ProcessingEnvironment env) {
    System.out.println(env);
    this.stereotypesProviders = getStereotypesProviders(env);
    this.typeHelper = new TypeHelper(env);
    this.metadataStore = new MetadataStore(env);
    this.metadataCollector = new MetadataCollector(env, this.metadataStore.readMetadata());
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    System.out.println(roundEnv);
    this.metadataCollector.processing(roundEnv);
    roundEnv.getRootElements().forEach(this::processElement);
    if (roundEnv.processingOver()) {
      writeMetaData();
    }
    return false;
  }

  @Override
  public Iterable<? extends Completion> getCompletions(
          Element element, AnnotationMirror annotation, ExecutableElement member, String userText) {

    return Collections.emptyList();
  }

  private List<StereotypesProvider> getStereotypesProviders(ProcessingEnvironment env) {
    ArrayList<StereotypesProvider> result = new ArrayList<>();
    TypeHelper typeHelper = new TypeHelper(env);
    result.add(new IndexedStereotypesProvider(typeHelper));
    result.add(new StandardStereotypesProvider(typeHelper));
    result.add(new PackageInfoStereotypesProvider());
    return result;
  }

  private void processElement(Element element) {
    addMetadataFor(element);
    staticTypesIn(element.getEnclosedElements()).forEach(this::processElement);
  }

  private void addMetadataFor(Element element) {
    LinkedHashSet<String> stereotypes = new LinkedHashSet<>();
    for (StereotypesProvider p : stereotypesProviders) {
      stereotypes.addAll(p.getStereotypes(element));
    }
    if (!stereotypes.isEmpty()) {
      this.metadataCollector.add(new ItemMetadata(this.typeHelper.getType(element), stereotypes));
    }
  }

  private void writeMetaData() {
    CandidateComponentsMetadata metadata = this.metadataCollector.getMetadata();
    if (!metadata.getItems().isEmpty()) {
      try {
        this.metadataStore.writeMetadata(metadata);
      }
      catch (IOException ex) {
        throw new IllegalStateException("Failed to write metadata", ex);
      }
    }
  }

  private static List<TypeElement> staticTypesIn(Iterable<? extends Element> elements) {
    ArrayList<TypeElement> list = new ArrayList<>();
    for (Element element : elements) {
      if ((element.getKind().isClass()
              || element.getKind() == ElementKind.INTERFACE)
              && element.getModifiers().contains(Modifier.STATIC)
              && element instanceof TypeElement) {
        list.add((TypeElement) element);
      }
    }
    return list;
  }

}

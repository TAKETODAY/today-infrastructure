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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;

import cn.taketoday.context.index.sample.AbstractController;
import cn.taketoday.context.index.sample.MetaControllerIndexed;
import cn.taketoday.context.index.sample.SampleComponent;
import cn.taketoday.context.index.sample.SampleController;
import cn.taketoday.context.index.sample.SampleEmbedded;
import cn.taketoday.context.index.sample.SampleMetaController;
import cn.taketoday.context.index.sample.SampleMetaIndexedController;
import cn.taketoday.context.index.sample.SampleNonStaticEmbedded;
import cn.taketoday.context.index.sample.SampleNone;
import cn.taketoday.context.index.sample.SampleRepository;
import cn.taketoday.context.index.sample.SampleService;
import cn.taketoday.context.index.sample.cdi.SampleManagedBean;
import cn.taketoday.context.index.sample.cdi.SampleNamed;
import cn.taketoday.context.index.sample.cdi.SampleTransactional;
import cn.taketoday.context.index.sample.jpa.SampleConverter;
import cn.taketoday.context.index.sample.jpa.SampleEmbeddable;
import cn.taketoday.context.index.sample.jpa.SampleEntity;
import cn.taketoday.context.index.sample.jpa.SampleMappedSuperClass;
import cn.taketoday.context.index.sample.type.Repo;
import cn.taketoday.context.index.sample.type.SampleRepo;
import cn.taketoday.context.index.sample.type.SampleSmartRepo;
import cn.taketoday.context.index.sample.type.SampleSpecializedRepo;
import cn.taketoday.context.index.sample.type.SmartRepo;
import cn.taketoday.context.index.sample.type.SpecializedRepo;
import cn.taketoday.context.index.test.TestCompiler;
import cn.taketoday.stereotype.Component;
import cn.taketoday.util.ClassUtils;
import jakarta.annotation.ManagedBean;
import jakarta.inject.Named;
import jakarta.persistence.Converter;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;
import jakarta.transaction.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CandidateComponentsIndexer}.
 *
 * @author Stephane Nicoll
 * @author Vedran Pavic
 * @author Sam Brannen
 */
class CandidateComponentsIndexerTests {

  private TestCompiler compiler;

  @BeforeEach
  void createCompiler(@TempDir Path tempDir) throws IOException {
    this.compiler = new TestCompiler(tempDir);
  }

  @Test
  void noCandidate() {
    CandidateComponentsMetadata metadata = compile(SampleNone.class);
    assertThat(metadata.getItems()).hasSize(0);
  }

  @Test
  void noAnnotation() {
    CandidateComponentsMetadata metadata = compile(CandidateComponentsIndexerTests.class);
    assertThat(metadata.getItems()).hasSize(0);
  }

  @Test
  void stereotypeComponent() {
    testComponent(SampleComponent.class);
  }

  @Test
  void stereotypeService() {
    testComponent(SampleService.class);
  }

  @Test
  void stereotypeController() {
    testComponent(SampleController.class);
  }

  @Test
  void stereotypeControllerMetaAnnotation() {
    testComponent(SampleMetaController.class);
  }

  @Test
  void stereotypeRepository() {
    testSingleComponent(SampleRepository.class, Component.class);
  }

  @Test
  void stereotypeControllerMetaIndex() {
    testSingleComponent(SampleMetaIndexedController.class, Component.class, MetaControllerIndexed.class);
  }

  @Test
  void stereotypeOnAbstractClass() {
    testComponent(AbstractController.class);
  }

  @Test
  void cdiManagedBean() {
    testSingleComponent(SampleManagedBean.class, ManagedBean.class);
  }

  @Test
  void cdiNamed() {
    testSingleComponent(SampleNamed.class, Named.class);
  }

  @Test
  void cdiTransactional() {
    testSingleComponent(SampleTransactional.class, Transactional.class);
  }

  @Test
  void persistenceEntity() {
    testSingleComponent(SampleEntity.class, Entity.class);
  }

  @Test
  void persistenceMappedSuperClass() {
    testSingleComponent(SampleMappedSuperClass.class, MappedSuperclass.class);
  }

  @Test
  void persistenceEmbeddable() {
    testSingleComponent(SampleEmbeddable.class, Embeddable.class);
  }

  @Test
  void persistenceConverter() {
    testSingleComponent(SampleConverter.class, Converter.class);
  }

  @Test
  void packageInfo() {
    CandidateComponentsMetadata metadata = compile("cn/taketoday/context/index/sample/jpa/package-info");
    assertThat(metadata).has(Metadata.of("cn.taketoday.context.index.sample.jpa", "package-info"));
  }

  @Test
  void typeStereotypeFromMetaInterface() {
    testSingleComponent(SampleSpecializedRepo.class, Repo.class);
  }

  @Test
  void typeStereotypeFromInterfaceFromSuperClass() {
    testSingleComponent(SampleRepo.class, Repo.class);
  }

  @Test
  void typeStereotypeFromSeveralInterfaces() {
    testSingleComponent(SampleSmartRepo.class, Repo.class, SmartRepo.class);
  }

  @Test
  void typeStereotypeOnInterface() {
    testSingleComponent(SpecializedRepo.class, Repo.class);
  }

  @Test
  void typeStereotypeOnInterfaceFromSeveralInterfaces() {
    testSingleComponent(SmartRepo.class, Repo.class, SmartRepo.class);
  }

  @Test
  void typeStereotypeOnIndexedInterface() {
    testSingleComponent(Repo.class, Repo.class);
  }

  @Test
  void embeddedCandidatesAreDetected()
          throws IOException, ClassNotFoundException {
    // Validate nested type structure
    String nestedType = "cn.taketoday.context.index.sample.SampleEmbedded.Another$AnotherPublicCandidate";
    Class<?> type = ClassUtils.forName(nestedType, getClass().getClassLoader());
    assertThat(type).isSameAs(SampleEmbedded.Another.AnotherPublicCandidate.class);

    CandidateComponentsMetadata metadata = compile(SampleEmbedded.class);
    assertThat(metadata).has(Metadata.of(SampleEmbedded.PublicCandidate.class, Component.class));
    assertThat(metadata).has(Metadata.of(nestedType, Component.class.getName()));
    assertThat(metadata.getItems()).hasSize(2);
  }

  @Test
  void embeddedNonStaticCandidateAreIgnored() {
    CandidateComponentsMetadata metadata = compile(SampleNonStaticEmbedded.class);
    assertThat(metadata.getItems()).hasSize(0);
  }

  private void testComponent(Class<?>... classes) {
    CandidateComponentsMetadata metadata = compile(classes);
    for (Class<?> c : classes) {
      assertThat(metadata).has(Metadata.of(c, Component.class));
    }
    assertThat(metadata.getItems()).hasSize(classes.length);
  }

  private void testSingleComponent(Class<?> target, Class<?>... stereotypes) {
    CandidateComponentsMetadata metadata = compile(target);
    assertThat(metadata).has(Metadata.of(target, stereotypes));
    assertThat(metadata.getItems()).hasSize(1);
  }

  private CandidateComponentsMetadata compile(Class<?>... types) {
    CandidateComponentsIndexer processor = new CandidateComponentsIndexer();
    this.compiler.getTask(types).call(processor);
    return readGeneratedMetadata(this.compiler.getOutputLocation());
  }

  private CandidateComponentsMetadata compile(String... types) {
    CandidateComponentsIndexer processor = new CandidateComponentsIndexer();
    this.compiler.getTask(types).call(processor);
    return readGeneratedMetadata(this.compiler.getOutputLocation());
  }

  private CandidateComponentsMetadata readGeneratedMetadata(File outputLocation) {
    File metadataFile = new File(outputLocation, MetadataStore.METADATA_PATH);
    if (metadataFile.isFile()) {
      try (FileInputStream fileInputStream = new FileInputStream(metadataFile)) {
        CandidateComponentsMetadata metadata = PropertiesMarshaller.read(fileInputStream);
        return metadata;
      }
      catch (IOException ex) {
        throw new IllegalStateException("Failed to read metadata from disk", ex);
      }
    }
    else {
      return new CandidateComponentsMetadata();
    }
  }

}

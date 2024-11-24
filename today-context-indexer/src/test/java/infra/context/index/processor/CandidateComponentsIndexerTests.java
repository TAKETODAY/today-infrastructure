/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.context.index.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;

import infra.context.index.sample.AbstractController;
import infra.context.index.sample.MetaControllerIndexed;
import infra.context.index.sample.SampleComponent;
import infra.context.index.sample.SampleController;
import infra.context.index.sample.SampleEmbedded;
import infra.context.index.sample.SampleMetaController;
import infra.context.index.sample.SampleMetaIndexedController;
import infra.context.index.sample.SampleNonStaticEmbedded;
import infra.context.index.sample.SampleNone;
import infra.context.index.sample.SampleRepository;
import infra.context.index.sample.SampleService;
import infra.context.index.sample.cdi.SampleManagedBean;
import infra.context.index.sample.cdi.SampleNamed;
import infra.context.index.sample.cdi.SampleTransactional;
import infra.context.index.sample.jpa.SampleConverter;
import infra.context.index.sample.jpa.SampleEmbeddable;
import infra.context.index.sample.jpa.SampleEntity;
import infra.context.index.sample.jpa.SampleMappedSuperClass;
import infra.context.index.sample.type.Repo;
import infra.context.index.sample.type.SampleRepo;
import infra.context.index.sample.type.SampleSmartRepo;
import infra.context.index.sample.type.SampleSpecializedRepo;
import infra.context.index.sample.type.SmartRepo;
import infra.context.index.sample.type.SpecializedRepo;
import infra.context.index.test.TestCompiler;
import infra.stereotype.Component;
import infra.util.ClassUtils;
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
    CandidateComponentsMetadata metadata = compile("infra/context/index/sample/jpa/package-info");
    assertThat(metadata).has(Metadata.of("infra.context.index.sample.jpa", "package-info"));
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
    String nestedType = "infra.context.index.sample.SampleEmbedded.Another$AnotherPublicCandidate";
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

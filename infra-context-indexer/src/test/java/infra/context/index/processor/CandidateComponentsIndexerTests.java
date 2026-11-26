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

package infra.context.index.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.type.NoType;
import javax.tools.StandardLocation;

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
import jakarta.inject.Named;
import jakarta.persistence.Converter;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;
import jakarta.transaction.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;

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

//  @Test
//  void cdiManagedBean() {
//    testSingleComponent(SampleManagedBean.class, ManagedBean.class);
//  }

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

  @Test
  void incrementalCompilationKeepsUnchangedEntriesAndReplacesOrDropsStaleEntries() throws IOException {
    String deletedType = "infra.context.index.sample.DeletedComponent";
    CandidateComponentsMetadata previous = new CandidateComponentsMetadata();
    previous.add(new ItemMetadata(SampleComponent.class.getName(), Set.of(Component.class.getName())));
    previous.add(new ItemMetadata(SampleService.class.getName(), Set.of("old.Stereotype")));
    previous.add(new ItemMetadata(SampleNone.class.getName(), Set.of(Component.class.getName())));
    previous.add(new ItemMetadata(deletedType, Set.of(Component.class.getName())));
    Path index = this.compiler.getOutputLocation().toPath().resolve(MetadataStore.METADATA_PATH);
    Files.createDirectories(index.getParent());
    try (var output = Files.newOutputStream(index)) {
      PropertiesMarshaller.write(previous, output);
    }

    CandidateComponentsMetadata metadata = compile(SampleService.class, SampleNone.class);

    assertThat(metadata.getItems()).extracting(ItemMetadata::getType)
            .containsExactlyInAnyOrder(SampleComponent.class.getName(), SampleService.class.getName());
    assertThat(metadata).has(Metadata.of(SampleComponent.class, Component.class));
    assertThat(metadata).has(Metadata.of(SampleService.class, Component.class));
  }

  @Test
  void reportsMetadataWriteFailureWithOriginalCause() throws IOException {
    Filer filer = mock(Filer.class);
    ProcessingEnvironment environment = mock(ProcessingEnvironment.class);
    given(environment.getFiler()).willReturn(filer);
    given(filer.getResource(StandardLocation.CLASS_OUTPUT, "", MetadataStore.METADATA_PATH))
            .willThrow(new IOException("No previous index"));
    IOException writeFailure = new IOException("Cannot create index");
    given(filer.createResource(StandardLocation.CLASS_OUTPUT, "", MetadataStore.METADATA_PATH)).willThrow(writeFailure);
    PackageElement packageInfo = mock();
    given(packageInfo.getKind()).willReturn(ElementKind.PACKAGE);
    NoType packageType = mock();
    given(packageInfo.asType()).willReturn(packageType);
    given(packageType.toString()).willReturn("example");
    RoundEnvironment round = mock(RoundEnvironment.class);
    willReturn(Set.of(packageInfo)).given(round).getRootElements();
    given(round.processingOver()).willReturn(true);
    CandidateComponentsIndexer indexer = new CandidateComponentsIndexer();
    indexer.init(environment);

    assertThatIllegalStateException().isThrownBy(() -> indexer.process(Set.of(), round))
            .withMessage("Failed to write metadata")
            .withCause(writeFailure);
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

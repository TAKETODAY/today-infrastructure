/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.buildpack.platform.build;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.buildpack.platform.docker.type.Image;
import cn.taketoday.buildpack.platform.docker.type.ImageArchive;
import cn.taketoday.buildpack.platform.docker.type.ImageConfig;
import cn.taketoday.buildpack.platform.docker.type.ImageReference;
import cn.taketoday.buildpack.platform.json.AbstractJsonTests;
import cn.taketoday.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link EphemeralBuilder}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
class EphemeralBuilderTests extends AbstractJsonTests {

  private static final int EXISTING_IMAGE_LAYER_COUNT = 43;

  @TempDir
  File temp;

  private final BuildOwner owner = BuildOwner.of(123, 456);

  private Image image;

  private ImageReference targetImage;

  private BuilderMetadata metadata;

  private Map<String, String> env;

  private Buildpacks buildpacks;

  private final Creator creator = Creator.withVersion("dev");

  @BeforeEach
  void setup() throws Exception {
    this.image = Image.of(getContent("image.json"));
    this.targetImage = ImageReference.of("my-image:latest");
    this.metadata = BuilderMetadata.fromImage(this.image);
    this.env = new HashMap<>();
    this.env.put("spring", "boot");
    this.env.put("empty", null);
  }

  @Test
  void getNameHasRandomName() throws Exception {
    EphemeralBuilder b1 = new EphemeralBuilder(this.owner, this.image, this.targetImage, this.metadata,
            this.creator, this.env, this.buildpacks);
    EphemeralBuilder b2 = new EphemeralBuilder(this.owner, this.image, this.targetImage, this.metadata,
            this.creator, this.env, this.buildpacks);
    assertThat(b1.getName().toString()).startsWith("pack.local/builder/").endsWith(":latest");
    assertThat(b1.getName().toString()).isNotEqualTo(b2.getName().toString());
  }

  @Test
  void getArchiveHasCreatedByConfig() throws Exception {
    EphemeralBuilder builder = new EphemeralBuilder(this.owner, this.image, this.targetImage, this.metadata,
            this.creator, this.env, this.buildpacks);
    ImageConfig config = builder.getArchive().getImageConfig();
    BuilderMetadata ephemeralMetadata = BuilderMetadata.fromImageConfig(config);
    assertThat(ephemeralMetadata.getCreatedBy().getName()).isEqualTo("Spring Boot");
    assertThat(ephemeralMetadata.getCreatedBy().getVersion()).isEqualTo("dev");
  }

  @Test
  void getArchiveHasTag() throws Exception {
    EphemeralBuilder builder = new EphemeralBuilder(this.owner, this.image, this.targetImage, this.metadata,
            this.creator, this.env, this.buildpacks);
    ImageReference tag = builder.getArchive().getTag();
    assertThat(tag.toString()).startsWith("pack.local/builder/").endsWith(":latest");
  }

  @Test
  void getArchiveHasFixedCreatedDate() throws Exception {
    EphemeralBuilder builder = new EphemeralBuilder(this.owner, this.image, this.targetImage, this.metadata,
            this.creator, this.env, this.buildpacks);
    Instant createInstant = builder.getArchive().getCreateDate();
    OffsetDateTime createDateTime = OffsetDateTime.ofInstant(createInstant, ZoneId.of("UTC"));
    assertThat(createDateTime.getYear()).isEqualTo(1980);
    assertThat(createDateTime.getMonthValue()).isOne();
    assertThat(createDateTime.getDayOfMonth()).isOne();
    assertThat(createDateTime.getHour()).isZero();
    assertThat(createDateTime.getMinute()).isZero();
    assertThat(createDateTime.getSecond()).isOne();
  }

  @Test
  void getArchiveContainsEnvLayer() throws Exception {
    EphemeralBuilder builder = new EphemeralBuilder(this.owner, this.image, this.targetImage, this.metadata,
            this.creator, this.env, this.buildpacks);
    File directory = unpack(getLayer(builder.getArchive(), EXISTING_IMAGE_LAYER_COUNT), "env");
    assertThat(new File(directory, "platform/env/spring")).usingCharset(StandardCharsets.UTF_8).hasContent("boot");
    assertThat(new File(directory, "platform/env/empty")).usingCharset(StandardCharsets.UTF_8).hasContent("");
  }

  @Test
  void getArchiveHasBuilderForLabel() throws Exception {
    EphemeralBuilder builder = new EphemeralBuilder(this.owner, this.image, this.targetImage, this.metadata,
            this.creator, this.env, this.buildpacks);
    ImageConfig config = builder.getArchive().getImageConfig();
    assertThat(config.getLabels())
            .contains(entry(EphemeralBuilder.BUILDER_FOR_LABEL_NAME, this.targetImage.toString()));
  }

  @Test
  void getArchiveContainsBuildpackLayers() throws Exception {
    List<Buildpack> buildpackList = new ArrayList<>();
    buildpackList.add(new TestBuildpack("example/buildpack1", "0.0.1"));
    buildpackList.add(new TestBuildpack("example/buildpack2", "0.0.2"));
    buildpackList.add(new TestBuildpack("example/buildpack3", "0.0.3"));
    this.buildpacks = Buildpacks.of(buildpackList);
    EphemeralBuilder builder = new EphemeralBuilder(this.owner, this.image, this.targetImage, this.metadata,
            this.creator, null, this.buildpacks);
    assertBuildpackLayerContent(builder, EXISTING_IMAGE_LAYER_COUNT,
            "/cnb/buildpacks/example_buildpack1/0.0.1/buildpack.toml");
    assertBuildpackLayerContent(builder, EXISTING_IMAGE_LAYER_COUNT + 1,
            "/cnb/buildpacks/example_buildpack2/0.0.2/buildpack.toml");
    assertBuildpackLayerContent(builder, EXISTING_IMAGE_LAYER_COUNT + 2,
            "/cnb/buildpacks/example_buildpack3/0.0.3/buildpack.toml");
    File orderDirectory = unpack(getLayer(builder.getArchive(), EXISTING_IMAGE_LAYER_COUNT + 3), "order");
    assertThat(new File(orderDirectory, "cnb/order.toml")).usingCharset(StandardCharsets.UTF_8)
            .hasContent(content("order.toml"));
  }

  private void assertBuildpackLayerContent(EphemeralBuilder builder, int index, String s) throws Exception {
    File buildpackDirectory = unpack(getLayer(builder.getArchive(), index), "buildpack");
    assertThat(new File(buildpackDirectory, s)).usingCharset(StandardCharsets.UTF_8).hasContent("[test]");
  }

  private TarArchiveInputStream getLayer(ImageArchive archive, int index) throws Exception {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    archive.writeTo(outputStream);
    TarArchiveInputStream tar = new TarArchiveInputStream(new ByteArrayInputStream(outputStream.toByteArray()));
    for (int i = 0; i <= index; i++) {
      tar.getNextEntry();
    }
    return new TarArchiveInputStream(tar);
  }

  private File unpack(TarArchiveInputStream archive, String name) throws Exception {
    File directory = new File(this.temp, name);
    directory.mkdirs();
    ArchiveEntry entry = archive.getNextEntry();
    while (entry != null) {
      File file = new File(directory, entry.getName());
      if (entry.isDirectory()) {
        file.mkdirs();
      }
      else {
        file.getParentFile().mkdirs();
        try (OutputStream out = new FileOutputStream(file)) {
          IOUtils.copy(archive, out);
        }
      }
      entry = archive.getNextEntry();
    }
    return directory;
  }

  private String content(String fileName) throws IOException {
    InputStream in = getClass().getResourceAsStream(fileName);
    return FileCopyUtils.copyToString(new InputStreamReader(in, StandardCharsets.UTF_8));
  }

}

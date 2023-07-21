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

package cn.taketoday.infra.maven;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarConstants;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;

import cn.taketoday.app.loader.tools.EntryWriter;
import cn.taketoday.app.loader.tools.ImagePackager;
import cn.taketoday.app.loader.tools.LayoutFactory;
import cn.taketoday.app.loader.tools.Libraries;
import cn.taketoday.buildpack.platform.build.AbstractBuildLog;
import cn.taketoday.buildpack.platform.build.BuildLog;
import cn.taketoday.buildpack.platform.build.BuildRequest;
import cn.taketoday.buildpack.platform.build.Builder;
import cn.taketoday.buildpack.platform.build.Creator;
import cn.taketoday.buildpack.platform.build.PullPolicy;
import cn.taketoday.buildpack.platform.docker.TotalProgressEvent;
import cn.taketoday.buildpack.platform.docker.configuration.DockerConfiguration;
import cn.taketoday.buildpack.platform.io.Owner;
import cn.taketoday.buildpack.platform.io.TarArchive;
import cn.taketoday.lang.Version;

/**
 * Package an application into an OCI image using a buildpack.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @author Jeroen Meijer
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class BuildImageMojo extends AbstractPackagerMojo {

  static {
    System.setProperty("org.slf4j.simpleLogger.log.org.apache.http.wire", "ERROR");
  }

  /**
   * Directory containing the source archive.
   */
  @Parameter(defaultValue = "${project.build.directory}", required = true)
  private File sourceDirectory;

  /**
   * Name of the source archive.
   */
  @Parameter(defaultValue = "${project.build.finalName}", readonly = true)
  private String finalName;

  /**
   * Skip the execution.
   */
  @Parameter(property = "infra.build-image.skip", defaultValue = "false")
  private boolean skip;

  /**
   * Classifier used when finding the source archive.
   */
  @Parameter
  private String classifier;

  /**
   * Image configuration, with {@code builder}, {@code runImage}, {@code name},
   * {@code env}, {@code cleanCache}, {@code verboseLogging}, {@code pullPolicy}, and
   * {@code publish} options.
   */
  @Parameter
  private Image image;

  /**
   * Alias for {@link Image#name} to support configuration through command-line
   * property.
   */
  @Parameter(property = "infra.build-image.imageName", readonly = true)
  String imageName;

  /**
   * Alias for {@link Image#builder} to support configuration through command-line
   * property.
   */
  @Parameter(property = "infra.build-image.builder", readonly = true)
  String imageBuilder;

  /**
   * Alias for {@link Image#runImage} to support configuration through command-line
   * property.
   */
  @Parameter(property = "infra.build-image.runImage", readonly = true)
  String runImage;

  /**
   * Alias for {@link Image#cleanCache} to support configuration through command-line
   * property.
   */
  @Parameter(property = "infra.build-image.cleanCache", readonly = true)
  Boolean cleanCache;

  /**
   * Alias for {@link Image#pullPolicy} to support configuration through command-line
   * property.
   */
  @Parameter(property = "infra.build-image.pullPolicy", readonly = true)
  PullPolicy pullPolicy;

  /**
   * Alias for {@link Image#publish} to support configuration through command-line
   * property.
   */
  @Parameter(property = "infra.build-image.publish", readonly = true)
  Boolean publish;

  /**
   * Alias for {@link Image#network} to support configuration through command-line
   * property.
   */
  @Parameter(property = "infra.build-image.network", readonly = true)
  String network;

  /**
   * Alias for {@link Image#createdDate} to support configuration through command-line
   * property.
   */
  @Parameter(property = "infra.build-image.createdDate", readonly = true)
  String createdDate;

  /**
   * Alias for {@link Image#applicationDirectory} to support configuration through
   * command-line property.
   */
  @Parameter(property = "infra.build-image.applicationDirectory", readonly = true)
  String applicationDirectory;

  /**
   * Docker configuration options.
   */
  @Parameter
  private Docker docker;

  /**
   * The type of archive (which corresponds to how the dependencies are laid out inside
   * it). Possible values are {@code JAR}, {@code WAR}, {@code ZIP}, {@code DIR},
   * {@code NONE}. Defaults to a guess based on the archive type.
   */
  @Parameter
  private LayoutType layout;

  /**
   * The layout factory that will be used to create the executable archive if no
   * explicit layout is set. Alternative layouts implementations can be provided by 3rd
   * parties.
   */
  @Parameter
  private LayoutFactory layoutFactory;

  /**
   * Return the type of archive that should be used when building the image.
   *
   * @return the value of the {@code layout} parameter, or {@code null} if the parameter
   * is not provided
   */
  @Override
  protected LayoutType getLayout() {
    return this.layout;
  }

  /**
   * Return the layout factory that will be used to determine the
   * {@link AbstractPackagerMojo.LayoutType} if no explicit layout is set.
   *
   * @return the value of the {@code layoutFactory} parameter, or {@code null} if the
   * parameter is not provided
   */
  @Override
  protected LayoutFactory getLayoutFactory() {
    return this.layoutFactory;
  }

  @Override
  public void execute() throws MojoExecutionException {
    if (this.project.getPackaging().equals("pom")) {
      getLog().debug("build-image goal could not be applied to pom project.");
      return;
    }
    if (this.skip) {
      getLog().debug("skipping build-image as per configuration.");
      return;
    }
    buildImage();
  }

  private void buildImage() throws MojoExecutionException {
    Libraries libraries = getLibraries(Collections.emptySet());
    try {
      DockerConfiguration dockerConfiguration = (this.docker != null) ? this.docker.asDockerConfiguration()
                                                                      : new Docker().asDockerConfiguration();
      BuildRequest request = getBuildRequest(libraries);
      Builder builder = new Builder(new MojoBuildLog(this::getLog), dockerConfiguration);
      builder.build(request);
    }
    catch (IOException ex) {
      throw new MojoExecutionException(ex.getMessage(), ex);
    }
  }

  private BuildRequest getBuildRequest(Libraries libraries) {
    ImagePackager imagePackager = new ImagePackager(getArchiveFile(), getBackupFile());
    Function<Owner, TarArchive> content = (owner) -> getApplicationContent(owner, libraries, imagePackager);
    Image image = (this.image != null) ? this.image : new Image();
    if (image.name == null && this.imageName != null) {
      image.setName(this.imageName);
    }
    if (image.builder == null && this.imageBuilder != null) {
      image.setBuilder(this.imageBuilder);
    }
    if (image.runImage == null && this.runImage != null) {
      image.setRunImage(this.runImage);
    }
    if (image.cleanCache == null && this.cleanCache != null) {
      image.setCleanCache(this.cleanCache);
    }
    if (image.pullPolicy == null && this.pullPolicy != null) {
      image.setPullPolicy(this.pullPolicy);
    }
    if (image.publish == null && this.publish != null) {
      image.setPublish(this.publish);
    }
    if (image.network == null && this.network != null) {
      image.setNetwork(this.network);
    }
    if (image.createdDate == null && this.createdDate != null) {
      image.setCreatedDate(this.createdDate);
    }
    if (image.applicationDirectory == null && this.applicationDirectory != null) {
      image.setApplicationDirectory(this.applicationDirectory);
    }
    return customize(image.getBuildRequest(this.project.getArtifact(), content));
  }

  private TarArchive getApplicationContent(Owner owner, Libraries libraries, ImagePackager imagePackager) {
    ImagePackager packager = configurePackager(imagePackager);
    return new PackagedTarArchive(owner, libraries, packager);
  }

  private File getArchiveFile() {
    // We can use 'project.getArtifact().getFile()' because that was done in a
    // forked lifecycle and is now null
    File archiveFile = getTargetFile(this.finalName, this.classifier, this.sourceDirectory);
    if (!archiveFile.exists()) {
      archiveFile = getSourceArtifact(this.classifier).getFile();
    }
    if (!archiveFile.exists()) {
      throw new IllegalStateException("A jar or war file is required for building image");
    }
    return archiveFile;
  }

  /**
   * Return the {@link File} to use to back up the original source.
   *
   * @return the file to use to back up the original source
   */
  private File getBackupFile() {
    Artifact source = getSourceArtifact(null);
    if (this.classifier != null && !this.classifier.equals(source.getClassifier())) {
      return source.getFile();
    }
    return null;
  }

  private BuildRequest customize(BuildRequest request) {
    return request.withCreator(Creator.withVersion(Version.instance.implementationVersion()));
  }

  /**
   * {@link BuildLog} backed by Mojo logging.
   */
  private static class MojoBuildLog extends AbstractBuildLog {

    private static final long THRESHOLD = Duration.ofSeconds(2).toMillis();

    private final Supplier<Log> log;

    MojoBuildLog(Supplier<Log> log) {
      this.log = log;
    }

    @Override
    protected void log(String message) {
      this.log.get().info(message);
    }

    @Override
    protected Consumer<TotalProgressEvent> getProgressConsumer(String message) {
      return new ProgressLog(message);
    }

    private class ProgressLog implements Consumer<TotalProgressEvent> {

      private final String message;

      private long last;

      ProgressLog(String message) {
        this.message = message;
        this.last = System.currentTimeMillis();
      }

      @Override
      public void accept(TotalProgressEvent progress) {
        log(progress.getPercent());
      }

      private void log(int percent) {
        if (percent == 100 || (System.currentTimeMillis() - this.last) > THRESHOLD) {
          MojoBuildLog.this.log.get().info(this.message + " " + percent + "%");
          this.last = System.currentTimeMillis();
        }
      }

    }

  }

  /**
   * Adapter class to expose the packaged jar as a {@link TarArchive}.
   */
  static class PackagedTarArchive implements TarArchive {

    static final long NORMALIZED_MOD_TIME = TarArchive.NORMALIZED_TIME.toEpochMilli();

    private final Owner owner;

    private final Libraries libraries;

    private final ImagePackager packager;

    PackagedTarArchive(Owner owner, Libraries libraries, ImagePackager packager) {
      this.owner = owner;
      this.libraries = libraries;
      this.packager = packager;
    }

    @Override
    public void writeTo(OutputStream outputStream) throws IOException {
      TarArchiveOutputStream tar = new TarArchiveOutputStream(outputStream);
      tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
      try {
        this.packager.packageImage(this.libraries, (entry, entryWriter) -> write(entry, entryWriter, tar));
      }
      catch (RuntimeException ex) {
        outputStream.close();
        throw new RuntimeException("Error packaging archive for image", ex);
      }
    }

    private void write(ZipEntry jarEntry, EntryWriter entryWriter, TarArchiveOutputStream tar) {
      try {
        TarArchiveEntry tarEntry = convert(jarEntry);
        tar.putArchiveEntry(tarEntry);
        if (tarEntry.isFile()) {
          entryWriter.write(tar);
        }
        tar.closeArchiveEntry();
      }
      catch (IOException ex) {
        throw new IllegalStateException(ex);
      }
    }

    private TarArchiveEntry convert(ZipEntry entry) {
      byte linkFlag = (entry.isDirectory()) ? TarConstants.LF_DIR : TarConstants.LF_NORMAL;
      TarArchiveEntry tarEntry = new TarArchiveEntry(entry.getName(), linkFlag, true);
      tarEntry.setUserId(this.owner.getUid());
      tarEntry.setGroupId(this.owner.getGid());
      tarEntry.setModTime(NORMALIZED_MOD_TIME);
      if (!entry.isDirectory()) {
        tarEntry.setSize(entry.getSize());
      }
      return tarEntry;
    }

  }

}

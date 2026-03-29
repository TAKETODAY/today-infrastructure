package infra.mariadb4j;

import org.jspecify.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import ch.vorburger.exec.ManagedProcessListener;
import infra.core.io.PatternResourceLoader;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/3/27 17:29
 */
class DBConfigurationImpl implements DBConfiguration {

  private final int port;
  private final File socket;
  private final String binariesClassPathLocation;
  private final File baseDir;
  private final File libDir;
  private final File dataDir;
  private final File tmpDir;
  private final boolean isDeletingTemporaryBaseAndDataDirsOnShutdown;
  private final boolean isWindows;
  private final List<String> args;
  private final String osLibraryEnvironmentVarName;

  private final @Nullable String defaultCharacterSet;

  private final @Nullable ManagedProcessListener listener;

  private final boolean isSecurityDisabled;
  private final Function<String, String> getURL;

  @SuppressWarnings("ImmutableMemberCollection")
  private final Map<Executable, Supplier<File>> executables;

  private final PatternResourceLoader resourceLoader;

  DBConfigurationImpl(int port, File socket, String binariesClassPathLocation,
          File baseDir, File libDir, File dataDir, File tmpDir, boolean isWindows, List<String> args,
          String osLibraryEnvironmentVarName, boolean isSecurityDisabled, boolean isDeletingTemporaryBaseAndDataDirsOnShutdown,
          Function<String, String> getURL, @Nullable String defaultCharacterSet, Map<Executable, Supplier<File>> executables,
          @Nullable ManagedProcessListener listener, PatternResourceLoader resourceLoader) {
    this.port = port;
    this.socket = socket;
    this.binariesClassPathLocation = binariesClassPathLocation;
    this.baseDir = baseDir;
    this.libDir = libDir;
    this.dataDir = dataDir;
    this.tmpDir = tmpDir;
    this.isDeletingTemporaryBaseAndDataDirsOnShutdown =
            isDeletingTemporaryBaseAndDataDirsOnShutdown;
    this.isWindows = isWindows;
    this.args = args;
    this.osLibraryEnvironmentVarName = osLibraryEnvironmentVarName;
    this.isSecurityDisabled = isSecurityDisabled;
    this.getURL = getURL;
    this.defaultCharacterSet = defaultCharacterSet;
    this.listener = listener;
    this.executables = Map.copyOf(executables);
    this.resourceLoader = resourceLoader;
  }

  @Override
  public int getPort() {
    return port;
  }

  @Override
  public File getSocketFile() {
    return socket;
  }

  @Override
  public String getBinariesClassPathLocation() {
    return binariesClassPathLocation;
  }

  @Override
  public File getBaseDir() {
    return baseDir;
  }

  @Override
  public File getLibDir() {
    return libDir;
  }

  @Override
  public File getDataDir() {
    return dataDir;
  }

  @Override
  public File getTmpDir() {
    return tmpDir;
  }

  @Override
  public boolean isDeletingTemporaryBaseAndDataDirsOnShutdown() {
    return isDeletingTemporaryBaseAndDataDirsOnShutdown;
  }

  @Override
  public boolean isWindows() {
    return isWindows;
  }

  @Override
  public List<String> getArgs() {
    return args;
  }

  @Override
  public String getOSLibraryEnvironmentVarName() {
    return osLibraryEnvironmentVarName;
  }

  @Override
  public boolean isSecurityDisabled() {
    return isSecurityDisabled;
  }

  @Override
  public String getURL(String dbName) {
    return getURL.apply(dbName);
  }

  @Override
  public @Nullable ManagedProcessListener getProcessListener() {
    return listener;
  }

  @Override
  public @Nullable String getDefaultCharacterSet() {
    return defaultCharacterSet;
  }

  @Override
  public File getExecutable(Executable executable) {
    return executables
            .getOrDefault(
                    executable,
                    () -> {
                      throw new IllegalArgumentException(executable.name());
                    })
            .get();
  }

  @Override
  public PatternResourceLoader getResourceLoader() {
    return resourceLoader;
  }

}

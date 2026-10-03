package io.github.headlesshq.headlessmc.java;

import java.util.List;
import java.util.Optional;

public interface JavaService {
    Optional<Java> getJava(int version);

    List<Java> getJavaVersions();

    List<JavaSource> getSources();

    void refresh();

}

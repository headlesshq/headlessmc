package io.github.headlesshq.headlessmc.java;

import java.util.List;

public interface JavaSource extends Comparable<JavaSource> {
    int SORT_CONFIG = 1000;
    int SORT_HMC = 2000;
    int SORT_DEFAULT = 3000;
    int SORT_GENERAL = 4000;
    int SORT_CURRENT = 5000;

    String getName();

    List<Java> getJavas();

    int sort();

    @Override
    default int compareTo(JavaSource o) {
        return Integer.compare(sort(), o.sort());
    }

}

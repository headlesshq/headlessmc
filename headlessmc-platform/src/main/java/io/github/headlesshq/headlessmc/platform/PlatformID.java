package io.github.headlesshq.headlessmc.platform;

import jakarta.enterprise.util.AnnotationLiteral;

public interface PlatformID {
    String getName();

    String getCapitalizedName();

    AnnotationLiteral<?> getAnnotation();

}

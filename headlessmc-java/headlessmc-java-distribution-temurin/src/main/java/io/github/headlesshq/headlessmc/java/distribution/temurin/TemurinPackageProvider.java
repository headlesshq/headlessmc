package io.github.headlesshq.headlessmc.java.distribution.temurin;

import com.google.auto.service.AutoService;
import io.github.headlesshq.headlessmc.cdi.PackageProvider;

@AutoService(PackageProvider.class)
public class TemurinPackageProvider implements PackageProvider {
    @Override
    public Class<?> getPackageClass() {
        return TemurinPackageProvider.class;
    }

}

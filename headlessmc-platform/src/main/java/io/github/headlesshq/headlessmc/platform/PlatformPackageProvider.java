package io.github.headlesshq.headlessmc.platform;

import com.google.auto.service.AutoService;
import io.github.headlesshq.headlessmc.cdi.PackageProvider;

@AutoService(PackageProvider.class)
public final class PlatformPackageProvider implements PackageProvider {
    @Override
    public Class<?> getPackageClass() {
        return PlatformPackageProvider.class;
    }

}

package io.github.headlesshq.headlessmc.cdi;

import com.google.auto.service.AutoService;

@AutoService(PackageProvider.class)
public final class CdiPackageProvider implements PackageProvider {
    @Override
    public Class<?> getPackageClass() {
        return PackageProvider.class;
    }

}

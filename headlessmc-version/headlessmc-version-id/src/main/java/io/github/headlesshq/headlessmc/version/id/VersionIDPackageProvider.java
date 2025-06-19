package io.github.headlesshq.headlessmc.version.id;

import com.google.auto.service.AutoService;
import io.github.headlesshq.headlessmc.cdi.PackageProvider;

@AutoService(PackageProvider.class)
public class VersionIDPackageProvider implements PackageProvider {
    @Override
    public Class<?> getPackageClass() {
        return VersionIDPackageProvider.class;
    }

}

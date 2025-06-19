package io.github.headlesshq.headlessmc.mods;

import com.google.auto.service.AutoService;
import io.github.headlesshq.headlessmc.cdi.PackageProvider;

@AutoService(PackageProvider.class)
public class ModPackageProvider implements PackageProvider {
    @Override
    public Class<?> getPackageClass() {
        return PackageProvider.class;
    }

}

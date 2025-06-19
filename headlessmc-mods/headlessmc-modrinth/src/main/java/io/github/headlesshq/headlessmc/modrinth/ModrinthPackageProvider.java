package io.github.headlesshq.headlessmc.modrinth;

import com.google.auto.service.AutoService;
import io.github.headlesshq.headlessmc.cdi.PackageProvider;

@AutoService(PackageProvider.class)
public class ModrinthPackageProvider implements PackageProvider {
    @Override
    public Class<?> getPackageClass() {
        return Modrinth.class;
    }

}

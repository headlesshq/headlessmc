package io.github.headlesshq.headlessmc.commons;

import com.google.auto.service.AutoService;
import io.github.headlesshq.headlessmc.cdi.PackageProvider;

@AutoService(PackageProvider.class)
public final class CommonsPackageProvider implements PackageProvider {
    @Override
    public Class<?> getPackageClass() {
        return CommonsPackageProvider.class;
    }

}

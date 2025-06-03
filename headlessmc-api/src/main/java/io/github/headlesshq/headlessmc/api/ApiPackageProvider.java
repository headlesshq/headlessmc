package io.github.headlesshq.headlessmc.api;

import com.google.auto.service.AutoService;
import io.github.headlesshq.headlessmc.cdi.PackageProvider;

@AutoService(PackageProvider.class)
public class ApiPackageProvider implements PackageProvider {
    @Override
    public Class<?> getPackageClass() {
        return ApiPackageProvider.class;
    }

}

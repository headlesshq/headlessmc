package io.github.headlesshq.headlessmc.api.cdi;

import com.google.auto.service.AutoService;
import io.github.headlesshq.headlessmc.api.Application;

@AutoService(PackageProvider.class)
public class ApiPackageProvider implements PackageProvider {
    @Override
    public Class<?> getPackageClass() {
        return Application.class;
    }

}

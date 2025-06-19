package io.github.headlesshq.headlessmc.java.distribution;

import com.google.auto.service.AutoService;
import io.github.headlesshq.headlessmc.cdi.PackageProvider;

@AutoService(PackageProvider.class)
public class JavaDistributionPackageProvider implements PackageProvider {
    @Override
    public Class<?> getPackageClass() {
        return JavaDistributionService.class;
    }

}

package io.github.headlesshq.headlessmc.platform;

import io.github.headlesshq.headlessmc.version.arg.VersionArg;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

import java.io.Serial;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The Vanilla {@link Qualifier} identifies all {@link Platform}
 * related implementations that refer to the {@link VanillaPlatform}.
 *
 * @see <a href=https://www.minecraft.net>https://www.minecraft.net</a>
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Vanilla {
    String PLATFORM_NAME = VersionArg.PLATFORM_VANILLA;
    String CAPITALIZED_PLATFORM_NAME = "Vanilla";

    @Vanilla
    @Dependent
    @SuppressWarnings("ClassExplicitlyAnnotation")
    final class Literal extends AnnotationLiteral<Vanilla> implements Vanilla, PlatformID {
        public static final Literal INSTANCE = new Literal();

        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public String getName() {
            return PLATFORM_NAME;
        }

        @Override
        public String getCapitalizedName() {
            return CAPITALIZED_PLATFORM_NAME;
        }

        @Override
        public AnnotationLiteral<?> getAnnotation() {
            return this;
        }
    }

}

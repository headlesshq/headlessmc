package io.github.headlesshq.headlessmc.api;

import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierDefault;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Bug spotting annotation that marks fields, methods and parameters as non-null.
 */
@Nonnull
@Documented
@Retention(RetentionPolicy.RUNTIME)
@TypeQualifierDefault({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
public @interface EverythingIsNonnullByDefault {

}

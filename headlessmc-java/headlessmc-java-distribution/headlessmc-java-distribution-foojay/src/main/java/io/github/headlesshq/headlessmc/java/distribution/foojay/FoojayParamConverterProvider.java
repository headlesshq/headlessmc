package io.github.headlesshq.headlessmc.java.distribution.foojay;

import eu.hansolo.jdktools.Api;
import eu.hansolo.jdktools.util.OutputFormat;
import eu.hansolo.jdktools.versioning.VersionNumber;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Provider
class FoojayParamConverterProvider implements ParamConverterProvider {
    @Override
    public @Nullable <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        if (VersionNumber.class.isAssignableFrom(rawType)) {
            return new ParamConverter<>() {
                @Override
                public T fromString(String value) {
                    return rawType.cast(VersionNumber.fromText(value));
                }

                @Override
                public String toString(T value) {
                    return URLEncoder.encode(
                        ((VersionNumber) value).toString(
                            OutputFormat.REDUCED_COMPRESSED,
                            true,
                            true
                        ), StandardCharsets.UTF_8
                    );
                }
            };
        }

        if (Api.class.isAssignableFrom(rawType)) {
            return new ParamConverter<>() {
                @Override
                public T fromString(String value) {
                    throw new UnsupportedOperationException(
                        "Cannot parse API object " + rawType + " from string " + value
                    );
                }

                @Override
                public String toString(T value) {
                    return ((Api) value).getApiString();
                }
            };
        }

        return null;
    }

}

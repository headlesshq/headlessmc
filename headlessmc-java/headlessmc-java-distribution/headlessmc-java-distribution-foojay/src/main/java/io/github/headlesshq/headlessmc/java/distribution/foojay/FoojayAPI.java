package io.github.headlesshq.headlessmc.java.distribution.foojay;

import eu.hansolo.jdktools.ArchiveType;
import io.github.headlesshq.headlessmc.net.rest.ApiException;
import io.github.headlesshq.headlessmc.net.rest.ApiExceptionMapper;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * foojay DiscoAPI
 *
 * <p>The foojay (Friends of OpenJDK) discovery api allows users to discover and query for Java packages (jre/jdk) from different distributions.
 *
 * @see <a href=https://api.foojay.io/swagger-ui>https://api.foojay.io/swagger-ui</a>
 */
@RegisterRestClient(baseUri = "https://api.foojay.io")
@RegisterProvider(ApiExceptionMapper.class)
@RegisterProvider(FoojayParamConverterProvider.class)
@Path("/disco/v3.0")
interface FoojayAPI {
    /**
     * Returns a list of all supported distributions
     *
     * @return a list of all supported distributions
     *
     */
    @GET
    @Path("/distributions")
    @Produces({"application/json"})
    Response getDistributions(
        @Nullable @QueryParam("include_versions") Boolean includeVersions,
        @Nullable @QueryParam("include_synonyms") Boolean includeSynonyms,
        @Nullable @QueryParam("discovery_scope_id") List<String> discoveryScopeId
    ) throws ApiException, ProcessingException;

    /**
     * Returns a list of all supported distributions.
     * Unwraps {@link #getDistributions(Boolean, Boolean, List)}.
     *
     * @return a list of all supported distributions
     */
    default List<Distribution> getDistributions() throws ApiException {
        //noinspection Convert2Diamond
        return resolve(
            new GenericType<Result<List<Distribution>>>() {},
            () -> getDistributions(false, false, null)
        );
    }

    /**
     * Returns detailed information about a given distribution
     *
     * @return detailled information about a given distribution
     */
    @GET
    @Path("/distributions/{distro_name}")
    @Produces({"application/json"})
    Response getDistributionByName(@PathParam("distro_name") String distroName) throws ApiException, ProcessingException;

    /**
     * Unwraps {@link #getDistributionByName(String)}.
     *
     * @param distroName the name of the distribution.
     * @return a {@link Distribution} for the given name.
     * @throws ApiException        if the API returns an error status code.
     * @throws ProcessingException if something goes wrong on the client side.
     */
    default Distribution getDistribution(String distroName) throws ApiException, ProcessingException {
        @SuppressWarnings("Convert2Diamond")
        List<Distribution> distributions = resolve(
            new GenericType<Result<List<Distribution>>>() {},
            () -> getDistributionByName(distroName)
        );

        if (distributions.isEmpty()) {
            throw new ApiException("Failed to find Distribution " + distroName);
        } else if (distributions.size() == 1) {
            return distributions.getFirst();
        } else {
            throw new ApiException("Multiple distributions found for " + distroName + ": " + distributions.stream()
                .map(Distribution::name)
                .collect(Collectors.joining(",")));
        }
    }

    /**
     * Returns a list of packages defined by the given parameters The version parameter not only supports different formats for version numbers (e.
     *
     * @return a list of packages defined by the given parameters The version parameter not only supports different formats for version numbers (e.g. 11.9.0.1, 1.8.0_262, 15, 16-ea) but also ranges (e.g. 15.0.1..\\&lt;16). The ranges are defined as follows: VersionNumber1...VersionNumber2 &#x3D;\\&gt; includes VersionNumber1 and VersionNumber2 VersionNumber1.. includes VersionNumber1 and excludes VersionNumber2 VersionNumber1\\&gt;..VersionNUmber2 &#x3D;\\&gt; excludes VersionNumber1 and includes VersionNumber2 VersionNumber1\\&gt;. excludes VersionNumber1 and VersionNumber2
     */
    @GET
    @Path("/packages")
    @Produces({"application/json"})
    Response getPackages(
        @Nullable @QueryParam("version") String version,
        @Nullable @QueryParam("distribution") List<String> distribution,
        @Nullable @QueryParam("operating_system") List<String> operatingSystem,
        @Nullable @QueryParam("architecture") List<String> architecture,
        @Nullable @QueryParam("bitness") Integer bitness,
        @Nullable @QueryParam("archive_type") List<ArchiveType> archiveType,
        @Nullable @QueryParam("package_type") String packageType,
        @Nullable @QueryParam("directly_downloadable") Boolean directlyDownloadable,
        @Nullable @QueryParam("libc_type") List<String> libcType
        /*
        Other unneeded parameters from swagger

        @Nullable @QueryParam("lib_c_type") List<String> libCType, // in swagger doc, but DiscoClient uses libc_type
        @Nullable @QueryParam("jdk_version") Integer jdkVersion,
        @Nullable @QueryParam("version_by_definition") String versionByDefinition,
        @Nullable @QueryParam("distro") List<String> distro,
        @Nullable @QueryParam("release_status") List<String> releaseStatus,
        @Nullable @QueryParam("term_of_support") List<String> termOfSupport,
        @Nullable @QueryParam("fpu") List<String> fpu,
        @Nullable @QueryParam("javafx_bundled") Boolean javafxBundled,
        @Nullable @QueryParam("with_javafx_if_available") Boolean withJavafxIfAvailable,
        @Nullable @QueryParam("latest") String latest,
        @Nullable @QueryParam("feature") List<String> feature,
        @Nullable @QueryParam("signature_available") Boolean signatureAvailable,
        @Nullable @QueryParam("free_to_use_in_production") Boolean freeToUseInProduction,
        @Nullable @QueryParam("tck_tested") String tckTested,
        @Nullable @QueryParam("aqavit_certified") String aqavitCertified,
        @Nullable @QueryParam("discovery_scope_id") List<String> discoveryScopeId,
        @Nullable @QueryParam("match") String match

        */
    ) throws ApiException, ProcessingException;

    /**
     * Returns a list of packages defined by the given parameters.
     * The version parameter not only supports different formats for version numbers
     * (e.g. 11.9.0.1, 1.8.0_262, 15, 16-ea) but also ranges (e.g. 15.0.1..\\&lt;16).
     * The ranges are defined as follows: VersionNumber1...VersionNumber2 &#x3D;\\&gt;
     * includes VersionNumber1 and VersionNumber2 VersionNumber1..
     * includes VersionNumber1 and excludes VersionNumber2 VersionNumber1\\&gt;..VersionNUmber2 &#x3D;\\&gt;
     * excludes VersionNumber1 and includes VersionNumber2 VersionNumber1\\&gt;.
     * excludes VersionNumber1 and VersionNumber2
     *
     * @return a list of packages defined by the given parameters.
     */
    default List<Pkg> packages(
        @Nullable @QueryParam("version") String version,
        @Nullable @QueryParam("distribution") List<String> distribution,
        @Nullable @QueryParam("operating_system") List<String> operatingSystem,
        @Nullable @QueryParam("architecture") List<String> architecture,
        @Nullable @QueryParam("bitness") Integer bitness,
        @Nullable @QueryParam("archive_type") List<ArchiveType> archiveType,
        @Nullable @QueryParam("package_type") String packageType,
        @Nullable @QueryParam("directly_downloadable") Boolean directlyDownloadable,
        @Nullable @QueryParam("libc_type") List<String> libcType
    ) throws ApiException, ProcessingException {
        //noinspection Convert2Diamond
        return resolve(
            new GenericType<Result<List<Pkg>>>() {}, () -> getPackages(
                version,
                distribution,
                operatingSystem,
                architecture,
                bitness,
                archiveType,
                packageType,
                directlyDownloadable,
                libcType
            )
        );
    }

    /**
     * Returns information about a package defined by the given package id
     *
     * @return information about a package defined by the given package id
     */
    @GET
    @Path("/ids/{id}")
    @Produces({"application/json"})
    Response getPkgInfo(@PathParam("id") String id) throws ApiException, ProcessingException;

    /**
     * Returns information about a package defined by the given package id.
     * Unwraps {@link #getPkgInfo(String)}.
     *
     * @return information about a package defined by the given package id
     */
    default PkgInfo pkgInfo(String id) throws ApiException, ProcessingException {
        //noinspection Convert2Diamond
        List<PkgInfo> infos = resolve(new GenericType<Result<List<PkgInfo>>>() {}, () -> getPkgInfo(id));
        if (infos.isEmpty()) {
            throw new ApiException("Failed to find package info " + id);
        } else if (infos.size() == 1) {
            return infos.getFirst();
        } else {
            throw new ApiException("Multiple package infos found for " + id + ": " + infos.stream()
                .map(PkgInfo::toString)
                .collect(Collectors.joining(",")));
        }
    }

    default <T> T resolve(GenericType<Result<T>> type, Supplier<Response> request) throws ApiException {
        Response response = null;
        try {
            response = request.get();
            response.bufferEntity();
            Result<T> result = response.readEntity(type);
            return result.resolve(response);
        } catch (ApiException | ProcessingException e) {
            String body = "<Failed to get response>";
            if (response == null && e instanceof ApiException apiException) {
                 response = apiException.getResponse();
            }

            if (response != null) {
                try {
                    // e.g. 400: {"result":[],"message":"Requested version is not released yet."}
                    body = response.readEntity(String.class);
                } catch (Exception bodyParseException) {
                    body = "<Failed to read response body (" + bodyParseException.getMessage() + ")>";
                    e.addSuppressed(bodyParseException);
                }
            }

            throw new ApiException("Failed to process response: " + body, e);
        }
    }

}

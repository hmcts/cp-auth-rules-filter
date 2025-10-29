package uk.gov.moj.cpp.auth.http;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class RequestAuthActionResolverTest {

    private static final String MEDIA_SJP_DELETE_FINANCIAL_MEANS =
            "application/vnd.sjp.delete-financial-means+json";
    private static final String MEDIA_HEARING_GET_DRAFT_RESULT =
            "application/vnd.hearing.get-draft-result+json";
    private static final String MEDIA_JSON = "application/json";

    private static final String HEADER_CPP_ACTION = "CPP-ACTION";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String HEADER_ACCEPT = "Accept";

    private static final String PATH_HELLO = "/api/hello";
    private static final String METHOD_GET = "GET";
    private static final String COMPUTED_GET_HELLO = "GET /api/hello";

    @InjectMocks
    RequestActionResolver actionResolver;

    @Test
    void extractVendorActionFromContentTypeWithSuffix() {
        final String token = actionResolver.extractVendorAction(MEDIA_SJP_DELETE_FINANCIAL_MEANS);
        assertEquals("sjp.delete-financial-means", token, "Should parse vendor token with +json suffix");
    }

    @Test
    void extractVendorActionIsCaseInsensitiveAndLowerCased() {
        final String weirdCasing = "Application/VnD.HeArInG.GeT-DrAfT-ReSuLt+Json";
        final String token = actionResolver.extractVendorAction(weirdCasing);
        assertEquals("hearing.get-draft-result", token, "Should lower-case and match case-insensitively");
    }

    @Test
    void extractVendorActionReturnsNullWhenNoVendor() {
        final String token = actionResolver.extractVendorAction(MEDIA_JSON);
        assertNull(token, "No vendor token should be found in non-vendor media type");
    }

    @Test
    void extractFirstVendorFromHeaderListPicksFirstLeftToRight() {
        final String acceptHeader = "text/html, " + MEDIA_HEARING_GET_DRAFT_RESULT + ", " + MEDIA_SJP_DELETE_FINANCIAL_MEANS;
        final String token = actionResolver.extractFirstVendorFromHeaderList(acceptHeader);
        assertEquals("hearing.get-draft-result", token, "Should pick the first vendor token left-to-right");
    }

    @Test
    void extractFirstVendorFromHeaderListReturnsNullWhenNoVendor() {
        final String acceptHeader = "text/html, " + MEDIA_JSON;
        final String token = actionResolver.extractFirstVendorFromHeaderList(acceptHeader);
        assertNull(token, "Should return null when no vendor media types present");
    }

    @Test
    void resolvesPrefersContentTypeOverHeaderAndComputedName() {
        final MockHttpServletRequest request = new MockHttpServletRequest(METHOD_GET, PATH_HELLO);
        request.addHeader(HEADER_CONTENT_TYPE, MEDIA_HEARING_GET_DRAFT_RESULT);
        request.addHeader(HEADER_CPP_ACTION, COMPUTED_GET_HELLO);

        final ResolvedAction resolved = actionResolver.resolve(request, HEADER_CPP_ACTION, PATH_HELLO);

        assertEquals("hearing.get-draft-result", resolved.getActionName(), "Content-Type vendor should win");
    }

    @Test
    void resolvesHeaderFlagFalseWhenContentTypeWins() {
        final MockHttpServletRequest request = new MockHttpServletRequest(METHOD_GET, PATH_HELLO);
        request.addHeader(HEADER_CONTENT_TYPE, MEDIA_HEARING_GET_DRAFT_RESULT);
        request.addHeader(HEADER_CPP_ACTION, COMPUTED_GET_HELLO);

        final ResolvedAction resolved = actionResolver.resolve(request, HEADER_CPP_ACTION, PATH_HELLO);

        assertFalse(resolved.isHeaderSupplied(), "Header flag should be false when Content-Type wins");
    }

    @Test
    void resolvesVendorSuppliedTrueWhenFromContentType() {
        final MockHttpServletRequest request = new MockHttpServletRequest(METHOD_GET, PATH_HELLO);
        request.addHeader(HEADER_CONTENT_TYPE, MEDIA_SJP_DELETE_FINANCIAL_MEANS);

        final ResolvedAction resolved = actionResolver.resolve(request, HEADER_CPP_ACTION, PATH_HELLO);

        assertTrue(resolved.isVendorSupplied(), "Vendor flag should be true when resolved from Content-Type");
    }

    @Test
    void resolvesAcceptUsedWhenNoContentTypeVendorName() {
        final MockHttpServletRequest request = new MockHttpServletRequest(METHOD_GET, PATH_HELLO);
        request.addHeader(HEADER_ACCEPT, MEDIA_SJP_DELETE_FINANCIAL_MEANS);

        final ResolvedAction resolved = actionResolver.resolve(request, HEADER_CPP_ACTION, PATH_HELLO);

        assertEquals("sjp.delete-financial-means", resolved.getActionName(), "Should resolve from Accept when no Content-Type vendor");
    }

    @Test
    void resolvesVendorSuppliedTrueWhenFromAccept() {
        final MockHttpServletRequest request = new MockHttpServletRequest(METHOD_GET, PATH_HELLO);
        request.addHeader(HEADER_ACCEPT, MEDIA_HEARING_GET_DRAFT_RESULT);

        final ResolvedAction resolved = actionResolver.resolve(request, HEADER_CPP_ACTION, PATH_HELLO);

        assertTrue(resolved.isVendorSupplied(), "Vendor flag should be true when resolved from Accept");
    }

    @Test
    void resolvesUsesExplicitHeaderWhenNoVendorPresentName() {
        final MockHttpServletRequest request = new MockHttpServletRequest(METHOD_GET, PATH_HELLO);
        request.addHeader(HEADER_CPP_ACTION, COMPUTED_GET_HELLO);
        request.addHeader(HEADER_CONTENT_TYPE, MEDIA_JSON);
        request.addHeader(HEADER_ACCEPT, MEDIA_JSON);

        final ResolvedAction resolved =
                actionResolver.resolve(request, HEADER_CPP_ACTION, PATH_HELLO);

        assertEquals(COMPUTED_GET_HELLO, resolved.getActionName(), "Should use explicit header when no vendor present");
    }

    @Test
    void resolvesHeaderSuppliedTrueWhenExplicitHeaderPresent() {
        final MockHttpServletRequest request = new MockHttpServletRequest(METHOD_GET, PATH_HELLO);
        request.addHeader(HEADER_CPP_ACTION, COMPUTED_GET_HELLO);

        final ResolvedAction resolved =
                actionResolver.resolve(request, HEADER_CPP_ACTION, PATH_HELLO);

        assertTrue(resolved.isHeaderSupplied(), "Header flag should be true when explicit header used");
    }

    @Test
    void resolvesComputedWhenNoVendorOrHeaderName() {
        final MockHttpServletRequest request = new MockHttpServletRequest(METHOD_GET, PATH_HELLO);
        request.addHeader(HEADER_CONTENT_TYPE, MEDIA_JSON);
        request.addHeader(HEADER_ACCEPT, MEDIA_JSON);

        final ResolvedAction resolved = actionResolver.resolve(request, HEADER_CPP_ACTION, PATH_HELLO);

        assertEquals(COMPUTED_GET_HELLO, resolved.getActionName(), "Should compute '<METHOD> <PATH>' when no vendor/header");
    }

    @Test
    void resolvesVendorFlagFalseWhenComputed() {
        final MockHttpServletRequest request = new MockHttpServletRequest(METHOD_GET, PATH_HELLO);

        final ResolvedAction resolved = actionResolver.resolve(request, HEADER_CPP_ACTION, PATH_HELLO);

        assertFalse(resolved.isVendorSupplied(), "Vendor flag should be false when computed");
    }

    @Test
    void resolvesHeaderFlagFalseWhenComputed() {
        final MockHttpServletRequest request = new MockHttpServletRequest(METHOD_GET, PATH_HELLO);

        final ResolvedAction resolved = actionResolver.resolve(request, HEADER_CPP_ACTION, PATH_HELLO);

        assertFalse(resolved.isHeaderSupplied(), "Header flag should be false when computed");
    }

    @Test
    void resolveHandlesNullActionHeaderName() {
        final MockHttpServletRequest request = new MockHttpServletRequest(METHOD_GET, PATH_HELLO);
        // If header name is null, resolver must ignore any physical header present.
        request.addHeader(HEADER_CPP_ACTION, COMPUTED_GET_HELLO);

        final ResolvedAction resolved = actionResolver.resolve(request, null, PATH_HELLO);

        assertEquals(COMPUTED_GET_HELLO, resolved.getActionName(), "Should compute '<METHOD> <PATH>' with null header name");
    }
}

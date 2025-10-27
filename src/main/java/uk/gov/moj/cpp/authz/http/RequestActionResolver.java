package uk.gov.moj.cpp.authz.http;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves an "action name" for authorization with the following priority:
 * 1) Vendor media type token from Content-Type (e.g., application/vnd.sjp.delete-financial-means+json)
 * 2) Vendor media type token from Accept (first vendor match, left-to-right)
 * 3) Explicit action header (e.g., CPP-ACTION)
 * 4) Computed: "<METHOD> <PATH>"
 */
public final class RequestActionResolver {

    private static final String ACCEPT = "Accept";
    private static final Pattern VENDOR_TOKEN_PATTERN =
            Pattern.compile("(?i)\\bapplication/vnd\\.([a-z0-9][a-z0-9._-]*)(?:\\+[^\\s;,]+)?\\b");

    public ResolvedAction resolve(final HttpServletRequest request,
                                  final String actionHeaderName,
                                  final String pathWithinApplication) {

        final String method = request.getMethod();
        final String contentType = request.getContentType();
        final String accept = request.getHeader(ACCEPT);
        final String headerAction = actionHeaderName == null ? null : request.getHeader(actionHeaderName);

        final String resolvedName;
        boolean vendorSupplied = false;
        boolean headerSupplied = false;

        final String vendorFromContentType = extractVendorAction(contentType);
        if (hasText(vendorFromContentType)) {
            resolvedName = vendorFromContentType;
            vendorSupplied = true;
        } else {
            final String vendorFromAccept = extractFirstVendorFromHeaderList(accept);
            if (hasText(vendorFromAccept)) {
                resolvedName = vendorFromAccept;
                vendorSupplied = true;
            } else if (hasText(headerAction)) {
                resolvedName = headerAction;
                headerSupplied = true;
            } else {
                resolvedName = method + " " + pathWithinApplication;
            }
        }

        return new ResolvedAction(resolvedName, vendorSupplied, headerSupplied);
    }

    public String extractVendorAction(final String mediaTypeValue) {
        String result = null;
        if (hasText(mediaTypeValue)) {
            final Matcher matcher = VENDOR_TOKEN_PATTERN.matcher(mediaTypeValue);
            if (matcher.find()) {
                final String token = matcher.group(1);
                result = (token == null) ? result : token.toLowerCase(Locale.ROOT);
            }
        }
        return result;
    }

    public String extractFirstVendorFromHeaderList(final String headerValue) {
        String result = null;
        if (hasText(headerValue)) {
            final String[] parts = headerValue.split(",");
            for (final String raw : parts) {
                final String candidate = raw.trim();
                final String token = extractVendorAction(candidate);
                if (hasText(token)) {
                    result = token;
                    break;
                }
            }
        }
        return result;
    }

    private boolean hasText(final String text) {
        return text != null && !text.isBlank();
    }
}

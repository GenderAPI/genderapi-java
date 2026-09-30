package io.genderapi.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/** The public machine-readable error catalog ({@code GET /errors}). */
public final class ErrorCatalog extends JsonBacked {

    public ErrorCatalog(JsonNode node) {
        super(node);
    }

    /** @return entries keyed by error code, in the server's order */
    public Map<String, ErrorCatalogEntry> getErrors() {
        JsonNode errors = child("errors");
        if (errors == null || !errors.isObject()) {
            return Collections.emptyMap();
        }
        Map<String, ErrorCatalogEntry> out = new LinkedHashMap<>();
        Iterator<Map.Entry<String, JsonNode>> fields = errors.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            out.put(field.getKey(), new ErrorCatalogEntry(field.getValue()));
        }
        return Collections.unmodifiableMap(out);
    }

    /**
     * @param code error code
     * @return the entry, or {@code null} if the code is not in the catalog
     */
    public ErrorCatalogEntry getEntry(String code) {
        return getErrors().get(code);
    }
}

package com.oncall.alert.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.oncall.alert.config.AppProperties;
import com.oncall.alert.exception.OpenSearchQueryException;
import com.oncall.alert.model.ErrorLogEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenSearchService {

    private final OpenSearchClient openSearchClient;
    private final AppProperties appProperties;

    @Value("${opensearch.index-name}")
    private String indexName;

    public String getIndexName() {
        return indexName;
    }

    /**
     * Queries OpenSearch for document-generation error logs in the given time window.
     */
    public List<ErrorLogEntry> fetchErrorLogs(Instant windowStart, Instant windowEnd) {
        try {
            String errorPrefix = appProperties.getErrorMessagePrefix();

            SearchResponse<JsonNode> response = openSearchClient.search(s -> s
                            .index(indexName)
                            .size(1000)
                            .query(q -> q
                                    .bool(b -> b
                                            .must(m -> m
                                                    .range(r -> r
                                                            .date(d -> d
                                                                    .field("@timestamp")
                                                                    .gte(windowStart.toString())
                                                                    .lte(windowEnd.toString())
                                                            )
                                                    )
                                            )
                                            .must(m -> m
                                                    .matchPhrase(mp -> mp
                                                            .field("message")
                                                            .query(errorPrefix)
                                                    )
                                            )
                                    )
                            )
                            .sort(so -> so
                                    .field(f -> f
                                            .field("@timestamp")
                                            .order(SortOrder.Desc)
                                    )
                            ),
                    JsonNode.class
            );

            List<ErrorLogEntry> results = new ArrayList<>();
            for (Hit<JsonNode> hit : response.hits().hits()) {
                try {
                    JsonNode source = hit.source();
                    if (source != null) {
                        results.add(parseLogEntry(source));
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse log entry: {}", e.getMessage());
                }
            }

            log.info("OpenSearch returned {} error logs from index={}", results.size(), indexName);
            return results;

        } catch (Exception e) {
            throw new OpenSearchQueryException(
                    "Failed to query OpenSearch: " + e.getMessage(), e);
        }
    }

    private ErrorLogEntry parseLogEntry(JsonNode source) {
        ErrorLogEntry entry = new ErrorLogEntry();

        if (source.has("@timestamp")) {
            entry.setTimestamp(Instant.parse(source.get("@timestamp").asText()));
        }
        if (source.has("message")) {
            entry.setMessage(source.get("message").asText());
        }

        JsonNode attr = source.path("attribute");
        if (!attr.isMissingNode()) {
            JsonNode errorNode = attr.path("error");
            if (!errorNode.isMissingNode()) {
                entry.setErrorCode(
                        errorNode.has("code") ? errorNode.get("code").asText() : null);
                entry.setErrorMessage(
                        errorNode.has("msg") ? errorNode.get("msg").asText() : null);
            }
        }

        entry.setDocumentType(
                source.has("documenttype") ? source.get("documenttype").asText() : null);
        entry.setApi(
                source.has("api") ? source.get("api").asText() : null);
        entry.setTraceId(
                source.has("traceId") ? source.get("traceId").asText() : null);
        entry.setLevel(
                source.has("level") ? source.get("level").asText() : null);

        return entry;
    }
}

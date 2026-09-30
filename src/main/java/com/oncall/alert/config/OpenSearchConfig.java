package com.oncall.alert.config;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.core5.http.HttpHost;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

@Configuration
public class OpenSearchConfig {

    @Value("${opensearch.url}")
    private String opensearchUrl;

    @Value("${opensearch.username}")
    private String username;

    @Value("${opensearch.password}")
    private String password;

    @Bean
    public OpenSearchClient openSearchClient() throws Exception {
        HttpHost host = HttpHost.create(opensearchUrl);

        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(
                new AuthScope(host),
                new UsernamePasswordCredentials(username, password.toCharArray())
        );

        ApacheHttpClient5TransportBuilder builder =
                ApacheHttpClient5TransportBuilder.builder(host);

        builder.setHttpClientConfigCallback(httpClientBuilder -> {
            try {
                // TLS setup (adjust for production — use a proper trust store)
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new TrustManager[]{
                        new X509TrustManager() {
                            public X509Certificate[] getAcceptedIssuers() {
                                return new X509Certificate[0];
                            }

                            public void checkClientTrusted(X509Certificate[] certs, String t) {
                            }

                            public void checkServerTrusted(X509Certificate[] certs, String t) {
                            }
                        }
                }, new java.security.SecureRandom());

                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                httpClientBuilder.setConnectionManager(
                        PoolingAsyncClientConnectionManagerBuilder.create()
                                .setTlsStrategy(
                                        ClientTlsStrategyBuilder.create()
                                                .setSslContext(sslContext)
                                                .build()
                                ).build()
                );
            } catch (Exception e) {
                throw new RuntimeException("Failed to configure OpenSearch TLS", e);
            }
            return httpClientBuilder;
        });

        return new OpenSearchClient(builder.build());
    }
}

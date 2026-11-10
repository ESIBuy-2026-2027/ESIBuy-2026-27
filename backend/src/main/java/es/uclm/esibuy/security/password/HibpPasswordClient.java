package es.uclm.esibuy.security.password;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class HibpPasswordClient {

    private static final String HIBP_URL = "https://api.pwnedpasswords.com/range/";

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    private final RestClient restClient;

    public HibpPasswordClient(RestClient.Builder builder) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(READ_TIMEOUT);

        this.restClient = builder
                .baseUrl(HIBP_URL)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.USER_AGENT, "ESIBuy/1.0")
                .defaultHeader("Add-Padding", "true")
                .build();
    }

    public List<String> findPasswordHashes(String prefix) {
        try {
            String response = restClient.get()
                    .uri(prefix)
                    .retrieve()
                    .body(String.class);

            if (response == null || response.isBlank()) {
                throw new PasswordCheckUnavailableException();
            }

            return response.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .toList();

        } catch (RestClientException exception) {
            throw new PasswordCheckUnavailableException(exception);
        }
    }
}
package us.pente.analytics.fetch;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.SimpleCommandLinePropertySource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import us.pente.graph.auth.SiteLogin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

@SpringBootApplication
public class AppMain implements CommandLineRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(AppMain.class);
    private static final String POST_URL = "https://pente.org/gameServer/controller/search.zip";
    private static final String POST_PARAMS_FORMAT =
            "format_name=org.pente.gameDatabase.SimpleGameStorerSearchRequestFormat&format_data=moves%%3DK10%%252C%%26response_format%%3Dorg.pente.gameDatabase.ZipFileGameStorerSearchResponseStream%%26response_params%%3DzippedPartNumParam%%253D1%%26results_order%%3D1%%26filter_data%%3Dstart_game_num%%253D%d%%2526end_game_num%%253D%d%%2526player_1_name%%253D%s%%2526player_2_name%%253D%s%%2526game%%253DPente%%2526site%%253DAll%%252520Sites%%2526event%%253DAll%%252520Events%%2526round%%253DAll%%252520Rounds%%2526section%%253DAll%%252520Sections%%2526winner%%253D0";
    private static final int GAMES_PER_ARCHIVE = 100;

    private final ClientHttpRequestFactory httpRequestFactory;

    public static void main(String[] args) throws Exception {
        SpringApplication.run(AppMain.class, args);
    }

    @Autowired
    public AppMain(ClientHttpRequestFactory httpRequestFactory) {
        this.httpRequestFactory = httpRequestFactory;
    }

    @Override
    public void run(String... args) throws Exception {
        SimpleCommandLinePropertySource propertySource = new SimpleCommandLinePropertySource(args);
        String username = propertySource.getProperty("username");
        String password = propertySource.getProperty("password");
        int total = propertySource.containsProperty("total")
                ? Integer.parseInt(propertySource.getProperty("total")) : GAMES_PER_ARCHIVE;

        List<String> cookieHeaders = SiteLogin.login(username, password);

        LOGGER.info("Arguments: username={}, total={}", username, total);
        int iterations = total / GAMES_PER_ARCHIVE;
        if (total % GAMES_PER_ARCHIVE > 0) {
            iterations += 1;
        }
        for (int i = 0; i <= iterations; i++) {
            int startGameNumber = i * GAMES_PER_ARCHIVE;
            int iterCount = GAMES_PER_ARCHIVE;

            LOGGER.info("Fetching game {} to {} as player 1", startGameNumber, startGameNumber + iterCount);
            try (InputStream content = fetch(cookieHeaders, username, "", startGameNumber, iterCount)) {
                writeGameArchive(content, username, "", startGameNumber, iterCount);
            }

            LOGGER.info("Fetching game {} to {} as player 2", startGameNumber, startGameNumber + iterCount);
            try (InputStream content = fetch(cookieHeaders, "", username, startGameNumber, iterCount)) {
                writeGameArchive(content, "", username, startGameNumber, iterCount);
            }
        }
    }

    private Path writeGameArchive(
            InputStream content,
            String player1username,
            String player2username,
            int startGameNumber,
            int numGames) {
        try {
            player1username = player1username.isEmpty() ? "ALL" : player1username;
            player2username = player2username.isEmpty() ? "ALL" : player2username;
            int endGameNumber = startGameNumber + numGames;
            String outFileName = String.format(
                    "pente_%s-%s-%d-%d.zip", player1username, player2username, startGameNumber, endGameNumber);
            Path outPath = new File(outFileName).toPath();
            Files.copy(content, outPath, StandardCopyOption.REPLACE_EXISTING);
            return outPath;
        } catch (IOException ex) {
           throw Throwables.propagate(ex);
        }
    }

    private InputStream fetch(
            List<String> cookieHeaders,
            String player1username,
            String player2username,
            int startGameNumber,
            int numGames) {
        try {
            ClientHttpRequest request = createRequest(cookieHeaders);
            int endGameNumber = startGameNumber + numGames;
            String params = String.format(
                    POST_PARAMS_FORMAT, startGameNumber, endGameNumber, player1username, player2username);
            LOGGER.debug(params);
            OutputStream postBody = request.getBody();
            postBody.write(params.getBytes("UTF-8"));
            return request.execute().getBody();
        } catch (Exception ex) {
            throw Throwables.propagate(ex);
        }
    }

    private ClientHttpRequest createRequest(List<String> cookieHeaders) {
        try {
            Map<String, String> headersMap = ImmutableMap.<String, String>builder()
                    .put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .put("Accept-Encoding", "gzip, deflate")
                    .put("Accept-Language", "en-US,en;q=0.8")
                    .put("Cache-Control", "max-age=0")
                    .put("Content-Type", "application/x-www-form-urlencoded")
                    .put("Host", "pente.org")
                    .put("Origin", "https://pente.org")
                    .put("Upgrade-Insecure-Requests", "1")
                    .build();
            URI uri = new URI(POST_URL);
            ClientHttpRequest request = httpRequestFactory.createRequest(uri, HttpMethod.POST);
            HttpHeaders headers = request.getHeaders();
            headers.setAll(headersMap);
            cookieHeaders.forEach(value -> headers.add("Cookie", value));
            return request;
        } catch (Exception ex) {
            throw Throwables.propagate(ex);
        }
    }
}
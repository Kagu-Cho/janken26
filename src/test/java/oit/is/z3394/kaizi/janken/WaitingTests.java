package oit.is.z3394.kaizi.janken;

import static org.assertj.core.api.Assertions.assertThat;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "spring.datasource.url=jdbc:h2:mem:waiting-tests")
class WaitingTests {
  @Value("${local.server.port}") int port;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void resetWaiting() { jdbc.update("delete from matchinfo where isActive=TRUE"); }

  HttpClient login(String name) throws Exception {
    var client = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
    var response = client.send(HttpRequest.newBuilder(URI.create(base() + "/login"))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .POST(HttpRequest.BodyPublishers.ofString("username=" + URLEncoder.encode(name, StandardCharsets.UTF_8) + "&password=isdev"))
        .build(), HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode()).isEqualTo(302);
    assertThat(response.headers().firstValue("Location").orElseThrow()).doesNotContain("error");
    return client;
  }

  String base() { return "http://localhost:" + port; }

  HttpResponse<String> get(HttpClient client, String path) throws Exception {
    return client.send(HttpRequest.newBuilder(URI.create(base() + path)).build(), HttpResponse.BodyHandlers.ofString());
  }

  @Test
  void handSelectionWaitsAndAppearsForIndependentLogin() throws Exception {
    var honda = login("ほんだ");
    assertThat(get(honda, "/janken").body()).contains("/match?id=3");
    var match = get(honda, "/match?id=3");
    assertThat(match.statusCode()).isEqualTo(200);
    assertThat(match.body()).contains("ほんだ vs いがき", "/wait?id=3&amp;hand=Gu");
    var waiting = get(honda, "/wait?id=3&hand=Gu");
    assertThat(waiting.statusCode()).isEqualTo(200);
    assertThat(waiting.body()).contains("相手の手を待ってます", "Hi ほんだ");
    assertThat(jdbc.queryForMap("select * from matchinfo where isActive=TRUE"))
        .containsEntry("USER1", 2L).containsEntry("USER2", 3L)
        .containsEntry("USER1HAND", "Gu").containsEntry("ISACTIVE", true);
    var igaki = login("いがき");
    var page = get(igaki, "/janken");
    assertThat(page.statusCode()).isEqualTo(200);
    assertThat(page.body()).contains("アクティブな試合", "user1:2 (ほんだ)", "user2:3 (いがき)", "isActive:true");
  }

  @Test
  void invalidSelectionsDoNotCreateWaitingRows() throws Exception {
    var honda = login("ほんだ");
    assertThat(get(honda, "/wait?id=2&hand=Gu").statusCode()).isEqualTo(400);
    assertThat(get(honda, "/wait?id=999&hand=Gu").statusCode()).isEqualTo(400);
    assertThat(get(honda, "/wait?id=3&hand=invalid").statusCode()).isEqualTo(400);
    assertThat(jdbc.queryForObject("select count(*) from matchinfo where isActive=TRUE", Integer.class)).isZero();
  }
}

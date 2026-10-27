package oit.is.z3394.kaizi.janken;

import static org.assertj.core.api.Assertions.assertThat;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
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
  void resetWaiting() {
    jdbc.update("delete from matchinfo where id > 2");
    jdbc.update("delete from matches where id > 3");
  }

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

  @Test
  void secondPlayerCompletesExactlyTheWaitingMatch() throws Exception {
    var honda = login("ほんだ");
    var igaki = login("いがき");
    assertThat(get(honda, "/wait?id=3&hand=Gu").statusCode()).isEqualTo(200);
    int waitingId = jdbc.queryForObject("select id from matchinfo where isActive=TRUE", Integer.class);
    // 先手の再読み込みで待機行を重複作成しない。
    assertThat(get(honda, "/wait?id=3&hand=Gu").statusCode()).isEqualTo(200);
    assertThat(jdbc.queryForObject("select count(*) from matchinfo where isActive=TRUE", Integer.class)).isEqualTo(1);
    assertThat(get(igaki, "/match?id=2").statusCode()).isEqualTo(200);
    assertThat(get(igaki, "/wait?id=2&hand=Choki").statusCode()).isEqualTo(200);
    assertThat(jdbc.queryForObject("select isActive from matchinfo where id=?", Boolean.class, waitingId)).isFalse();
    assertThat(jdbc.queryForMap("select * from matches where isActive=TRUE"))
        .containsEntry("USER1", 2L).containsEntry("USER2", 3L)
        .containsEntry("USER1HAND", "Gu").containsEntry("USER2HAND", "Choki")
        .containsEntry("ISACTIVE", true);
    assertThat(jdbc.queryForObject("select count(*) from matches", Integer.class)).isEqualTo(4);
    assertThat(get(honda, "/janken").body()).doesNotContain("isActive:true");
  }

  @Test
  void inactiveAndUnrelatedWaitingRowsAreNotMatched() throws Exception {
    jdbc.update("insert into matchinfo(user1,user2,user1Hand,isActive) values(1,3,'Pa',TRUE)");
    var igaki = login("いがき");
    assertThat(get(igaki, "/wait?id=2&hand=Choki").statusCode()).isEqualTo(200);
    assertThat(jdbc.queryForObject("select count(*) from matches", Integer.class)).isEqualTo(3);
    assertThat(jdbc.queryForObject("select count(*) from matchinfo where user1=3 and user2=2 and isActive=TRUE", Integer.class)).isEqualTo(1);
    assertThat(jdbc.queryForObject("select isActive from matchinfo where user1=1 and user2=3", Boolean.class)).isTrue();
  }

  HttpResponse<InputStream> stream(HttpClient client, int id) throws Exception {
    return client.sendAsync(HttpRequest.newBuilder(URI.create(base() + "/results/" + id)).build(),
        HttpResponse.BodyHandlers.ofInputStream()).get(5, TimeUnit.SECONDS);
  }

  String readEvents(InputStream stream) throws Exception {
    return CompletableFuture.supplyAsync(() -> {
      try (stream) {
        return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
      } catch (Exception error) {
        throw new RuntimeException(error);
      }
    }).get(5, TimeUnit.SECONDS);
  }

  @Test
  void sseDeliversResultThenDeactivatesAndAllowsLateSecondPlayer() throws Exception {
    var honda = login("ほんだ");
    var igaki = login("いがき");
    var page = get(honda, "/wait?id=3&hand=Gu");
    int id = jdbc.queryForObject("select id from matchinfo where isActive=TRUE", Integer.class);
    assertThat(page.body().replace("\\/", "/")).contains("/results/" + id, "new EventSource", "source.close()", "[戻る]", "href=\"/janken\"");
    var events = stream(honda, id);
    assertThat(events.statusCode()).isEqualTo(200);
    assertThat(events.headers().firstValue("Content-Type").orElseThrow()).startsWith("text/event-stream");
    assertThat(jdbc.queryForObject("select count(*) from matches where isActive=TRUE", Integer.class)).isZero();
    assertThat(get(igaki, "/wait?id=2&hand=Choki").statusCode()).isEqualTo(200);
    String result = readEvents(events.body());
    assertThat(result).contains("event:result", "ほんだ (Gu) vs いがき (Choki): ほんだの勝ち");
    assertThat(result.split("event:result", -1)).hasSize(2);
    assertThat(jdbc.queryForObject("select count(*) from matches where isActive=TRUE", Integer.class)).isZero();
    assertThat(jdbc.queryForObject("select isActive from matchinfo where id=?", Boolean.class, id)).isFalse();
    // 先手に配信済み（matches.isActive=FALSE）の後でも後手に同じ結果を返せる。
    assertThat(readEvents(stream(igaki, id).body())).contains("ほんだの勝ち");
    assertThat(get(honda, "/janken").body()).doesNotContain("isActive:true");
    // 同じ2人が続けて対戦しても、過去の試合結果を配信しない。
    assertThat(get(honda, "/wait?id=3&hand=Pa").statusCode()).isEqualTo(200);
    int nextId = jdbc.queryForObject("select id from matchinfo where isActive=TRUE", Integer.class);
    var nextEvents = stream(honda, nextId);
    get(igaki, "/wait?id=2&hand=Gu");
    assertThat(readEvents(nextEvents.body())).contains("ほんだ (Pa) vs いがき (Gu): ほんだの勝ち")
        .doesNotContain("(Choki)");
  }

  @Test
  void sseRequiresAuthenticationAndParticipation() throws Exception {
    var honda = login("ほんだ");
    get(honda, "/wait?id=3&hand=Gu");
    int id = jdbc.queryForObject("select id from matchinfo where isActive=TRUE", Integer.class);
    assertThat(get(HttpClient.newHttpClient(), "/results/" + id).statusCode()).isEqualTo(302);
    assertThat(get(login("user1"), "/results/" + id).statusCode()).isEqualTo(404);
    assertThat(get(honda, "/results/999999").statusCode()).isEqualTo(404);
  }
}

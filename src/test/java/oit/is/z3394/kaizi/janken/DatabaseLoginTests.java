package oit.is.z3394.kaizi.janken;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.beans.factory.annotation.Value;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "spring.datasource.url=jdbc:h2:mem:login-tests")
class DatabaseLoginTests {
  @Value("${local.server.port}") int port;
  @Autowired JdbcTemplate jdbc;
  @Autowired UserDetailsService users;

  @Test
  void initialDataAndInactiveMatches() {
    assertThat(jdbc.queryForList("select name from users order by id", String.class))
        .containsExactly("CPU", "ほんだ", "いがき");
    var rows = jdbc.queryForList("select * from matchinfo order by id");
    assertThat(rows).hasSize(2);
    assertThat(rows.get(0)).containsEntry("USER1", 1L).containsEntry("USER2", 2L)
        .containsEntry("USER1HAND", "Gu").containsEntry("ISACTIVE", false);
    assertThat(rows.get(1)).containsEntry("USER1", 2L).containsEntry("USER2", 3L)
        .containsEntry("USER1HAND", "Choki").containsEntry("ISACTIVE", false);
    assertThat(jdbc.queryForObject("select count(*) from matches where isActive = true", Integer.class)).isZero();
  }

  @Test
  void bothPlayersCanAuthenticateWithUserRole() {
    for (String name : new String[] {"ほんだ", "いがき"}) {
      var user = users.loadUserByUsername(name);
      assertThat(PasswordEncoderFactories.createDelegatingPasswordEncoder().matches("isdev", user.getPassword())).isTrue();
      assertThat(user.getAuthorities()).extracting("authority").contains("ROLE_USER");
    }
  }

  @Test
  void loginFromIndexDisplaysJanken() throws Exception {
    var client = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
    String base = "http://localhost:" + port;
    var index = client.send(HttpRequest.newBuilder(URI.create(base + "/")).build(), HttpResponse.BodyHandlers.ofString());
    assertThat(index.body()).contains("jankenへのGETをリクエスト");
    var protectedPage = client.send(HttpRequest.newBuilder(URI.create(base + "/janken")).build(), HttpResponse.BodyHandlers.ofString());
    assertThat(protectedPage.statusCode()).isEqualTo(302);
    assertThat(protectedPage.headers().firstValue("Location").orElseThrow()).endsWith("/login");
    var login = client.send(HttpRequest.newBuilder(URI.create(base + "/login"))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .POST(HttpRequest.BodyPublishers.ofString("username=" + URLEncoder.encode("いがき", StandardCharsets.UTF_8) + "&password=isdev"))
        .build(), HttpResponse.BodyHandlers.ofString());
    assertThat(login.statusCode()).isEqualTo(302);
    assertThat(login.headers().firstValue("Location").orElseThrow()).doesNotContain("error");
    var game = client.send(HttpRequest.newBuilder(URI.create(base + "/janken")).build(), HttpResponse.BodyHandlers.ofString());
    assertThat(game.statusCode()).isEqualTo(200);
    assertThat(game.body()).contains("いがき", "エントリー", "試合結果");
    var console = client.send(HttpRequest.newBuilder(URI.create(base + "/h2-console/")).build(), HttpResponse.BodyHandlers.ofString());
    assertThat(console.statusCode()).isEqualTo(200);
  }
}

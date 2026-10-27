package oit.is.z3394.kaizi.janken.service;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import oit.is.z3394.kaizi.janken.model.Match;
import oit.is.z3394.kaizi.janken.model.MatchInfoMapper;
import oit.is.z3394.kaizi.janken.model.MatchMapper;
import oit.is.z3394.kaizi.janken.model.UserMapper;

@Service
public class AsyncKekka {
  private final MatchInfoMapper waiting;
  private final MatchMapper matches;
  private final UserMapper users;

  public AsyncKekka(MatchInfoMapper waiting, MatchMapper matches, UserMapper users) {
    this.waiting = waiting;
    this.matches = matches;
    this.users = users;
  }

  @Async
  public void watch(int waitingId, SseEmitter emitter) {
    AtomicBoolean connected = new AtomicBoolean(true);
    emitter.onCompletion(() -> connected.set(false));
    emitter.onTimeout(() -> {
      connected.set(false);
      emitter.complete();
    });
    emitter.onError(error -> connected.set(false));
    try {
      emitter.send(SseEmitter.event().comment("connected"));
      while (connected.get()) {
        var info = waiting.selectById(waitingId);
        if (info == null) {
          emitter.complete();
          return;
        }
        if (info.getResultId() != null) {
          Match result = matches.selectById(info.getResultId());
          if (result != null) {
            emitter.send(SseEmitter.event().name("result").id(Integer.toString(result.getId()))
                .data(describe(result)));
            // 1接続につき1回だけ結果を配信し、アクティブな結果を処理済みにする。
            // resultIdで紐付けているため、接続が遅れた後手も同じ結果を取得できる。
            if (result.getIsActive()) {
              matches.finish(result.getId());
            }
            emitter.complete();
            return;
          }
        }
        Thread.sleep(200);
      }
    } catch (InterruptedException error) {
      Thread.currentThread().interrupt();
      emitter.complete();
    } catch (IOException | IllegalStateException error) {
      // ブラウザを閉じた接続には配信を続けない。
      emitter.completeWithError(error);
    }
  }

  private String describe(Match match) {
    String first = users.selectById(match.getUser1()).getName();
    String second = users.selectById(match.getUser2()).getName();
    String a = match.getUser1Hand();
    String b = match.getUser2Hand();
    String outcome;
    if (a.equals(b)) {
      outcome = "あいこ";
    } else if ((a.equals("Gu") && b.equals("Choki"))
        || (a.equals("Choki") && b.equals("Pa"))
        || (a.equals("Pa") && b.equals("Gu"))) {
      outcome = first + "の勝ち";
    } else {
      outcome = second + "の勝ち";
    }
    return first + " (" + a + ") vs " + second + " (" + b + "): " + outcome;
  }
}

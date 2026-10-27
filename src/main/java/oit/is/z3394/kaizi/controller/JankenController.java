package oit.is.z3394.kaizi.janken.controller;

import java.security.Principal;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import oit.is.z3394.kaizi.janken.model.Entry;
import oit.is.z3394.kaizi.janken.model.Janken;
import oit.is.z3394.kaizi.janken.model.Match;
import oit.is.z3394.kaizi.janken.model.MatchMapper;
import oit.is.z3394.kaizi.janken.model.MatchInfo;
import oit.is.z3394.kaizi.janken.model.MatchInfoMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import oit.is.z3394.kaizi.janken.model.User;
import oit.is.z3394.kaizi.janken.model.UserMapper;

@Controller
public class JankenController {

  @Autowired
  private Entry entry;

  @Autowired
  private UserMapper userMapper;

  @Autowired
  private MatchMapper matchMapper;

  @Autowired
  private MatchInfoMapper matchInfoMapper;

  @org.springframework.web.bind.annotation.ModelAttribute
  public void activeMatches(ModelMap model) {
    model.addAttribute("activeMatches", matchInfoMapper.selectActive());
  }

  @GetMapping("/wait")
  @Transactional
  public String waitForOpponent(@RequestParam Integer id, @RequestParam String hand,
      Principal principal, ModelMap model) {
    User user = userMapper.selectByName(principal.getName());
    User opponent = userMapper.selectById(id);
    if (user == null || opponent == null || user.getId() == id
        || !java.util.Set.of("Gu", "Choki", "Pa").contains(hand)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "対戦相手と手を確認してください");
    }
    if (id == 1) {
      return fight(id, hand, principal, model);
    }
    matchInfoMapper.lockPlayers(user.getId(), id);
    MatchInfo info = matchInfoMapper.selectWaiting(id, user.getId());
    if (info != null) {
      Match result = new Match(info.getUser1(), user.getId(), info.getUser1Hand(), hand);
      result.setIsActive(true);
      matchMapper.insertMatch(result);
      matchInfoMapper.finish(info.getId());
    } else {
      // 待機ページの再読み込みでは同じ先手のレコードを増やさない。
      info = matchInfoMapper.selectWaiting(user.getId(), id);
      if (info == null) {
        info = new MatchInfo();
        info.setUser1(user.getId());
        info.setUser2(id);
        info.setUser1Hand(hand);
        info.setIsActive(true);
        matchInfoMapper.insert(info);
      }
    }
    model.addAttribute("loginUser", principal.getName());
    model.addAttribute("matchInfoId", info.getId());
    return "wait.html";
  }

  @GetMapping("/janken")
  public String janken(Principal principal, ModelMap model) {

    String loginUser = principal.getName();

    entry.addUser(loginUser);

    model.addAttribute("loginUser", loginUser);
    model.addAttribute("entryUsers", entry.getUsers());

    // DBから全ユーザを取得
    ArrayList<User> users = userMapper.selectAllUsers();
    model.addAttribute("users", users);

    // DBから全試合結果を取得
    ArrayList<Match> matches = matchMapper.selectAllMatches();
    model.addAttribute("matches", matches);

    return "janken.html";
  }

  @PostMapping("/janken")
  public String janken(@RequestParam String name, ModelMap model) {

    model.addAttribute("name", name);

    // DBから全ユーザを取得
    ArrayList<User> users = userMapper.selectAllUsers();
    model.addAttribute("users", users);

    // DBから全試合結果を取得
    ArrayList<Match> matches = matchMapper.selectAllMatches();
    model.addAttribute("matches", matches);

    return "janken.html";
  }

  @GetMapping("/janken/{hand}")
  public String jankenBattle(
      @PathVariable String hand,
      Principal principal,
      ModelMap model) {

    String loginUser = principal.getName();

    entry.addUser(loginUser);

    model.addAttribute("loginUser", loginUser);
    model.addAttribute("entryUsers", entry.getUsers());

    // DBから全ユーザを取得
    ArrayList<User> users = userMapper.selectAllUsers();
    model.addAttribute("users", users);

    // DBから全試合結果を取得
    ArrayList<Match> matches = matchMapper.selectAllMatches();
    model.addAttribute("matches", matches);

    Janken janken = new Janken(hand);
    model.addAttribute("janken", janken);

    return "janken.html";
  }

  @GetMapping("/match")
  public String match(
      @RequestParam Integer id,
      Principal principal,
      ModelMap model) {

    String loginUser = principal.getName();

    User opponent = userMapper.selectById(id);

    model.addAttribute("loginUser", loginUser);
    model.addAttribute("opponent", opponent);

    return "match.html";
  }

  @GetMapping("/fight")
  public String fight(
      @RequestParam Integer id,
      @RequestParam String hand,
      Principal principal,
      ModelMap model) {

    // ログインユーザ
    String loginUser = principal.getName();

    // ログインユーザと対戦相手をDBから取得
    User user = userMapper.selectByName(loginUser);
    User opponent = userMapper.selectById(id);

    // CPUの手は固定でよい
    String cpuHand = "Gu";

    // 試合結果をDBへ登録
    Match match = new Match(
        user.getId(),
        opponent.getId(),
        hand,
        cpuHand);

    matchMapper.insertMatch(match);

    // 勝敗判定
    String result;

    if (hand.equals(cpuHand)) {
      result = "あいこ";
    } else if ((hand.equals("Gu") && cpuHand.equals("Choki"))
        || (hand.equals("Choki") && cpuHand.equals("Pa"))
        || (hand.equals("Pa") && cpuHand.equals("Gu"))) {
      result = "ほんだの勝ち";
    } else {
      result = "CPUの勝ち";
    }

    model.addAttribute("loginUser", loginUser);
    model.addAttribute("opponent", opponent);
    model.addAttribute("userHand", hand);
    model.addAttribute("cpuHand", cpuHand);
    model.addAttribute("result", result);

    return "match.html";
  }
}

package oit.is.z3394.kaizi.janken.model;

public class Janken {

  private String userHand;
  private String cpuHand;
  private String result;

  public Janken(String userHand) {
    this.userHand = userHand;

    // CPUの手はグーで固定
    this.cpuHand = "グー";

    if (userHand.equals(cpuHand)) {
      this.result = "あいこ";
    } else if (userHand.equals("パー")) {
      this.result = "勝ち";
    } else {
      this.result = "負け";
    }
  }

  public String getUserHand() {
    return userHand;
  }

  public String getCpuHand() {
    return cpuHand;
  }

  public String getResult() {
    return result;
  }
}

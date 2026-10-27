package oit.is.z3394.kaizi.janken.model;

public class MatchInfo {
  private int id;
  private int user1;
  private int user2;
  private String user1Hand;
  private boolean isActive;
  private String user1Name;
  private String user2Name;

  public int getId() { return id; }
  public void setId(int id) { this.id = id; }
  public int getUser1() { return user1; }
  public void setUser1(int user1) { this.user1 = user1; }
  public int getUser2() { return user2; }
  public void setUser2(int user2) { this.user2 = user2; }
  public String getUser1Hand() { return user1Hand; }
  public void setUser1Hand(String user1Hand) { this.user1Hand = user1Hand; }
  public boolean getIsActive() { return isActive; }
  public void setIsActive(boolean isActive) { this.isActive = isActive; }
  public String getUser1Name() { return user1Name; }
  public void setUser1Name(String user1Name) { this.user1Name = user1Name; }
  public String getUser2Name() { return user2Name; }
  public void setUser2Name(String user2Name) { this.user2Name = user2Name; }
}

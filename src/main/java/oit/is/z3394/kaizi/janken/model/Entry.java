package oit.is.z3394.kaizi.janken.model;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class Entry {

  private List<String> users = new ArrayList<>();

  public void addUser(String userName) {
    if (!users.contains(userName)) {
      users.add(userName);
    }
  }

  public List<String> getUsers() {
    return users;
  }
}

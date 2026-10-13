package oit.is.z3394.kaizi.janken.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import oit.is.z3394.kaizi.janken.model.Janken;
import org.springframework.web.bind.annotation.PathVariable;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;

import oit.is.z3394.kaizi.janken.model.Entry;

@Controller
public class JankenController {
  @Autowired
  private Entry entry;

  @GetMapping("/janken")
  public String janken(Principal principal, ModelMap model) {

    String loginUser = principal.getName();

    entry.addUser(loginUser);

    model.addAttribute("loginUser", loginUser);
    model.addAttribute("entryUsers", entry.getUsers());

    return "janken.html";
  }

  @PostMapping("/janken")
  public String janken(@RequestParam String name, ModelMap model) {
    model.addAttribute("name", name);
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

    Janken janken = new Janken(hand);
    model.addAttribute("janken", janken);

    return "janken.html";
  }
}

package oit.is.z3394.kaizi.janken.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import oit.is.z3394.kaizi.janken.model.Janken;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class JankenController {

  @GetMapping("/janken")
  public String janken() {
    return "janken.html";
  }

  @PostMapping("/janken")
  public String janken(@RequestParam String name, ModelMap model) {
    model.addAttribute("name", name);
    return "janken.html";
  }

  @GetMapping("/janken/{hand}")
  public String jankenBattle(@PathVariable String hand, ModelMap model) {

    Janken janken = new Janken(hand);

    model.addAttribute("janken", janken);

    return "janken.html";
  }
}

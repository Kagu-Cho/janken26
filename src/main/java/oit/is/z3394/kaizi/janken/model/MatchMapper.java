package oit.is.z3394.kaizi.janken.model;

import java.util.ArrayList;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MatchMapper {

  @Select("SELECT ID, USER1, USER2, USER1HAND, USER2HAND FROM MATCHES")
  ArrayList<Match> selectAllMatches();

  @Insert("INSERT INTO MATCHES (USER1, USER2, USER1HAND, USER2HAND) "
      + "VALUES (#{user1}, #{user2}, #{user1Hand}, #{user2Hand})")
  void insertMatch(Match match);
}

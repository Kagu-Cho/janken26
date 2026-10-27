package oit.is.z3394.kaizi.janken.model;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface MatchInfoMapper {
  // 両ユーザの行を同じ順序でロックし、同時送信による待機や結果の重複を防ぐ。
  @Select("SELECT id FROM users WHERE id IN (#{user1},#{user2}) ORDER BY id FOR UPDATE")
  List<Integer> lockPlayers(@Param("user1") int user1, @Param("user2") int user2);

  @Select("SELECT * FROM matchinfo WHERE user1=#{user1} AND user2=#{user2} "
      + "AND isActive=TRUE ORDER BY id LIMIT 1 FOR UPDATE")
  MatchInfo selectWaiting(@Param("user1") int user1, @Param("user2") int user2);

  @Update("UPDATE matchinfo SET isActive=FALSE WHERE id=#{id} AND isActive=TRUE")
  int finish(int id);

  @Select("SELECT m.*, a.name AS user1Name, b.name AS user2Name FROM matchinfo m "
      + "JOIN users a ON a.id=m.user1 JOIN users b ON b.id=m.user2 "
      + "WHERE m.isActive=TRUE ORDER BY m.id")
  List<MatchInfo> selectActive();

  @Insert("INSERT INTO matchinfo(user1,user2,user1Hand,isActive) "
      + "VALUES(#{user1},#{user2},#{user1Hand},#{isActive})")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  void insert(MatchInfo info);
}

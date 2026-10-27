package oit.is.z3394.kaizi.janken.model;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MatchInfoMapper {
  @Select("SELECT m.*, a.name AS user1Name, b.name AS user2Name FROM matchinfo m "
      + "JOIN users a ON a.id=m.user1 JOIN users b ON b.id=m.user2 "
      + "WHERE m.isActive=TRUE ORDER BY m.id")
  List<MatchInfo> selectActive();

  @Insert("INSERT INTO matchinfo(user1,user2,user1Hand,isActive) "
      + "VALUES(#{user1},#{user2},#{user1Hand},#{isActive})")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  void insert(MatchInfo info);
}

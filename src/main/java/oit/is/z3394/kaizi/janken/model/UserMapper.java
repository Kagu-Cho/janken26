package oit.is.z3394.kaizi.janken.model;

import java.util.ArrayList;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {

  @Select("SELECT ID, NAME FROM USERS")
  ArrayList<User> selectAllUsers();

  @Select("SELECT ID, NAME FROM USERS WHERE ID = #{id}")
  User selectById(int id);

  @Select("SELECT ID, NAME FROM USERS WHERE NAME = #{name}")
  User selectByName(String name);
}

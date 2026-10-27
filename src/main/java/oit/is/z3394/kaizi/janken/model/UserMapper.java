package oit.is.z3394.kaizi.janken.model;

import java.util.ArrayList;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {

  @Select("SELECT ID, NAME FROM USERS")
  ArrayList<User> selectAllUsers();
}

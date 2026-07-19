package com.diy.mapper;

import com.diy.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface UserMapper {
    /**
     * 根据openid查询用户
     * @param openid
     * @return
     */
    @Select("select * from user where openid=#{openid}")
    User getByOpenId(String openid);

    /**
     * 插入数据
     * @param user
     */
    void insert(User user);

    void update(User user);

    @Select("select * from user where id=#{id}")
    User getById(Long userId);

    /**
     * 根据条件动态统计
     * @param map
     * @return
     */
    Integer countByMap(Map map);
}

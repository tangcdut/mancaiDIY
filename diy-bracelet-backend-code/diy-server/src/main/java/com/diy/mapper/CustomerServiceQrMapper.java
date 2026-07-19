package com.diy.mapper;

import com.diy.entity.CustomerServiceQr;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 客服二维码表Mapper
 */
@Mapper
public interface CustomerServiceQrMapper {

    /**
     * 新增客服二维码记录
     */
    @Insert("insert into customer_service_qr(image_path, status) values(#{imagePath}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(CustomerServiceQr record);

    /**
     * 将所有启用的二维码标记为禁用
     */
    @Update("update customer_service_qr set status = 0 where status = 1")
    void disableAll();

    /**
     * 查询当前启用的最新一条客服二维码
     */
    @Select("select id, image_path as imagePath, status, create_time as createTime " +
            "from customer_service_qr where status = 1 order by create_time desc limit 1")
    CustomerServiceQr getActiveOne();
}

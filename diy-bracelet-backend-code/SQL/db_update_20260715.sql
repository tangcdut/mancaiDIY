-- 2026-07-15 珠子实拍图字段扩展
USE `diy_bangle`;

ALTER TABLE `diy_material` 
ADD COLUMN `real_image_url1` VARCHAR(500) DEFAULT NULL COMMENT '实拍展示图1' AFTER `image_url`,
ADD COLUMN `real_image_url2` VARCHAR(500) DEFAULT NULL COMMENT '实拍展示图2' AFTER `real_image_url1`;

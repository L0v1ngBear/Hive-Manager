package my.hive.infrastructure.wechat.mapper;

import my.hive.infrastructure.wechat.WechatSubscribeUser;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WechatSubscribeUserMapper extends BaseMapper<WechatSubscribeUser> {
}

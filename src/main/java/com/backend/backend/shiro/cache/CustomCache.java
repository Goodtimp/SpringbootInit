package com.backend.backend.shiro.cache;

import com.backend.backend.enums.TokenEnum;
import com.backend.backend.jwt.JwtUtil;
import com.backend.backend.redis.RedisUtil;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheException;

import java.util.*;

import static com.backend.backend.enums.RedisEnum.REFRESH_TOKEN_PREFIX;
import static com.backend.backend.enums.RedisEnum.SHIRO_CACHE_PREFIX;
import static com.backend.backend.enums.TokenEnum.SHIRO_CACHE_EXPIRE_TIME;

/**
 * 重写Shiro的Cache保存读取
 * todo 这个类似乎没用，我们用redis进行手动管理
 *
 * @author dolyw.com
 * @date 2018/9/4 17:31
 */
public class CustomCache<K, V> implements Cache<K, V> {
    /**
     * 缓存的key名称获取为shiro:cache:account
     *
     * @param key
     * @return java.lang.String
     * @author dolyw.com
     * @date 2018/9/4 18:33
     */
    private String getKey(Object key) {
        return SHIRO_CACHE_PREFIX.getCode() + JwtUtil.getClaim(key.toString(), TokenEnum.PAYLOAD_USER_ID_TAG.getCode());
    }

    /**
     * 获取缓存
     */
    @Override
    public Object get(Object key) throws CacheException {
        return RedisUtil.get(this.getKey(key));
    }

    /**
     * 保存缓存
     */
    @Override
    public Object put(Object key, Object value) throws CacheException {
        // 读取配置文件，获取Redis的Shiro缓存过期时间
        // 设置Redis的Shiro缓存 似乎没什么用
        return RedisUtil.set(this.getKey(key), value, Long.parseLong(SHIRO_CACHE_EXPIRE_TIME.getCode()));
    }


    /**
     * 移除缓存
     */
    @Override
    public Object remove(Object key) throws CacheException {
        RedisUtil.del(this.getKey(key));
        return null;
    }

    /**
     * 清空所有shiro缓存
     */
    @Override
    public void clear() throws CacheException {
        RedisUtil.clear(REFRESH_TOKEN_PREFIX.getCode());
    }

    /**
     * 缓存的个数
     */
    @Override
    public int size() {
        return RedisUtil.getAllKeyLength(REFRESH_TOKEN_PREFIX.getCode());
    }

    /**
     * 获取所有的key
     */
    @Override
    public Set keys() {
        return RedisUtil.getAllKey(REFRESH_TOKEN_PREFIX.getCode());
    }

    /**
     * 获取所有的value
     */
    @Override
    public Collection values() {
        Set keys = this.keys();
        List<Object> values = new ArrayList<Object>();
        for (Object key : keys) {
            values.add(RedisUtil.get(key.toString()));
        }
        return values;
    }
}

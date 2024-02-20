package blossom.project.core.util;


import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Tuple;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/*redis-jedis工具包*/
@Slf4j
public class JedisUtil {
    public static ReentrantLock lock = new ReentrantLock();
    private final String DIST_LOCK_SUCCESS = "OK";
    private final Long DIST_LOCK_RELEASE_SUCCESS = 1L;
    private final String SET_IF_NOT_EXIST = "NX";
    private final String SET_WITH_EXPIRE_TIME = "PX";
    private JedisPoolUtil jedisPool = new JedisPoolUtil();

    /**
     * 设置指定的key的值，并设置过期时间
     * @param key 键名
     * @param seconds 过期时间，单位：秒
     * @param value 键值
     * @return 设置成功返回true，否则返回false
     */
    public boolean setStringEx(String key, int seconds, String value){
        Jedis jedis = jedisPool.getJedis();
        try {
            jedis.setex(key, seconds, value);
            return true;
        }catch (Exception e) {
            log.debug("setStringEx() key{} throws:{}",key,e.getMessage());
            return false;
        }finally{
            close(jedis);
        }
    }
    /**
     * 通过给定的key和成员列表，获取哈希表中对应成员的值
     * @param key 哈希表的key
     * @param members 成员列表
     * @return 包含对应成员值的列表，如果成员不存在则返回null
     */
    public List<String> getHashMulti(String key, String[] members) {
        Jedis jedis = jedisPool.getJedis();
        try {
            return jedis.hmget(key, members);
        } catch (Exception e) {
            log.debug("getHashMulti() key {} throws:{}", key,e.getMessage());
        } finally {
            close(jedis);
        }
        return null;
    }
    /**
     * 获取哈希表中指定key的所有值
     * @param key 哈希表的key
     * @return 包含哈希表中指定key的所有值的列表，如果key不存在则返回null
     */
    public List<String> getHashValsAll(String key) {
        Jedis jedis = jedisPool.getJedis();
        try {
            return jedis.hvals(key);
        } catch (Exception e) {
            log.debug("getHashValsAll() key {} throws:{}", key,e.getMessage());
        } finally {
            close(jedis);
        }
        return null;
    }
    /**
     * 获取哈希表中指定key的所有字段
     * @param key 哈希表的key
     * @return 包含哈希表中指定key的所有字段的集合，如果key不存在则返回null
     */
    public Set<String> getHashKeysAll(String key) {
        Jedis jedis = jedisPool.getJedis();
        try {
            return jedis.hkeys(key);
        } catch (Exception e) {
            log.debug("getHashKeysAll() key {} throws:{}", key,e.getMessage());
        } finally {
            close(jedis);
        }
        return null;
    }


    /**
     * 向指定的有序集合中添加成员及其分数
     * @param key 键值
     * @param mKey 成员
     * @param score 分数
     * @return 添加成功返回true，否则返回false
     */
    public boolean addScoreSet(String key, String mKey, int score) {
        Jedis jedis = jedisPool.getJedis();
        try {
            jedis.zadd(key, score, mKey);
            return true;
        } catch (Exception e) {
            log.debug("addScoreSet() key {} throws:{}", key,e.getMessage());
            return false;
        } finally {
            close(jedis);
        }
    }


    /**
     * 从指定的有序集合中移除成员及其分数
     * @param key 键值
     * @param mKey 成员
     * @return 移除成功返回true，否则返回false
     */
    public boolean delScoreSet(String key, String mKey) {
        Jedis jedis = jedisPool.getJedis();
        try {
            jedis.zrem(key, mKey);
            return true;
        } catch (Exception e) {
            log.debug("delScoreSet() key {} throws:{}", key,e.getMessage());
            return false;
        } finally {
            close(jedis);
        }
    }


    /**
     * 在有序集合中更新成员的分数
     * @param key 键值
     * @param mKey 成员
     * @param score 分数
     * @return 更新成功返回true，否则返回false
     */
    public boolean changeScoreSet(String key, String mKey, int score) {
        Jedis jedis = jedisPool.getJedis();
        try {
            jedis.zincrby(key, score, mKey);
            return true;
        } catch (Exception e) {
            log.debug("changeScoreSet() key {} throws:{}", key,e.getMessage());
            return false;
        } finally {
            close(jedis);
        }
    }


    /**
     * 获取有序集合中指定范围内的成员列表
     * @param key 键值
     * @param start 起始位置
     * @param end 结束位置
     * @param asc 是否升序排列
     * @return 指定范围内的成员列表
     */
    public Set<String> listScoreSetString(String key, int start, int end, boolean asc) {
        Jedis jedis = jedisPool.getJedis();
        try {
            if (asc) {
                return jedis.zrange(key, start, end);
            } else {
                return jedis.zrevrange(key, start, end);
            }
        } catch (Exception e) {
            log.debug("listScoreSetString() key {} throws:{}", key,e.getMessage());
        } finally {
            close(jedis);
        }
        return null;
    }


    /**    /**
     * 获取有序集合中指定范围内的成员及其分数的列表
     * @param key 键值
     * @param start 起始位置
     * @param end 结束位置
     * @param asc 是否升序排列
     * @return 指定范围内的成员及其分数的列表
     */
    public Set<Tuple> listScoreSetTuple(String key, int start, int end, boolean asc) {
        Jedis jedis = jedisPool.getJedis();
        try {
            if (asc) {
                return jedis.zrangeWithScores(key, start, end);
            } else {
                return jedis.zrevrangeWithScores(key, start, end);
            }
        } catch (Exception e) {
            log.debug("listScoreSetString() key {} throws:{}", key,e.getMessage());
        } finally {
            close(jedis);
        }
        return null;
    }
    /**
     * 获取分布式锁
     * @param lockKey 锁的key
     * @param requestId 请求id
     * @param expireTime 锁的过期时间
     * @return 是否获取到锁
     */
    public boolean getDistributedLock(String lockKey, String requestId, int expireTime) {
        Jedis jedis = jedisPool.getJedis();
        try {
            String result = jedis.set(lockKey, requestId, SET_IF_NOT_EXIST, SET_WITH_EXPIRE_TIME, expireTime);
            if (DIST_LOCK_SUCCESS.equals(result)) {
                return true;
            }
            return false;
        } catch (Exception e) {
            log.debug("getDistributedLock throws {}", e);
        } finally {
            close(jedis);
        }
        return false;
    }


    /**
     * 释放分布式锁
     * @param lockKey 锁的key
     * @param requestId 请求id
     * @return 是否成功释放锁
     */
    public boolean releaseDistributedLock(String lockKey, String requestId) {
        Jedis jedis = jedisPool.getJedis();
        try {
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            Object result = jedis.eval(script, Collections.singletonList(lockKey), Collections.singletonList(requestId));
            if (DIST_LOCK_RELEASE_SUCCESS.equals(result)) {
                return true;
            }
            return false;
        } catch (Exception e) {
            log.debug("releaseDistributedLock throws {}", e.getMessage());
        } finally {
            close(jedis);
        }
        return false;
    }


    public void close(Jedis jedis) {
        if (jedis != null) {
            jedis.close();
        }
    }
    /**
     * 执行Lua脚本
     * @param key 键值
     * @param limit 限制
     * @param expire 过期时间
     * @return 执行结果
     */
    public Object executeScript(String key, int limit, int expire){
        Jedis jedis = jedisPool.getJedis();
        String lua = buildLuaScript();
        //加载Lua脚本到Redis中 以获得脚本的SHA-1散列值
        String scriptLoad =jedis.scriptLoad(lua);
        try {
            //尝试执行Lua脚本
            Object result = jedis.evalsha(scriptLoad, Arrays.asList(key), Arrays.asList(String.valueOf(expire), String.valueOf(limit)));
            System.out.println(result);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (jedis != null) {
                try {
                    jedis.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }



    // 构造lua脚本
    private static String buildLuaScript() {
        String lua = "local num = redis.call('incr', KEYS[1])\n" +
                "if tonumber(num) == 1 then\n" +
                "\tredis.call('expire', KEYS[1], ARGV[1])\n" +
                "\treturn 1\n" +
                "elseif tonumber(num) > tonumber(ARGV[2]) then\n" +
                "\treturn 0\n" +
                "else \n" +
                "\treturn 1\n" +
                "end\n";
        return lua;
    }
}

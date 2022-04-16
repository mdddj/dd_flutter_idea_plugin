package util

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import model.PluginVersion
import java.util.concurrent.TimeUnit


class CacheUtil private constructor(){








    companion object {
        var cache: Cache<String, PluginVersion>? = null

        var CACHE_NAME = "pluginCache"
        // 单例模式
        fun instance() : CacheUtil {
            return CacheUtil()
        }


        fun getCatch(): Cache<String, PluginVersion> {
            if(cache==null) {
                cache = CacheBuilder.newBuilder()
                    .initialCapacity(1)
                    .maximumSize(1000)
                    .expireAfterWrite(1,TimeUnit.DAYS)
                    .build<String,PluginVersion>()

            }
            return cache!!
        }

    }

}
package util

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import model.PluginVersion
import java.util.concurrent.TimeUnit


class CacheUtil private constructor(){

    companion object {
        private var cache: Cache<String, PluginVersion>? = null //可更新的插件,这个只会在项目打开时进行检测
        private var unred: Cache<String,String>? = null // 未使用的插件,这个会在yaml文件每次打开时检测

        // 获取缓存
        fun unredCaChe() : Cache<String,String>{
            if(unred == null) {
                unred = CacheBuilder.newBuilder().initialCapacity(1).maximumSize(1000)
                    .expireAfterWrite(1,TimeUnit.DAYS)
                    .build()
            }
            return unred!!
        }


        fun getCatch(): Cache<String, PluginVersion> {
            if(cache==null) {
                cache = CacheBuilder.newBuilder()
                    .initialCapacity(1)
                    .maximumSize(1000)
                    .expireAfterWrite(1,TimeUnit.DAYS)
                    .build()

            }
            return cache!!
        }

    }

}
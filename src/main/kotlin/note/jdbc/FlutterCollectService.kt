package note.jdbc

import cn.hutool.core.date.DateUtil
import cn.hutool.db.handler.EntityListHandler
import cn.hutool.db.sql.SqlExecutor
import com.intellij.openapi.project.Project
import shop.itbug.fluttercheckversionx.util.toast
import shop.itbug.fluttercheckversionx.util.toastWithError

object FlutterCollectService {



    ///添加收藏
    fun add(pluginName: String,project: Project) {
        try {
            SqlExecutor.execute(SqliteConnectManager.connect,"""
            insert into ${SqliteConnectManager.FlutterPluginTableName} (name,time) values ('${pluginName}','${DateUtil.now()}')
        """.trimIndent())
            project.toast("Saved successfully\n")
        }catch (e:Exception){
            project.toastWithError("Save failed\n$e")
        }
    }


    /**
     * 判断是否存在
     * @return true : 已存在,  false: 不存在
     */

    fun exits(pluginName: String) : Boolean {
        val sql = """
            select name from ${SqliteConnectManager.FlutterPluginTableName} where name=?;
        """.trimIndent()
        val query = SqlExecutor.query(SqliteConnectManager.connect, sql, EntityListHandler(), pluginName)
        return query.isNotEmpty()
    }


    /**
     * 删除
     * @return true : 删除成功
     */
    fun remove(pluginName: String) : Boolean {
        val sql = """
            delete from ${SqliteConnectManager.FlutterPluginTableName} where name='$pluginName'
        """.trimIndent()
        val execute = SqlExecutor.execute(SqliteConnectManager.connect, sql)
        println("删除结果:$execute")
        return execute > 0
    }



}
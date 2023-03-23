package note.jdbc

import cn.hutool.core.date.DateUtil
import cn.hutool.db.Entity
import cn.hutool.db.handler.EntityListHandler
import cn.hutool.db.sql.SqlExecutor
import shop.itbug.fluttercheckversionx.bus.FlutterPluginCollectEvent
import shop.itbug.fluttercheckversionx.bus.FlutterPluginCollectEventType

val defaultAddToCollect = listOf("cupertino_icons", "dd_js_util", "provider", "riverpod", "freezed")

object FlutterCollectService {


    /**
     * 操作前先检查表是否存在,不存在先创建表再进行下一步操作
     */
    private fun check(call: () -> Unit) {

        ///表不存在,先创建
        if (!hasTable()) {
            val success = SqliteConnectManager.createFlutterPluginTable()
            if (success.not()) {
                println("警告⚠️: 表创建失败了.!请检查sql语句是否正确")
                return
            } else {
                ///设置默认收藏
                defaultAddToCollect.forEach { add(it) }
            }
        }
        call.invoke()
    }

    /**
     * 判断插件收藏表是否存在
     * true : 存在
     */
    private fun hasTable(): Boolean {
        return SqliteConnectManager.isExits(SqliteConnectManager.FlutterPluginTableName)
    }

    ///添加收藏
    fun add(pluginName: String): Boolean {
        var success = false
        check {
            success = try {
                val result = SqlExecutor.execute(
                    SqliteConnectManager.connect, """
                    insert into ${SqliteConnectManager.FlutterPluginTableName} (name,time) values ('${pluginName}','${DateUtil.now()}')
                """.trimIndent()
                )
                result >= 1
            } catch (e: Exception) {
                println("警告: ⚠️添加收藏失败:$e")
                false
            }
        }
        if(success){
            FlutterPluginCollectEvent.fire(FlutterPluginCollectEventType.add,pluginName)
        }
        return success
    }


    /**
     * 判断是否存在
     * @return true : 已存在,  false: 不存在
     */

    fun exits(pluginName: String): Boolean {
        var success = false
        check {
            success = try {

                val sql = """
            select name from ${SqliteConnectManager.FlutterPluginTableName} where name=?;
        """.trimIndent()
                val query = SqlExecutor.query(SqliteConnectManager.connect, sql, EntityListHandler(), pluginName)
                query.isNotEmpty()
            } catch (e: Exception) {
                false
            }
        }
        return success
    }


    /**
     * 删除
     * @return true : 删除成功
     */
    fun remove(pluginName: String): Boolean {
        var success = false
        check {
            success = try {

                val sql = """
            delete from ${SqliteConnectManager.FlutterPluginTableName} where name='$pluginName'
        """.trimIndent()
                val execute = SqlExecutor.execute(SqliteConnectManager.connect, sql)
                println("删除结果:$execute")
                execute > 0
            } catch (e: Exception) {
                println("警告: ⚠️删除失败:$e")
                false
            }
        }
        if(success){
            FlutterPluginCollectEvent.fire(FlutterPluginCollectEventType.remove,pluginName)
        }
        return success
    }


    /**
     * 查询所有已经收藏的插件列表
     */
    fun selectAll(): List<Entity> {

        var list = emptyList<Entity>()

        check {
            val sql = """
            select * from ${SqliteConnectManager.FlutterPluginTableName}
        """.trimIndent()
            val query = SqlExecutor.query(SqliteConnectManager.connect, sql, EntityListHandler())
            println(query.size)
            list = query.toList()
        }
        return list

    }

}
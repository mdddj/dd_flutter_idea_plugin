package note.jdbc

import cn.hutool.db.handler.EntityListHandler
import cn.hutool.db.sql.SqlExecutor
import shop.itbug.fluttercheckversionx.util.Util
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

object SqliteConnectManager {

    lateinit var connect: Connection
    const val FlutterPluginTableName = "FlutterPluginsCollect"


    init {
        initConnect()
    }


    /**
     * 创建表
     */
    fun createFlutterPluginTable() : Boolean {
       try{
           if (!isExits(FlutterPluginTableName)) {
               val createTableSql = """
            create table $FlutterPluginTableName(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name varchat(64) NOT NULL,
                time varchat(64) NOT NULL,
                icon varchat(64) DEFAULT '/icons/collect.svg',
                type varchat(64) DEFAULT '',
                ext varchat(64) DEFAULT '',
                ext2 varchat(64) DEFAULT ''
            );
        """.trimIndent()
               SqlExecutor.execute(connect, createTableSql)
               println("flutter插件表创建成功")
               return true
           }
       }catch (e: Exception){
           return false
       }
        return false

    }

    /**
     * 初始化连接jdbc
     */
    private fun initConnect() {
        try {
            val initDbName = "FlutterCheckVersionXNote.db"
            val homePath = Util.userHomePath
            val filePath = homePath + File.separator + initDbName
            val file = File(filePath)
            if (!file.exists()) {
                val isCreate = file.createNewFile()
                if (isCreate) {
                    println("创建成功")
                }
            }
            Class.forName("org.sqlite.JDBC")
            connect = DriverManager.getConnection("jdbc:sqlite:$filePath")
        } catch (e: SQLException) {
            println("连接sql失败:$e")
        }

    }

    /**
     *
     *  检测表是否已经存在
     *
     *   @return true -> 表已经存在
     */
     fun isExits(tableName: String): Boolean {
        try {
            val sql = """
            SELECT name FROM sqlite_master WHERE type='table' AND name=?;
        """.trimIndent()
            val result = SqlExecutor.query(connect, sql, EntityListHandler(), tableName)
            return result.isNotEmpty()
        } catch (e: Exception) {
            println("检测表失败")
            e.printStackTrace()
        }
        return false
    }
}
package note.jdbc

import shop.itbug.fluttercheckversionx.util.Util
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

object SqliteConnectManager {

    private var connect: Connection? = null


    val connection: Connection? = connect

    init {
        initConnect()
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
            println("连接数据库成功!")
        } catch (e: SQLException) {
            println("连接sql失败:$e")
        }

    }
}
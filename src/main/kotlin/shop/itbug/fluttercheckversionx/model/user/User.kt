package shop.itbug.fluttercheckversionx.model.user

import java.io.Serializable

data class User(
    val accountNonExpired: Boolean,
    val accountNonLocked: Boolean,
    val credentialsNonExpired: Boolean,
    val email: String,
    val enabled: Boolean,
    val id: Int,
    val loginNumber: String,
    val loginTime: Long,
    val nickName: String,
    val phone: String,
    val picture: String,
    val status: Int,
    val type: Int,
    val username: String
): Serializable
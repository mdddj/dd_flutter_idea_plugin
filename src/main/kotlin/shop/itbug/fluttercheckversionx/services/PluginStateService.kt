package shop.itbug.fluttercheckversionx.services

import com.intellij.openapi.components.*

@State(
    name = "user",
    storages = [Storage("my-user.xml")]
)
class PluginStateService: PersistentStateComponent<MyUserState> {

    private var token = MyUserState()

    override fun getState(): MyUserState {
        return token
    }

    override fun loadState(state: MyUserState) {
        token = state
    }

    companion object {
        fun getInstance(): PersistentStateComponent<MyUserState> {
            return service<PluginStateService>()
        }
    }
}

class MyUserState {
    var token  = ""

    var username = ""

    override fun toString(): String {
        return "token===$token \n用户名:$username\n"
    }
}

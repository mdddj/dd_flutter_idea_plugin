package shop.itbug.fluttercheckversionx.services;

import com.intellij.util.messages.Topic;
import shop.itbug.fluttercheckversionx.socket.ProjectSocketService;

public interface SokcetMessageBus {

    Topic<SokcetMessageBus> CHANGE_ACTION_TOPIC = Topic.create("dio request send", SokcetMessageBus.class);



    void handleData(ProjectSocketService.SocketResponseModel data);

}

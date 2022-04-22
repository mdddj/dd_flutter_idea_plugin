package form.components;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


///表格的操作工具栏
public class DioTableToolbar  {



   public ActionToolbar create(){
        ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar(
                "jtable-bar",
                new MyActionGroups(),
                true
        );
        return actionToolbar;
    }

}

class MyActionGroups extends DefaultActionGroup {


    @Override
    public AnAction  [] getChildren(@Nullable AnActionEvent e) {
        AnAction anAction = new AnAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent anActionEvent) {

            }
        };
        anAction.getTemplatePresentation().setIcon(AllIcons.Actions.AddFile);

        return new AnAction[]{anAction};
    }
}
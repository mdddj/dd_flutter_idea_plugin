package form.socket;

import cn.hutool.core.lang.Console;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.table.JBTable;
import form.dialog.DetailDialog;
import form.sub.CustomTableColumnRender;
import services.SokcetMessageBus;
import socket.ProjectSocketService;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

// 监听http请求的窗口

public class SocketRequestForm {
    private JPanel myJanel;
    private JPanel contaner;
    private JPanel detailPanel;
    private JTable table1;
    private JScrollPane scrollPanel;

    DioRequestTableModel dioRequestTableModel;


    Project project;


    public SocketRequestForm(ToolWindow toolWindow) {
        Project defaultProject = toolWindow.getProject();
        this.project = defaultProject;
        refreshData();
        defaultProject.getMessageBus().connect().subscribe(SokcetMessageBus.CHANGE_ACTION_TOPIC, data -> {
            refreshData();
        });
        updateRowWidth();
        Presentation presentation = new Presentation("清空");
        presentation.setIcon(AllIcons.Actions.AddFile);



        table1.setDefaultRenderer(Object.class,new CustomTableColumnRender());




        //监听表格的双击事件
        table1.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    //表示双击
                    showRequestDetail(e);
                }
                super.mouseClicked(e);
            }
        });
    }


    /**
     * 当用户双击后,弹出请求详情数据
     */
    private void showRequestDetail(MouseEvent e) {



        int selectedRow = table1.getSelectedRow(); // 用户选中的行数
        if (selectedRow != -1) {


            detailPanel.removeAll();

            ///设置模型
            ProjectSocketService.SocketResponseModel socketResponseModel = dioRequestTableModel.getDatas().get(selectedRow);


            if(socketResponseModel!=null){

                new DetailDialog(project,socketResponseModel).show();
            }else{
                Console.log("双击了,但是没有获取到model");
            }



        }
    }

    /**
     * 清空表格数据
     */
    private void cleanData() {
        SwingUtilities.invokeLater(() -> {
            this.table1.setModel(new DioRequestTableModel(4, new ArrayList<>()));
            project.getService(ProjectSocketService.class).clean();
        });
    }

    /// 改变列宽
    private void updateRowWidth() {

    }


    /// 刷新数据
    public void refreshData() {
        SwingUtilities.invokeLater(() -> {
            ProjectSocketService service = project.getService(ProjectSocketService.class);
            dioRequestTableModel = new DioRequestTableModel(4, service.getRequests());
            table1.setModel(dioRequestTableModel);
        });

    }


    public JPanel getContent() {
        return this.contaner;
    }

    private void createUIComponents() {
        int columnLen = 4;
        detailPanel = new JPanel();
        table1 = new JBTable(new DioRequestTableModel(columnLen, new ArrayList<>()));
    }
}


/// 表格模型
class DioRequestTableModel extends AbstractTableModel {

    private int column; // 列
    private List<ProjectSocketService.SocketResponseModel> datas; // 数据列表


    public List<ProjectSocketService.SocketResponseModel> getDatas() {
        return datas;
    }

    public DioRequestTableModel(int column, List<ProjectSocketService.SocketResponseModel> datas) {
        this.datas = datas;
        this.column = column;
    }


    @Override
    public int getRowCount() {
        return datas.size();
    }

    @Override
    public int getColumnCount() {
        return this.column;
    }


    @Override
    public String getColumnName(int column) {
        switch (column) {
            case 0:
                return "Url";
            case 1:
                return "Methed";
            case 2:
                return "Status";
            case 3:
                return "Timestamp";
        }
        return super.getColumnName(column);
    }


    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        ProjectSocketService.SocketResponseModel data = datas.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return data.getUrl();
            case 1:
                return data.getMethed();
            case 2:
                return data.getStatusCode();
            case 3:
                return data.getTimesatamp();
            default:
                break;
        }
        return "1";
    }


}



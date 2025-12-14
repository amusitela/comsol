/*
 * CylinderFlowGUI.java - COMSOL 仿真配置 GUI
 * 避免匿名内部类，兼容 COMSOL 编译器
 * 集成 Gemini AI 自然语言配置助手
 */

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultCaret;

public class CylinderFlowGUI extends JFrame implements ActionListener {

    private SimulationConfig config;
    private static final String CONFIG_FILE = "config.json";
    private Map<String, Object> inputFields = new HashMap<String, Object>();

    // Buttons as fields for action handling
    private JButton loadBtn, saveBtn, defaultBtn, runBtn;

    // AI 助手相关字段
    private GeminiClient geminiClient;
    private JTextArea aiChatHistory;
    private JTextField aiInputField;
    private JButton aiSendBtn, aiApplyBtn, aiClearBtn;
    private JTextArea aiChangesPreview;
    private List<AIConfigParser.ConfigChange> pendingChanges;
    private boolean aiProcessing = false;

    // Colors - Light Theme for Better Readability
    private static final Color BG_DARK = new Color(245, 245, 250);
    private static final Color BG_PANEL = new Color(255, 255, 255);
    private static final Color BG_INPUT = new Color(250, 250, 252);
    private static final Color TEXT_PRIMARY = new Color(30, 30, 40);
    private static final Color TEXT_SECONDARY = new Color(100, 100, 120);
    private static final Color ACCENT_BLUE = new Color(25, 118, 210);
    private static final Color ACCENT_GREEN = new Color(46, 125, 50);
    private static final Color ACCENT_ORANGE = new Color(230, 81, 0);
    private static final Color ACCENT_PURPLE = new Color(103, 58, 183);
    private static final Color BORDER_COLOR = new Color(200, 200, 210);

    public CylinderFlowGUI() {
        loadConfig();
        initGeminiClient();
        initUI();
    }

    private void initGeminiClient() {
        geminiClient = new GeminiClient();
        if (!geminiClient.isConfigured()) {
            System.out.println("提示: 设置环境变量 GEMINI_API_KEY 以启用 AI 助手功能");
        }
    }

    private void loadConfig() {
        try {
            config = ConfigManager.loadConfig(CONFIG_FILE);
        } catch (IOException e) {
            config = SimulationConfig.getDefault();
            System.out.println("Using default config: " + e.getMessage());
        }
    }

    private void initUI() {
        setTitle("COMSOL 圆柱绕流仿真配置");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 750);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_DARK);

        setUIFont();

        setLayout(new BorderLayout(10, 10));
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(15, 15, 15, 15));

        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createTabbedPane(), BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);
    }

    private void setUIFont() {
        Font defaultFont = new Font("Microsoft YaHei UI", Font.PLAIN, 14);
        UIManager.put("Label.font", defaultFont);
        UIManager.put("TextField.font", defaultFont);
        UIManager.put("ComboBox.font", defaultFont);
        UIManager.put("Button.font", new Font("Microsoft YaHei UI", Font.BOLD, 14));
        UIManager.put("TabbedPane.font", new Font("Microsoft YaHei UI", Font.BOLD, 14));
        UIManager.put("CheckBox.font", defaultFont);
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_DARK);
        panel.setBorder(new EmptyBorder(0, 0, 15, 0));

        JLabel title = new JLabel("COMSOL 圆柱绕流仿真配置");
        title.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 26));
        title.setForeground(TEXT_PRIMARY);

        JLabel subtitle = new JLabel("配置卡门涡街 (Karman Vortex Street) 仿真参数");
        subtitle.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 14));
        subtitle.setForeground(TEXT_SECONDARY);

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        textPanel.setBackground(BG_DARK);
        textPanel.add(title);
        textPanel.add(subtitle);

        panel.add(textPanel, BorderLayout.WEST);
        return panel;
    }

    private JTabbedPane createTabbedPane() {
        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.LEFT);
        tabbedPane.setBackground(BG_PANEL);
        tabbedPane.setForeground(TEXT_PRIMARY);

        tabbedPane.addTab("几何参数", createGeometryPanel());
        tabbedPane.addTab("边界条件", createBoundaryPanel());
        tabbedPane.addTab("物理参数", createPhysicsPanel());
        tabbedPane.addTab("材料参数", createMaterialPanel());
        tabbedPane.addTab("网格参数", createMeshPanel());
        tabbedPane.addTab("求解参数", createSolverPanel());
        tabbedPane.addTab("输出参数", createOutputPanel());
        tabbedPane.addTab("🤖 AI 助手", createAIPanel());

        return tabbedPane;
    }

    private JPanel createGeometryPanel() {
        JPanel panel = createBasePanel();
        GridBagConstraints gbc = createGBC();

        int row = 0;
        addSectionTitle(panel, gbc, row++, "计算域尺寸");
        addDoubleField(panel, gbc, row++, "域宽度 (m)", "domainWidth", config.domainWidth);
        addDoubleField(panel, gbc, row++, "域高度 (m)", "domainHeight", config.domainHeight);

        addSectionTitle(panel, gbc, row++, "圆柱障碍物");
        addDoubleField(panel, gbc, row++, "圆柱半径 (m)", "cylinderRadius", config.cylinderRadius);
        addDoubleField(panel, gbc, row++, "圆柱 X 坐标 (m)", "cylinderX", config.cylinderX);
        addDoubleField(panel, gbc, row++, "圆柱 Y 坐标 (m)", "cylinderY", config.cylinderY);

        addFiller(panel, gbc, row);
        return wrapInScrollPane(panel);
    }

    private JPanel createBoundaryPanel() {
        JPanel panel = createBasePanel();
        GridBagConstraints gbc = createGBC();

        int row = 0;
        addSectionTitle(panel, gbc, row++, "入口边界 (Inlet)");
        addComboField(panel, gbc, row++, "边界类型", "inletType",
                new String[] { "Velocity", "Pressure" }, config.inletType);
        addDoubleField(panel, gbc, row++, "入口速度 (m/s)", "inletVelocity", config.inletVelocity);
        addDoubleField(panel, gbc, row++, "入口压力 (Pa)", "inletPressure", config.inletPressure);

        addSectionTitle(panel, gbc, row++, "出口边界 (Outlet)");
        addComboField(panel, gbc, row++, "边界类型", "outletType",
                new String[] { "Pressure", "Velocity", "Outflow" }, config.outletType);
        addDoubleField(panel, gbc, row++, "出口压力 (Pa)", "outletPressure", config.outletPressure);
        addDoubleField(panel, gbc, row++, "出口速度 (m/s)", "outletVelocity", config.outletVelocity);

        addSectionTitle(panel, gbc, row++, "上下边界");
        addComboField(panel, gbc, row++, "上边界类型", "topBoundaryType",
                new String[] { "Symmetry", "Wall", "Slip" }, config.topBoundaryType);
        addComboField(panel, gbc, row++, "下边界类型", "bottomBoundaryType",
                new String[] { "Symmetry", "Wall", "Slip" }, config.bottomBoundaryType);

        addSectionTitle(panel, gbc, row++, "圆柱壁面");
        addComboField(panel, gbc, row++, "壁面类型", "cylinderWallType",
                new String[] { "Wall", "Slip" }, config.cylinderWallType);
        addComboField(panel, gbc, row++, "壁面条件", "cylinderWallCondition",
                new String[] { "NoSlip", "Slip" }, config.cylinderWallCondition);

        addFiller(panel, gbc, row);
        return wrapInScrollPane(panel);
    }

    private JPanel createPhysicsPanel() {
        JPanel panel = createBasePanel();
        GridBagConstraints gbc = createGBC();

        int row = 0;
        addSectionTitle(panel, gbc, row++, "流动模型");
        addComboField(panel, gbc, row++, "流动类型", "flowType",
                new String[] { "Laminar", "Turbulent" }, config.flowType);
        addComboField(panel, gbc, row++, "方程形式", "equationForm",
                new String[] { "Transient", "Stationary" }, config.equationForm);

        addFiller(panel, gbc, row);
        return wrapInScrollPane(panel);
    }

    private JPanel createMaterialPanel() {
        JPanel panel = createBasePanel();
        GridBagConstraints gbc = createGBC();

        int row = 0;
        addSectionTitle(panel, gbc, row++, "流体属性");

        // Fluid preset selector
        gbc.gridx = 0;
        gbc.gridy = row;
        JLabel presetLabel = new JLabel("流体预设");
        presetLabel.setForeground(TEXT_PRIMARY);
        panel.add(presetLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JComboBox<String> fluidPreset = new JComboBox<>(new String[] { "自定义", "空气 (Air)", "水 (Water)" });
        fluidPreset.setBackground(BG_INPUT);
        fluidPreset.setForeground(TEXT_PRIMARY);
        fluidPreset.addActionListener(e -> {
            String selected = (String) fluidPreset.getSelectedItem();
            if ("空气 (Air)".equals(selected)) {
                setFluidPreset("Air", 1.225, 1.7894e-5);
            } else if ("水 (Water)".equals(selected)) {
                setFluidPreset("Water", 998.0, 1.002e-3);
            }
        });
        panel.add(fluidPreset, gbc);
        gbc.weightx = 0;
        row++;

        addTextField(panel, gbc, row++, "流体名称", "fluidName", config.fluidName);
        addDoubleField(panel, gbc, row++, "密度 (kg/m3)", "density", config.density);
        addDoubleField(panel, gbc, row++, "动力粘度 (Pa*s)", "dynamicViscosity", config.dynamicViscosity);
        addNote(panel, gbc, row++, "动力粘度使用科学计数法，如 1.7894e-5 (空气) 或 1.002e-3 (水)");

        addFiller(panel, gbc, row);
        return wrapInScrollPane(panel);
    }

    // Helper method to apply fluid preset
    private void setFluidPreset(String name, double density, double viscosity) {
        Object nameField = inputFields.get("fluidName");
        Object densityField = inputFields.get("density");
        Object viscosityField = inputFields.get("dynamicViscosity");

        if (nameField instanceof JTextField) {
            ((JTextField) nameField).setText(name);
        }
        if (densityField instanceof JTextField) {
            ((JTextField) densityField).setText(String.valueOf(density));
        }
        if (viscosityField instanceof JTextField) {
            ((JTextField) viscosityField).setText(String.valueOf(viscosity));
        }
    }

    private JPanel createMeshPanel() {
        JPanel panel = createBasePanel();
        GridBagConstraints gbc = createGBC();

        int row = 0;
        addSectionTitle(panel, gbc, row++, "全局网格");
        addIntField(panel, gbc, row++, "网格精度等级 (1-9)", "meshSizeLevel", config.meshSizeLevel);
        addDoubleField(panel, gbc, row++, "最大单元尺寸 (m)", "meshMaxSize", config.meshMaxSize);
        addDoubleField(panel, gbc, row++, "最小单元尺寸 (m)", "meshMinSize", config.meshMinSize);

        addSectionTitle(panel, gbc, row++, "局部加密");
        addDoubleField(panel, gbc, row++, "圆柱区域最大单元 (m)", "cylinderMeshMaxSize", config.cylinderMeshMaxSize);
        addNote(panel, gbc, row++, "网格等级: 1=极细, 3=细, 5=正常, 7=粗, 9=极粗");

        addFiller(panel, gbc, row);
        return wrapInScrollPane(panel);
    }

    private JPanel createSolverPanel() {
        JPanel panel = createBasePanel();
        GridBagConstraints gbc = createGBC();

        int row = 0;
        addSectionTitle(panel, gbc, row++, "时间设置");
        addDoubleField(panel, gbc, row++, "开始时间 (s)", "startTime", config.startTime);
        addDoubleField(panel, gbc, row++, "结束时间 (s)", "endTime", config.endTime);
        addDoubleField(panel, gbc, row++, "时间步长 (s)", "timeStep", config.timeStep);
        addNote(panel, gbc, row++, "仿真时长 = 结束时间 - 开始时间");

        addFiller(panel, gbc, row);
        return wrapInScrollPane(panel);
    }

    private JPanel createOutputPanel() {
        JPanel panel = createBasePanel();
        GridBagConstraints gbc = createGBC();

        int row = 0;
        addSectionTitle(panel, gbc, row++, "输出路径");
        addTextField(panel, gbc, row++, "工作目录 (空=当前)", "outputDir", config.outputDir);
        addTextField(panel, gbc, row++, "模型文件名", "modelFileName", config.modelFileName);

        addSectionTitle(panel, gbc, row++, "图像导出");
        addCheckBox(panel, gbc, row++, "导出速度云图", "exportVelocity", config.exportVelocity);
        addCheckBox(panel, gbc, row++, "导出涡量云图", "exportVorticity", config.exportVorticity);

        addSectionTitle(panel, gbc, row++, "动画导出");
        addCheckBox(panel, gbc, row++, "导出 GIF 动画", "exportAnimation", config.exportAnimation);
        addIntField(panel, gbc, row++, "动画帧率 (fps)", "animationFps", config.animationFps);
        addIntField(panel, gbc, row++, "最大帧数", "animationMaxFrames", config.animationMaxFrames);

        addFiller(panel, gbc, row);
        return wrapInScrollPane(panel);
    }

    // ============================================
    // AI 助手面板
    // ============================================

    private JPanel createAIPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(BG_PANEL);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // 顶部状态栏
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBackground(BG_PANEL);

        JLabel aiTitle = new JLabel("🤖 AI 配置助手 (Gemini)");
        aiTitle.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 16));
        aiTitle.setForeground(ACCENT_PURPLE);

        JLabel statusLabel = new JLabel();
        if (geminiClient.isConfigured()) {
            statusLabel.setText("✓ API 已配置");
            statusLabel.setForeground(ACCENT_GREEN);
        } else {
            statusLabel.setText("✗ 请设置环境变量 GEMINI_API_KEY");
            statusLabel.setForeground(ACCENT_ORANGE);
        }
        statusLabel.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));

        statusPanel.add(aiTitle, BorderLayout.WEST);
        statusPanel.add(statusLabel, BorderLayout.EAST);

        // 聊天历史区域
        aiChatHistory = new JTextArea();
        aiChatHistory.setEditable(false);
        aiChatHistory.setLineWrap(true);
        aiChatHistory.setWrapStyleWord(true);
        aiChatHistory.setBackground(new Color(248, 248, 252));
        aiChatHistory.setForeground(TEXT_PRIMARY);
        aiChatHistory.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        aiChatHistory.setBorder(new EmptyBorder(10, 10, 10, 10));
        aiChatHistory.setText(
                "欢迎使用 AI 配置助手！\n\n你可以用自然语言描述想要修改的配置，例如：\n• \"把入口速度改成 0.05 m/s\"\n• \"使用水作为流体\"\n• \"把仿真时间延长到 300 秒\"\n• \"加密网格，把最大单元改成 0.005\"\n\n---\n\n");

        // 自动滚动到底部
        DefaultCaret caret = (DefaultCaret) aiChatHistory.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        JScrollPane chatScrollPane = new JScrollPane(aiChatHistory);
        chatScrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        chatScrollPane.setPreferredSize(new Dimension(400, 200));

        // 变更预览区域
        JPanel previewPanel = new JPanel(new BorderLayout(5, 5));
        previewPanel.setBackground(BG_PANEL);
        previewPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(ACCENT_BLUE, 1),
                "待应用的变更",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("Microsoft YaHei UI", Font.BOLD, 12),
                ACCENT_BLUE));

        aiChangesPreview = new JTextArea();
        aiChangesPreview.setEditable(false);
        aiChangesPreview.setLineWrap(true);
        aiChangesPreview.setWrapStyleWord(true);
        aiChangesPreview.setBackground(new Color(240, 248, 255));
        aiChangesPreview.setForeground(TEXT_PRIMARY);
        aiChangesPreview.setFont(new Font("Consolas", Font.PLAIN, 12));
        aiChangesPreview.setBorder(new EmptyBorder(8, 8, 8, 8));
        aiChangesPreview.setText("(暂无变更)");

        JScrollPane previewScrollPane = new JScrollPane(aiChangesPreview);
        previewScrollPane.setPreferredSize(new Dimension(400, 100));
        previewScrollPane.setBorder(null);

        // 变更操作按钮
        JPanel changesBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        changesBtnPanel.setBackground(BG_PANEL);

        aiApplyBtn = new JButton("✓ 应用变更");
        aiApplyBtn.setBackground(ACCENT_GREEN);
        aiApplyBtn.setForeground(Color.WHITE);
        aiApplyBtn.setFocusPainted(false);
        aiApplyBtn.setBorderPainted(false);
        aiApplyBtn.setEnabled(false);
        aiApplyBtn.addActionListener(this);

        aiClearBtn = new JButton("✗ 清除");
        aiClearBtn.setBackground(ACCENT_ORANGE);
        aiClearBtn.setForeground(Color.WHITE);
        aiClearBtn.setFocusPainted(false);
        aiClearBtn.setBorderPainted(false);
        aiClearBtn.setEnabled(false);
        aiClearBtn.addActionListener(this);

        changesBtnPanel.add(aiClearBtn);
        changesBtnPanel.add(aiApplyBtn);

        previewPanel.add(previewScrollPane, BorderLayout.CENTER);
        previewPanel.add(changesBtnPanel, BorderLayout.SOUTH);

        // 输入区域
        JPanel inputPanel = new JPanel(new BorderLayout(8, 0));
        inputPanel.setBackground(BG_PANEL);
        inputPanel.setBorder(new EmptyBorder(10, 0, 0, 0));

        aiInputField = new JTextField();
        aiInputField.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 14));
        aiInputField.setBackground(BG_INPUT);
        aiInputField.setForeground(TEXT_PRIMARY);
        aiInputField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));

        // 添加回车发送功能
        aiInputField.addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent e) {
            }

            public void keyReleased(KeyEvent e) {
            }

            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && !aiProcessing) {
                    doAISend();
                }
            }
        });

        aiSendBtn = new JButton("发送");
        aiSendBtn.setBackground(ACCENT_PURPLE);
        aiSendBtn.setForeground(Color.WHITE);
        aiSendBtn.setFocusPainted(false);
        aiSendBtn.setBorderPainted(false);
        aiSendBtn.setPreferredSize(new Dimension(80, 40));
        aiSendBtn.addActionListener(this);

        inputPanel.add(aiInputField, BorderLayout.CENTER);
        inputPanel.add(aiSendBtn, BorderLayout.EAST);

        // 使用 JSplitPane 分割聊天区和预览区
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.add(chatScrollPane, BorderLayout.CENTER);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));
        centerPanel.setBackground(BG_PANEL);
        centerPanel.add(chatPanel, BorderLayout.CENTER);
        centerPanel.add(previewPanel, BorderLayout.SOUTH);

        // 组装面板
        panel.add(statusPanel, BorderLayout.NORTH);
        panel.add(centerPanel, BorderLayout.CENTER);
        panel.add(inputPanel, BorderLayout.SOUTH);

        return panel;
    }

    // AI 发送消息
    private void doAISend() {
        String userInput = aiInputField.getText().trim();
        if (userInput.isEmpty())
            return;

        if (!geminiClient.isConfigured()) {
            appendToChatHistory("系统", "请先设置环境变量 GEMINI_API_KEY 以使用 AI 助手功能。");
            return;
        }

        // 显示用户消息
        appendToChatHistory("你", userInput);
        aiInputField.setText("");
        aiInputField.setEnabled(false);
        aiSendBtn.setEnabled(false);
        aiProcessing = true;

        // 同步当前 UI 到 config
        syncUIToConfig();

        // 在后台线程调用 AI API
        SwingWorker<AIConfigParser.ParseResult, Void> worker = new SwingWorker<AIConfigParser.ParseResult, Void>() {
            @Override
            protected AIConfigParser.ParseResult doInBackground() throws Exception {
                String systemPrompt = AIConfigParser.generateSystemPrompt(config);
                String response = geminiClient.chat(userInput, systemPrompt);
                return AIConfigParser.parseAIResponse(response, config);
            }

            @Override
            protected void done() {
                try {
                    AIConfigParser.ParseResult result = get();
                    handleAIResponse(result);
                } catch (Exception e) {
                    appendToChatHistory("AI", "请求失败: " + e.getMessage());
                }
                aiInputField.setEnabled(true);
                aiSendBtn.setEnabled(true);
                aiProcessing = false;
                aiInputField.requestFocus();
            }
        };
        worker.execute();
    }

    // 处理 AI 响应
    private void handleAIResponse(AIConfigParser.ParseResult result) {
        // 显示 AI 回复
        appendToChatHistory("AI", result.message);

        if (result.success && result.changes != null && !result.changes.isEmpty()) {
            // 有配置变更，显示在预览区
            pendingChanges = result.changes;
            StringBuilder sb = new StringBuilder();
            for (AIConfigParser.ConfigChange change : result.changes) {
                sb.append(change.fieldLabel).append(" (").append(change.fieldName).append(")\n");
                sb.append("  ").append(change.oldValue).append(" → ").append(change.newValue).append("\n\n");
            }
            aiChangesPreview.setText(sb.toString());
            aiApplyBtn.setEnabled(true);
            aiClearBtn.setEnabled(true);
        } else if (result.error != null) {
            appendToChatHistory("系统", "解析错误: " + result.error);
        }
    }

    // 追加聊天记录
    private void appendToChatHistory(String sender, String message) {
        String prefix = "";
        if ("你".equals(sender)) {
            prefix = "👤 你: ";
        } else if ("AI".equals(sender)) {
            prefix = "🤖 AI: ";
        } else {
            prefix = "⚙️ " + sender + ": ";
        }
        aiChatHistory.append(prefix + message + "\n\n");
    }

    // 应用 AI 建议的变更
    private void doAIApplyChanges() {
        if (pendingChanges == null || pendingChanges.isEmpty())
            return;

        // 应用变更到 config
        AIConfigParser.applyChanges(config, pendingChanges);

        // 同步到 UI
        syncConfigToUI();

        // 清除待应用变更
        appendToChatHistory("系统", "已应用 " + pendingChanges.size() + " 项配置变更！");
        pendingChanges = null;
        aiChangesPreview.setText("(变更已应用)");
        aiApplyBtn.setEnabled(false);
        aiClearBtn.setEnabled(false);
    }

    // 清除待应用变更
    private void doAIClearChanges() {
        pendingChanges = null;
        aiChangesPreview.setText("(已清除)");
        aiApplyBtn.setEnabled(false);
        aiClearBtn.setEnabled(false);
        appendToChatHistory("系统", "已清除待应用的变更。");
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        panel.setBackground(BG_DARK);
        panel.setBorder(new EmptyBorder(10, 0, 0, 0));

        loadBtn = createButton("加载配置", ACCENT_BLUE);
        saveBtn = createButton("保存配置", ACCENT_BLUE);
        defaultBtn = createButton("恢复默认", ACCENT_ORANGE);
        runBtn = createButton("运行仿真", ACCENT_GREEN);
        runBtn.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 14));

        panel.add(loadBtn);
        panel.add(saveBtn);
        panel.add(defaultBtn);
        panel.add(Box.createHorizontalStrut(30));
        panel.add(runBtn);

        return panel;
    }

    // ActionListener implementation
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == loadBtn)
            doLoadConfig();
        else if (src == saveBtn)
            doSaveConfig();
        else if (src == defaultBtn)
            doResetDefault();
        else if (src == runBtn)
            doRunSimulation();
        else if (src == aiSendBtn)
            doAISend();
        else if (src == aiApplyBtn)
            doAIApplyChanges();
        else if (src == aiClearBtn)
            doAIClearChanges();
    }

    // UI Helpers
    private JPanel createBasePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BG_PANEL);
        panel.setBorder(new EmptyBorder(20, 25, 20, 25));
        return panel;
    }

    private GridBagConstraints createGBC() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        return gbc;
    }

    private JPanel wrapInScrollPane(JPanel panel) {
        JScrollPane scroll = new JScrollPane(panel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_PANEL);
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(scroll, BorderLayout.CENTER);
        return wrapper;
    }

    private void addSectionTitle(JPanel panel, GridBagConstraints gbc, int row, String title) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(row == 0 ? 0 : 15, 8, 8, 8);
        JLabel label = new JLabel("> " + title);
        label.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 14));
        label.setForeground(ACCENT_BLUE);
        panel.add(label, gbc);
        gbc.gridwidth = 1;
        gbc.insets = new Insets(6, 8, 6, 8);
    }

    private void addDoubleField(JPanel panel, GridBagConstraints gbc, int row, String label, String key, double value) {
        addLabel(panel, gbc, row, label);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JTextField field = createTextField(String.valueOf(value));
        inputFields.put(key, field);
        panel.add(field, gbc);
        gbc.weightx = 0;
    }

    private void addIntField(JPanel panel, GridBagConstraints gbc, int row, String label, String key, int value) {
        addLabel(panel, gbc, row, label);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JTextField field = createTextField(String.valueOf(value));
        inputFields.put(key, field);
        panel.add(field, gbc);
        gbc.weightx = 0;
    }

    private void addTextField(JPanel panel, GridBagConstraints gbc, int row, String label, String key, String value) {
        addLabel(panel, gbc, row, label);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JTextField field = createTextField(value);
        inputFields.put(key, field);
        panel.add(field, gbc);
        gbc.weightx = 0;
    }

    private void addComboField(JPanel panel, GridBagConstraints gbc, int row, String label, String key, String[] opts,
            String sel) {
        addLabel(panel, gbc, row, label);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JComboBox<String> combo = new JComboBox<String>(opts);
        combo.setSelectedItem(sel);
        combo.setBackground(BG_INPUT);
        combo.setForeground(TEXT_PRIMARY);
        inputFields.put(key, combo);
        panel.add(combo, gbc);
        gbc.weightx = 0;
    }

    private void addCheckBox(JPanel panel, GridBagConstraints gbc, int row, String label, String key, boolean value) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        JCheckBox cb = new JCheckBox(label, value);
        cb.setBackground(BG_PANEL);
        cb.setForeground(TEXT_PRIMARY);
        cb.setFocusPainted(false);
        inputFields.put(key, cb);
        panel.add(cb, gbc);
        gbc.gridwidth = 1;
    }

    private void addLabel(JPanel panel, GridBagConstraints gbc, int row, String text) {
        gbc.gridx = 0;
        gbc.gridy = row;
        JLabel label = new JLabel(text);
        label.setForeground(TEXT_PRIMARY);
        panel.add(label, gbc);
    }

    private void addNote(JPanel panel, GridBagConstraints gbc, int row, String text) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        JLabel label = new JLabel("* " + text);
        label.setFont(new Font("Microsoft YaHei UI", Font.ITALIC, 11));
        label.setForeground(TEXT_SECONDARY);
        panel.add(label, gbc);
        gbc.gridwidth = 1;
    }

    private void addFiller(JPanel panel, GridBagConstraints gbc, int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weighty = 1.0;
        panel.add(Box.createVerticalGlue(), gbc);
        gbc.weighty = 0;
    }

    private JTextField createTextField(String value) {
        JTextField field = new JTextField(value, 15);
        field.setBackground(BG_INPUT);
        field.setForeground(TEXT_PRIMARY);
        field.setCaretColor(TEXT_PRIMARY);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        return field;
    }

    private JButton createButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(10, 20, 10, 20));
        button.addActionListener(this);
        return button;
    }

    // Config sync
    private void syncUIToConfig() {
        config.domainWidth = getDouble("domainWidth");
        config.domainHeight = getDouble("domainHeight");
        config.cylinderRadius = getDouble("cylinderRadius");
        config.cylinderX = getDouble("cylinderX");
        config.cylinderY = getDouble("cylinderY");
        config.inletType = getString("inletType");
        config.inletVelocity = getDouble("inletVelocity");
        config.inletPressure = getDouble("inletPressure");
        config.outletType = getString("outletType");
        config.outletPressure = getDouble("outletPressure");
        config.outletVelocity = getDouble("outletVelocity");
        config.topBoundaryType = getString("topBoundaryType");
        config.bottomBoundaryType = getString("bottomBoundaryType");
        config.cylinderWallType = getString("cylinderWallType");
        config.cylinderWallCondition = getString("cylinderWallCondition");
        config.flowType = getString("flowType");
        config.equationForm = getString("equationForm");
        config.fluidName = getString("fluidName");
        config.density = getDouble("density");
        config.dynamicViscosity = getDouble("dynamicViscosity");
        config.meshSizeLevel = getInt("meshSizeLevel");
        config.meshMaxSize = getDouble("meshMaxSize");
        config.meshMinSize = getDouble("meshMinSize");
        config.cylinderMeshMaxSize = getDouble("cylinderMeshMaxSize");
        config.startTime = getDouble("startTime");
        config.endTime = getDouble("endTime");
        config.timeStep = getDouble("timeStep");
        config.outputDir = getString("outputDir");
        config.modelFileName = getString("modelFileName");
        config.exportVelocity = getBool("exportVelocity");
        config.exportVorticity = getBool("exportVorticity");
        config.exportAnimation = getBool("exportAnimation");
        config.animationFps = getInt("animationFps");
        config.animationMaxFrames = getInt("animationMaxFrames");
    }

    private void syncConfigToUI() {
        setDouble("domainWidth", config.domainWidth);
        setDouble("domainHeight", config.domainHeight);
        setDouble("cylinderRadius", config.cylinderRadius);
        setDouble("cylinderX", config.cylinderX);
        setDouble("cylinderY", config.cylinderY);
        setString("inletType", config.inletType);
        setDouble("inletVelocity", config.inletVelocity);
        setDouble("inletPressure", config.inletPressure);
        setString("outletType", config.outletType);
        setDouble("outletPressure", config.outletPressure);
        setDouble("outletVelocity", config.outletVelocity);
        setString("topBoundaryType", config.topBoundaryType);
        setString("bottomBoundaryType", config.bottomBoundaryType);
        setString("cylinderWallType", config.cylinderWallType);
        setString("cylinderWallCondition", config.cylinderWallCondition);
        setString("flowType", config.flowType);
        setString("equationForm", config.equationForm);
        setString("fluidName", config.fluidName);
        setDouble("density", config.density);
        setDouble("dynamicViscosity", config.dynamicViscosity);
        setInt("meshSizeLevel", config.meshSizeLevel);
        setDouble("meshMaxSize", config.meshMaxSize);
        setDouble("meshMinSize", config.meshMinSize);
        setDouble("cylinderMeshMaxSize", config.cylinderMeshMaxSize);
        setDouble("startTime", config.startTime);
        setDouble("endTime", config.endTime);
        setDouble("timeStep", config.timeStep);
        setString("outputDir", config.outputDir);
        setString("modelFileName", config.modelFileName);
        setBool("exportVelocity", config.exportVelocity);
        setBool("exportVorticity", config.exportVorticity);
        setBool("exportAnimation", config.exportAnimation);
        setInt("animationFps", config.animationFps);
        setInt("animationMaxFrames", config.animationMaxFrames);
    }

    private double getDouble(String key) {
        Object c = inputFields.get(key);
        if (c instanceof JTextField) {
            try {
                return Double.parseDouble(((JTextField) c).getText().trim());
            } catch (Exception e) {
                return 0;
            }
        }
        return 0;
    }

    private int getInt(String key) {
        Object c = inputFields.get(key);
        if (c instanceof JTextField) {
            try {
                return Integer.parseInt(((JTextField) c).getText().trim());
            } catch (Exception e) {
                return 0;
            }
        }
        return 0;
    }

    private String getString(String key) {
        Object c = inputFields.get(key);
        if (c instanceof JTextField)
            return ((JTextField) c).getText().trim();
        if (c instanceof JComboBox)
            return (String) ((JComboBox) c).getSelectedItem();
        return "";
    }

    private boolean getBool(String key) {
        Object c = inputFields.get(key);
        if (c instanceof JCheckBox)
            return ((JCheckBox) c).isSelected();
        return false;
    }

    private void setDouble(String key, double v) {
        Object c = inputFields.get(key);
        if (c instanceof JTextField)
            ((JTextField) c).setText(String.valueOf(v));
    }

    private void setInt(String key, int v) {
        Object c = inputFields.get(key);
        if (c instanceof JTextField)
            ((JTextField) c).setText(String.valueOf(v));
    }

    private void setString(String key, String v) {
        Object c = inputFields.get(key);
        if (c instanceof JTextField)
            ((JTextField) c).setText(v);
        if (c instanceof JComboBox)
            ((JComboBox) c).setSelectedItem(v);
    }

    private void setBool(String key, boolean v) {
        Object c = inputFields.get(key);
        if (c instanceof JCheckBox)
            ((JCheckBox) c).setSelected(v);
    }

    // Actions
    private void doLoadConfig() {
        JFileChooser chooser = new JFileChooser(System.getProperty("user.dir"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                config = ConfigManager.loadConfig(chooser.getSelectedFile().getAbsolutePath());
                syncConfigToUI();
                msg("配置已加载", "成功: " + chooser.getSelectedFile().getName());
            } catch (IOException e) {
                err("加载失败: " + e.getMessage());
            }
        }
    }

    private void doSaveConfig() {
        try {
            syncUIToConfig();
            ConfigManager.saveConfig(config, CONFIG_FILE);
            msg("保存成功", "配置已保存到: " + CONFIG_FILE);
        } catch (Exception e) {
            err("保存失败: " + e.getMessage());
        }
    }

    private void doResetDefault() {
        int r = JOptionPane.showConfirmDialog(this, "确定恢复默认值?", "恢复默认",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r == JOptionPane.YES_OPTION) {
            config = SimulationConfig.getDefault();
            syncConfigToUI();
            msg("已恢复默认", "所有参数已恢复");
        }
    }

    private void doRunSimulation() {
        try {
            syncUIToConfig();
            ConfigManager.saveConfig(config, CONFIG_FILE);

            int r = JOptionPane.showConfirmDialog(this,
                    "配置已保存。\n时间: " + config.startTime + "s -> " + config.endTime + "s\n入口速度: " + config.inletVelocity
                            + " m/s\n\n确定启动?",
                    "运行仿真", JOptionPane.YES_NO_OPTION);

            if (r == JOptionPane.YES_OPTION) {
                File bat = new File("run_comsol.bat");
                if (bat.exists()) {
                    Runtime.getRuntime().exec("cmd /c start \"COMSOL\" \"" + bat.getAbsolutePath() + "\"");
                    msg("仿真已启动", "请查看命令行窗口");
                } else {
                    err("未找到 run_comsol.bat");
                }
            }
        } catch (Exception e) {
            err("启动失败: " + e.getMessage());
        }
    }

    private void msg(String t, String m) {
        JOptionPane.showMessageDialog(this, m, t, JOptionPane.INFORMATION_MESSAGE);
    }

    private void err(String m) {
        JOptionPane.showMessageDialog(this, m, "错误", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
        }
        CylinderFlowGUI gui = new CylinderFlowGUI();
        gui.setVisible(true);
    }
}

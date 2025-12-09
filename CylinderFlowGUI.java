/*
 * CylinderFlowGUI.java - COMSOL 仿真配置 GUI
 * 避免匿名内部类，兼容 COMSOL 编译器
 */

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
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
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

public class CylinderFlowGUI extends JFrame implements ActionListener {

    private SimulationConfig config;
    private static final String CONFIG_FILE = "config.json";
    private Map<String, Object> inputFields = new HashMap<String, Object>();

    // Buttons as fields for action handling
    private JButton loadBtn, saveBtn, defaultBtn, runBtn;

    // Colors - Light Theme for Better Readability
    private static final Color BG_DARK = new Color(245, 245, 250);
    private static final Color BG_PANEL = new Color(255, 255, 255);
    private static final Color BG_INPUT = new Color(250, 250, 252);
    private static final Color TEXT_PRIMARY = new Color(30, 30, 40);
    private static final Color TEXT_SECONDARY = new Color(100, 100, 120);
    private static final Color ACCENT_BLUE = new Color(25, 118, 210);
    private static final Color ACCENT_GREEN = new Color(46, 125, 50);
    private static final Color ACCENT_ORANGE = new Color(230, 81, 0);
    private static final Color BORDER_COLOR = new Color(200, 200, 210);

    public CylinderFlowGUI() {
        loadConfig();
        initUI();
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

// ============================================
// SimulationConfig - 配置数据类
// ============================================
class SimulationConfig {
    public double domainWidth = 2.2;
    public double domainHeight = 1.0;
    public double cylinderRadius = 0.05;
    public double cylinderX = 0.5;
    public double cylinderY = 0.5;
    public String inletType = "Velocity";
    public double inletVelocity = 0.031;
    public double inletPressure = 0.0;
    public String outletType = "Pressure";
    public double outletPressure = 0.0;
    public double outletVelocity = 0.0;
    public String topBoundaryType = "Symmetry";
    public String bottomBoundaryType = "Symmetry";
    public String cylinderWallType = "Wall";
    public String cylinderWallCondition = "NoSlip";
    public String flowType = "Laminar";
    public String equationForm = "Transient";
    public String fluidName = "Air";
    public double density = 1.225;
    public double dynamicViscosity = 1.7894e-5;
    public int meshSizeLevel = 3;
    public double meshMaxSize = 0.01;
    public double meshMinSize = 0.0005;
    public double cylinderMeshMaxSize = 0.002;
    public double startTime = 0.0;
    public double endTime = 200.0;
    public double timeStep = 0.5;
    public String outputDir = "";
    public String modelFileName = "CylinderFlow.mph";
    public boolean exportVelocity = true;
    public boolean exportVorticity = true;
    public boolean exportAnimation = true;
    public int animationFps = 60;
    public int animationMaxFrames = 200;

    public static SimulationConfig getDefault() {
        return new SimulationConfig();
    }

    public String getTimeListString() {
        return "range(" + startTime + "," + timeStep + "," + endTime + ")";
    }

    public String getEffectiveOutputDir() {
        if (outputDir == null || outputDir.isEmpty())
            return System.getProperty("user.dir");
        return outputDir;
    }
}

// ============================================
// ConfigManager - JSON 配置管理
// ============================================
class ConfigManager {
    public static void saveConfig(SimulationConfig cfg, String path) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"domainWidth\": ").append(cfg.domainWidth).append(",\n");
        sb.append("  \"domainHeight\": ").append(cfg.domainHeight).append(",\n");
        sb.append("  \"cylinderRadius\": ").append(cfg.cylinderRadius).append(",\n");
        sb.append("  \"cylinderX\": ").append(cfg.cylinderX).append(",\n");
        sb.append("  \"cylinderY\": ").append(cfg.cylinderY).append(",\n");
        sb.append("  \"inletType\": \"").append(cfg.inletType).append("\",\n");
        sb.append("  \"inletVelocity\": ").append(cfg.inletVelocity).append(",\n");
        sb.append("  \"inletPressure\": ").append(cfg.inletPressure).append(",\n");
        sb.append("  \"outletType\": \"").append(cfg.outletType).append("\",\n");
        sb.append("  \"outletPressure\": ").append(cfg.outletPressure).append(",\n");
        sb.append("  \"outletVelocity\": ").append(cfg.outletVelocity).append(",\n");
        sb.append("  \"topBoundaryType\": \"").append(cfg.topBoundaryType).append("\",\n");
        sb.append("  \"bottomBoundaryType\": \"").append(cfg.bottomBoundaryType).append("\",\n");
        sb.append("  \"cylinderWallType\": \"").append(cfg.cylinderWallType).append("\",\n");
        sb.append("  \"cylinderWallCondition\": \"").append(cfg.cylinderWallCondition).append("\",\n");
        sb.append("  \"flowType\": \"").append(cfg.flowType).append("\",\n");
        sb.append("  \"equationForm\": \"").append(cfg.equationForm).append("\",\n");
        sb.append("  \"fluidName\": \"").append(cfg.fluidName).append("\",\n");
        sb.append("  \"density\": ").append(cfg.density).append(",\n");
        sb.append("  \"dynamicViscosity\": ").append(cfg.dynamicViscosity).append(",\n");
        sb.append("  \"meshSizeLevel\": ").append(cfg.meshSizeLevel).append(",\n");
        sb.append("  \"meshMaxSize\": ").append(cfg.meshMaxSize).append(",\n");
        sb.append("  \"meshMinSize\": ").append(cfg.meshMinSize).append(",\n");
        sb.append("  \"cylinderMeshMaxSize\": ").append(cfg.cylinderMeshMaxSize).append(",\n");
        sb.append("  \"startTime\": ").append(cfg.startTime).append(",\n");
        sb.append("  \"endTime\": ").append(cfg.endTime).append(",\n");
        sb.append("  \"timeStep\": ").append(cfg.timeStep).append(",\n");
        sb.append("  \"outputDir\": \"").append(cfg.outputDir).append("\",\n");
        sb.append("  \"modelFileName\": \"").append(cfg.modelFileName).append("\",\n");
        sb.append("  \"exportVelocity\": ").append(cfg.exportVelocity).append(",\n");
        sb.append("  \"exportVorticity\": ").append(cfg.exportVorticity).append(",\n");
        sb.append("  \"exportAnimation\": ").append(cfg.exportAnimation).append(",\n");
        sb.append("  \"animationFps\": ").append(cfg.animationFps).append(",\n");
        sb.append("  \"animationMaxFrames\": ").append(cfg.animationMaxFrames).append("\n");
        sb.append("}");

        java.nio.file.Files.write(java.nio.file.Paths.get(path), sb.toString().getBytes("UTF-8"));
        System.out.println("Config saved: " + path);
    }

    public static SimulationConfig loadConfig(String path) throws IOException {
        SimulationConfig cfg = new SimulationConfig();
        if (!java.nio.file.Files.exists(java.nio.file.Paths.get(path)))
            return cfg;

        String c = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(path)), "UTF-8");

        cfg.domainWidth = parseDouble(c, "domainWidth", cfg.domainWidth);
        cfg.domainHeight = parseDouble(c, "domainHeight", cfg.domainHeight);
        cfg.cylinderRadius = parseDouble(c, "cylinderRadius", cfg.cylinderRadius);
        cfg.cylinderX = parseDouble(c, "cylinderX", cfg.cylinderX);
        cfg.cylinderY = parseDouble(c, "cylinderY", cfg.cylinderY);
        cfg.inletType = parseString(c, "inletType", cfg.inletType);
        cfg.inletVelocity = parseDouble(c, "inletVelocity", cfg.inletVelocity);
        cfg.inletPressure = parseDouble(c, "inletPressure", cfg.inletPressure);
        cfg.outletType = parseString(c, "outletType", cfg.outletType);
        cfg.outletPressure = parseDouble(c, "outletPressure", cfg.outletPressure);
        cfg.outletVelocity = parseDouble(c, "outletVelocity", cfg.outletVelocity);
        cfg.topBoundaryType = parseString(c, "topBoundaryType", cfg.topBoundaryType);
        cfg.bottomBoundaryType = parseString(c, "bottomBoundaryType", cfg.bottomBoundaryType);
        cfg.cylinderWallType = parseString(c, "cylinderWallType", cfg.cylinderWallType);
        cfg.cylinderWallCondition = parseString(c, "cylinderWallCondition", cfg.cylinderWallCondition);
        cfg.flowType = parseString(c, "flowType", cfg.flowType);
        cfg.equationForm = parseString(c, "equationForm", cfg.equationForm);
        cfg.fluidName = parseString(c, "fluidName", cfg.fluidName);
        cfg.density = parseDouble(c, "density", cfg.density);
        cfg.dynamicViscosity = parseDouble(c, "dynamicViscosity", cfg.dynamicViscosity);
        cfg.meshSizeLevel = parseInt(c, "meshSizeLevel", cfg.meshSizeLevel);
        cfg.meshMaxSize = parseDouble(c, "meshMaxSize", cfg.meshMaxSize);
        cfg.meshMinSize = parseDouble(c, "meshMinSize", cfg.meshMinSize);
        cfg.cylinderMeshMaxSize = parseDouble(c, "cylinderMeshMaxSize", cfg.cylinderMeshMaxSize);
        cfg.startTime = parseDouble(c, "startTime", cfg.startTime);
        cfg.endTime = parseDouble(c, "endTime", cfg.endTime);
        cfg.timeStep = parseDouble(c, "timeStep", cfg.timeStep);
        cfg.outputDir = parseString(c, "outputDir", cfg.outputDir);
        cfg.modelFileName = parseString(c, "modelFileName", cfg.modelFileName);
        cfg.exportVelocity = parseBoolean(c, "exportVelocity", cfg.exportVelocity);
        cfg.exportVorticity = parseBoolean(c, "exportVorticity", cfg.exportVorticity);
        cfg.exportAnimation = parseBoolean(c, "exportAnimation", cfg.exportAnimation);
        cfg.animationFps = parseInt(c, "animationFps", cfg.animationFps);
        cfg.animationMaxFrames = parseInt(c, "animationMaxFrames", cfg.animationMaxFrames);

        System.out.println("Config loaded: " + path);
        return cfg;
    }

    private static double parseDouble(String json, String key, double defaultVal) {
        try {
            int idx = json.indexOf("\"" + key + "\":");
            if (idx < 0)
                return defaultVal;
            int start = idx + key.length() + 3;
            while (start < json.length() && Character.isWhitespace(json.charAt(start)))
                start++;
            int end = start;
            while (end < json.length()
                    && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.' || json.charAt(end) == '-'
                            || json.charAt(end) == 'e' || json.charAt(end) == 'E' || json.charAt(end) == '+'))
                end++;
            return Double.parseDouble(json.substring(start, end).trim());
        } catch (Exception e) {
            return defaultVal;
        }
    }

    private static int parseInt(String json, String key, int defaultVal) {
        try {
            return (int) parseDouble(json, key, defaultVal);
        } catch (Exception e) {
            return defaultVal;
        }
    }

    private static String parseString(String json, String key, String defaultVal) {
        try {
            int idx = json.indexOf("\"" + key + "\":");
            if (idx < 0)
                return defaultVal;
            int start = json.indexOf('"', idx + key.length() + 3) + 1;
            int end = json.indexOf('"', start);
            return json.substring(start, end);
        } catch (Exception e) {
            return defaultVal;
        }
    }

    private static boolean parseBoolean(String json, String key, boolean defaultVal) {
        try {
            int idx = json.indexOf("\"" + key + "\":");
            if (idx < 0)
                return defaultVal;
            int start = idx + key.length() + 3;
            while (start < json.length() && Character.isWhitespace(json.charAt(start)))
                start++;
            return json.substring(start, start + 4).equals("true");
        } catch (Exception e) {
            return defaultVal;
        }
    }
}

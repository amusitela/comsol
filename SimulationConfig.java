/*
 * SimulationConfig.java - COMSOL 仿真配置数据类
 * 存储所有可配置的仿真参数
 */

public class SimulationConfig {

    // ============================================
    // 1. 几何参数 (Geometry)
    // ============================================
    public double domainWidth = 2.2; // 域宽度 (m)
    public double domainHeight = 1.0; // 域高度 (m)
    public double cylinderRadius = 0.05; // 圆柱半径 (m)
    public double cylinderX = 0.5; // 圆柱 X 坐标 (m)
    public double cylinderY = 0.5; // 圆柱 Y 坐标 (m)

    // ============================================
    // 2. 边界条件参数 (Boundary Conditions)
    // ============================================
    // 入口边界
    public String inletType = "Velocity"; // Velocity / Pressure
    public double inletVelocity = 0.031; // 入口速度 (m/s)
    public double inletPressure = 0.0; // 入口压力 (Pa)

    // 出口边界
    public String outletType = "Pressure"; // Pressure / Velocity / Outflow
    public double outletPressure = 0.0; // 出口压力 (Pa)
    public double outletVelocity = 0.0; // 出口速度 (m/s)

    // 上边界
    public String topBoundaryType = "Symmetry"; // Symmetry / Wall / Slip

    // 下边界
    public String bottomBoundaryType = "Symmetry"; // Symmetry / Wall / Slip

    // 圆柱壁面
    public String cylinderWallType = "Wall"; // Wall / Slip
    public String cylinderWallCondition = "NoSlip"; // NoSlip / Slip

    // ============================================
    // 3. 物理参数 (Physics)
    // ============================================
    public String flowType = "Laminar"; // Laminar / Turbulent
    public String equationForm = "Transient"; // Transient / Stationary

    // ============================================
    // 4. 材料参数 (Material)
    // ============================================
    public String fluidName = "Air";
    public double density = 1.225; // 密度 (kg/m³)
    public double dynamicViscosity = 1.7894e-5; // 动力粘度 (Pa·s)

    // ============================================
    // 5. 网格参数 (Mesh)
    // ============================================
    public int meshSizeLevel = 3; // 网格精度等级 (1-9)
    public double meshMaxSize = 0.01; // 全局最大单元 (m)
    public double meshMinSize = 0.0005; // 全局最小单元 (m)
    public double cylinderMeshMaxSize = 0.002; // 圆柱加密最大单元 (m)

    // ============================================
    // 6. 求解参数 (Solver)
    // ============================================
    public double startTime = 0.0; // 开始时间 (s)
    public double endTime = 200.0; // 结束时间 (s)
    public double timeStep = 0.5; // 时间步长 (s)

    // ============================================
    // 7. 输出参数 (Output)
    // ============================================
    public String outputDir = ""; // 工作目录 (空 = 当前目录)
    public String modelFileName = "CylinderFlow.mph";
    public boolean exportVelocity = true;
    public boolean exportVorticity = true;
    public boolean exportPressure = true;
    public boolean exportAnimation = true;
    public int animationFps = 60;
    public int animationMaxFrames = 200;

    // ============================================
    // 8. 压力云图颜色范围 (Pressure Color Range)
    // ============================================
    // 手动锁定压力颜色范围，解决动画和PNG颜色不一致问题
    // 设置为true启用手动范围，避免初始压力冲击导致动画"死灰"
    public boolean pressureRangeManual = true;
    public double pressureRangeMin = -2.0; // 压力最小值 (Pa)
    public double pressureRangeMax = 2.0; // 压力最大值 (Pa)

    // ============================================
    // 工厂方法
    // ============================================

    /**
     * 获取默认配置
     */
    public static SimulationConfig getDefault() {
        return new SimulationConfig();
    }

    /**
     * 生成时间列表字符串 (用于 COMSOL)
     */
    public String getTimeListString() {
        return String.format("range(%s,%s,%s)", startTime, timeStep, endTime);
    }

    /**
     * 获取有效的输出目录
     */
    public String getEffectiveOutputDir() {
        if (outputDir == null || outputDir.isEmpty()) {
            return System.getProperty("user.dir");
        }
        return outputDir;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SimulationConfig {\n");
        sb.append("  Geometry: domain=").append(domainWidth).append("x").append(domainHeight);
        sb.append(", cylinder: r=").append(cylinderRadius).append(" at (").append(cylinderX).append(",")
                .append(cylinderY).append(")\n");
        sb.append("  Inlet: ").append(inletType).append(", v=").append(inletVelocity).append("\n");
        sb.append("  Outlet: ").append(outletType).append(", p=").append(outletPressure).append("\n");
        sb.append("  Physics: ").append(flowType).append(", ").append(equationForm).append("\n");
        sb.append("  Solver: t=").append(startTime).append(" to ").append(endTime).append(", dt=").append(timeStep)
                .append("\n");
        sb.append("}");
        return sb.toString();
    }
}

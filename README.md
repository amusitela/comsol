# COMSOL 圆柱绕流仿真 (卡门涡街) - Java API 详解

本项目使用 **COMSOL Multiphysics 6.0 Java API** 自动化构建并计算一个经典的 **2D 层流圆柱绕流** 模型，模拟 **卡门涡街 (Karman Vortex Street)** 现象。

本文档将详细解释 `CylinderFlow.java` 代码中的每一个步骤、使用的 API 方法以及其背后的物理意义。

## 1. 模型初始化 (Model Initialization)

代码首先创建一个新的模型容器。

```java
Model model = ModelUtil.create("Model");
model.label("Cylinder Flow - Karman Vortex Street");
```

- `ModelUtil.create("Model")`: 这是 COMSOL API 的入口。创建一个唯一的模型对象，相当于在 GUI 中点击 "新建"。
- `model.label(...)`: 设置模型在 COMSOL 界面中显示的名称。

## 2. 几何建模 (Geometry)

我们创建一各 2.2m x 1.0m 的矩形计算域，并在位置 (0.5, 0.5) 处挖去一个半径为 0.05m 的圆柱。

```java
model.component().create("comp1", true);
model.component("comp1").geom().create("geom1", 2);
```

- `component().create("comp1", true)`: 创建一个组件。`true` 表示这是一个 "Model Node"（这也是 GUI 的默认行为）。
- `geom().create("geom1", 2)`: 在组件中创建一个 **2D** 几何序列。

### 2.1 创建矩形 (计算域)

```java
model.component("comp1").geom("geom1").create("r1", "Rectangle");
model.component("comp1").geom("geom1").feature("r1").set("size", new double[] { 2.2, 1.0 });
model.component("comp1").geom("geom1").feature("r1").set("pos", new double[] { 0, 0 });
```

- `create("r1", "Rectangle")`: 创建一个矩形特征。
- `set("size", ...)`: 设置矩形的宽和高。
- `set("pos", ...)`: 设置矩形左下角的坐标 (0,0)。

### 2.2 创建圆形 (圆柱障碍物)

```java
model.component("comp1").geom("geom1").create("c1", "Circle");
...
```

- 这与矩形类似，使用 `Circle` 类型，设置半径 `r` 和圆心位置 `pos`。

### 2.3 布尔操作 (差集)

```java
model.component("comp1").geom("geom1").create("dif1", "Difference");
model.component("comp1").geom("geom1").feature("dif1").selection("input").set("r1");
model.component("comp1").geom("geom1").feature("dif1").selection("input2").set("c1");
```

- `create("dif1", "Difference")`: 创建布尔差集操作。
- `selection("input")`: 被减对象（矩形 `r1`）。
- `selection("input2")`: 减去的对象（圆形 `c1`）。
- **结果**: 得到一个带有圆孔的矩形平面。

### 2.4 命名的选择集 (Named Selections)

这是为了方便后续物理场设置边界条件。我们使用 **BoxSelection** (框选) 和 **BallSelection** (球选) 来智能选取边界。

```java
model.component("comp1").geom("geom1").create("inlet_sel", "BoxSelection");
...
model.component("comp1").geom("geom1").feature("inlet_sel").set("condition", "inside");
```

- `BoxSelection`: 定义一个矩形框。
- `set("entitydim", 1)`: 指定我们要选的是 **1 维实体**（即 2D 中的**边**）。
- `set("condition", "inside")`: **关键细节**。选择完全位于框**内部**的几何实体。这比 "intersects" (相交) 更准确，能避免误选相邻的边。
- **定义的四个选择集**:

  - `inlet_sel`: 入口左边界。
  - `outlet_sel`: 出口右边界。
  - `top_sel` / `bottom_sel`: 上下对称边界。
  - `cyl_sel`: 圆柱壁面（使用 `BallSelection` 选取圆形中心附近的边界）。

- `model.component("comp1").geom("geom1").run()`: **执行几何序列**。这相当于点击 GUI 中的 "构建所有"。

## 3. 材料 (Materials)

定义流体属性（空气）。

```java
model.component("comp1").material().create("mat1", "Common");
...
model.component("comp1").material("mat1").propertyGroup("def").set("density", "1.225[kg/m^3]");
model.component("comp1").material("mat1").propertyGroup("def").set("dynamicviscosity", "1.7894e-5[Pa*s]");
```

- `mat1`: 创建一个材料。
- `selection().all()`: 该材料适用于所有域。
- `propertyGroup("def").set(...)`: 手动设置密度和动力粘度。这里使用的是标准空气参数。

## 4. 物理场 (Physics)

设置 **层流 (Laminar Flow)** 物理场接口。

```java
model.component("comp1").physics().create("spf", "LaminarFlow", "geom1");
model.component("comp1").physics("spf").prop("EquationForm").set("form", "Transient");
```

- `create("spf", "LaminarFlow", "geom1")`: 在 `geom1` 上创建层流接口，标识符为 `spf`。
- `set("form", "Transient")`: **关键细节**。将方程形式设置为 **瞬态 (Transient)**，因为我们要模拟随时间变化的涡街脱落，而不是稳态结果。

### 边界条件

- **Inlet (入口)**:

  ```java
  model.component("comp1").physics("spf").create("inl1", "InletBoundary", 1);
  model.component("comp1").physics("spf").feature("inl1").selection().named("geom1_inlet_sel");
  model.component("comp1").physics("spf").feature("inl1").set("U0in", "0.031[m/s]");
  ```

  - 使用之前定义的 `geom1_inlet_sel` 选择集。
  - 设置入口速度 `U0in` 为 0.031 m/s。

- **Outlet (出口)**: 压力边界条件，相对压力为 0。
- **Symmetry (对称)**: 上下壁面设为对称边界，模拟无限大空间或滑移壁面。
- **Wall (壁面)**: 圆柱表面设为无滑移壁面 (`Wall`)。

## 5. 网格 (Mesh)

为了捕捉细微的涡旋，网格质量至关重要。

```java
model.component("comp1").mesh().create("mesh1");
model.component("comp1").mesh("mesh1").feature("size").set("hauto", 3); // 预设 "Fine"
...
model.component("comp1").mesh("mesh1").feature("size").set("hmax", "0.01"); // 全局最大单元
```

### 局部加密

我们在圆柱周围进行了局部加密：

```java
model.component("comp1").mesh("mesh1").create("size2", "Size");
model.component("comp1").mesh("mesh1").feature("size2").selection().named("geom1_cyl_sel");
model.component("comp1").mesh("mesh1").feature("size2").set("hmax", "0.002");
```

- `selection().named("geom1_cyl_sel")`: 仅针对圆柱边界。
- `hmax` = 0.002: 圆柱表面的网格非常细，是全局网格的 1/5，这对准确计算边界层和分离点非常重要。

## 6. 研究与求解 (Study)

设置瞬态求解器。

```java
model.study().create("std1");
model.study("std1").create("time", "Transient");
model.study("std1").feature("time").set("tlist", "range(0,0.5,200)");
```

- `create("time", "Transient")`: 创建瞬态研究步骤。
- `set("tlist", "range(0,0.5,200)")`: 设置时间步长。
  - 开始时间: 0s
  - 步长: 0.5s
  - 结束时间: 200s
- `model.study("std1").run()`: **开始计算**。这会触发求解器，可能需要几分钟。

## 7. 结果处理与可视化 (Results & Visualization)

### 7.1 创建绘图组 (Plot Groups)

我们创建了两个 2D 绘图组：

1.  **Velocity Magnitude** (速度模)
2.  **Vorticity** (涡量 - Z 分量) - 涡量图最能清晰地展示涡街结构。

```java
model.result().create("pg1", "PlotGroup2D");
model.result("pg1").create("surf1", "Surface");
model.result("pg1").feature("surf1").set("expr", "spf.U"); // 表达式: 速度模
model.result("pg1").feature("surf1").set("colortable", "RainbowLight"); // 颜色表
```

### 7.2 关键修复：关联数据集

为了防止导出的图片为空白：

```java
model.result("pg1").set("data", "dset1");
```

- 求解完成后，COMSOL 会自动生成数据集 `dset1`。我们需要显式地告诉绘图组使用这个数据集。

### 7.3 导出图片

```java
model.result().export().create("img1", "Image");
model.result().export("img1").set("sourceobject", "pg1"); // 来源: 速度绘图组
model.result().export("img1").set("filename", ...);
model.result().export("img1").set("showlegends", true); // 显示图例
model.result().export("img1").run(); // 执行导出
```

### 7.4 导出动画 (Animation)

这是最复杂的部分，很容易出错。

```java
model.result().export().create("anim1", "Animation");
model.result().export("anim1").set("target", "file"); // 目标：文件
model.result().export("anim1").set("type", "movie");  // 类型：电影
model.result().export("anim1").set("format", "gif");  // 格式：GIF
model.result().export("anim1").set("plotgroup", "pg2"); // 来源：涡量图
model.result().export("anim1").set("giffilename", ...); // 注意：属性名是 giffilename 不是 filename
model.result().export("anim1").set("frames", "all");  // 导出所有时间步
```

- **注意**: 导出 GIF 时，文件名属性必须使用 `giffilename`。

---

## 如何运行

1.  确保电脑已安装 COMSOL 6.0。
2.  双击运行 `run_gui.bat`。
3.  程序将自动编译 Java 代码，调用 COMSOL 内核进行计算，并最终在当前目录生成结果文件。

---

## AI 配置助手

本项目集成了 **AI 自然语言配置功能**，支持 **通义千问 (Qwen)** 和 **DeepSeek** 两个 AI 服务商，让你可以用自然语言描述来修改仿真参数。

### 获取 API Key

**方式一：阿里通义千问 (推荐)**

1. 访问 [阿里云 DashScope](https://dashscope.console.aliyun.com/)
2. 注册/登录阿里云账号
3. 开通 DashScope 服务（有免费额度）
4. 创建 API Key

**方式二：DeepSeek (备用)**

1. 访问 [DeepSeek 开放平台](https://platform.deepseek.com/)
2. 注册账号
3. 获取 API Key（有免费额度）

### 配置 API Key

在项目根目录创建 `.env` 文件：

```env
# 阿里通义千问 (主要)
QWEN_API_KEY=你的阿里API_Key

# DeepSeek (备用，可选)
DEEPSEEK_API_KEY=你的DeepSeek_API_Key
```

> 配置一个即可使用，建议两个都配置以实现自动降级。

### 启动 GUI

```powershell
# 编译
javac -encoding UTF-8 SimulationConfig.java ConfigManager.java QwenClient.java AIConfigParser.java CylinderFlowGUI.java

# 运行
java CylinderFlowGUI
```

或直接运行 `run_gui.bat`（需要 COMSOL 环境）。

### 使用示例

```
你: 把入口速度改成 0.05 m/s，使用水作为流体

AI: 好的，我将为您进行以下修改：
    - 入口速度 (inletVelocity): 0.031 -> 0.05
    - 流体名称 (fluidName): Air -> Water
    - 密度 (density): 1.225 -> 998.0
    - 动力粘度 (dynamicViscosity): 1.7894E-5 -> 1.002E-3

点击 [应用变更] 按钮确认
```

### 支持的自然语言命令

- **修改几何参数**: "把圆柱半径改成 0.03 米" / "域宽度设为 3 米"
- **设置边界条件**: "入口改成压力边界" / "上边界改成壁面" / "圆柱壁面使用滑移条件"
- **切换流体**: "使用水作为流体" / "换成空气"
- **调整速度/压力**: "入口速度设为 0.1 m/s" / "出口压力改成 100 Pa"
- **修改仿真时间**: "把仿真时间延长到 300 秒" / "时间步长改成 0.2 秒"
- **网格控制**: "加密网格，最大单元尺寸 0.005" / "网格精度等级改成 5"
- **输出控制**: "不要导出动画" / "帧率改成 30 fps"

---

## 常见问题解答 (FAQ)

### Q1: 编译时报错 "找不到符号 Model" 或 "程序包 com.comsol.model 不存在"

**原因**: `CylinderFlow.java` 依赖 COMSOL 的 Java 库，不能用普通 `javac` 编译。

**解决方案**:

- 如果只想运行 GUI 配置界面，请只编译 GUI 相关文件：
  ```powershell
  javac -encoding UTF-8 SimulationConfig.java ConfigManager.java QwenClient.java AIConfigParser.java CylinderFlowGUI.java
  ```
- 如果要运行仿真，请使用 `run_comsol.bat`（需要安装 COMSOL 并配置路径）

### Q2: 编译时报错 "类重复: SimulationConfig"

**原因**: 同一个类在多个文件中重复定义。

**解决方案**: 确保 `SimulationConfig.java` 是独立文件，`CylinderFlowGUI.java` 中不要有重复的类定义。

### Q3: GUI 界面显示乱码（方框或问号）

**原因**: 编译时未指定 UTF-8 编码。

**解决方案**: 编译时添加 `-encoding UTF-8` 参数：

```powershell
javac -encoding UTF-8 *.java
```

### Q4: AI 助手显示 "API Key 未配置"

**原因**: 未创建 `.env` 文件或 API Key 格式错误。

**解决方案**:

1. 在项目根目录创建 `.env` 文件
2. 填入正确的 API Key：
   ```env
   QWEN_API_KEY=sk-xxxxxxxxxxxxxxxx
   ```
3. 重启 GUI

### Q5: AI 请求失败 (HTTP 429)

**原因**: API 请求配额已满（免费版有限制）。

**解决方案**:

- 等待几秒后重试
- 切换到其他 AI 服务商（同时配置 QWEN 和 DEEPSEEK 可自动降级）
- 升级为付费版本

### Q6: AI 请求失败 (HTTP 401)

**原因**: API Key 无效或已过期。

**解决方案**: 检查 `.env` 文件中的 API Key 是否正确，重新生成新的 Key。

### Q7: 运行 `run_gui.bat` 时报错 "找不到 comsolmphserver"

**原因**: COMSOL 未安装或路径配置错误。

**解决方案**:

1. 确保已安装 COMSOL Multiphysics 6.0
2. 编辑 `run_gui.bat`，修改 `COMSOL_ROOT` 为你的实际安装路径：
   ```batch
   set COMSOL_ROOT=C:\Program Files\COMSOL\COMSOL60\Multiphysics
   ```

### Q8: 仿真运行很慢或内存不足

**解决方案**:

- 减小 `endTime`（仿真结束时间）
- 增大 `timeStep`（时间步长）
- 降低网格精度（增大 `meshMaxSize`）
- 减少输出帧数（降低 `animationMaxFrames`）

---

## 文件结构

```
comsol/
├── CylinderFlow.java      # COMSOL 仿真主程序
├── CylinderFlowGUI.java   # GUI 配置界面
├── SimulationConfig.java  # 配置数据类
├── ConfigManager.java     # 配置文件管理
├── QwenClient.java        # AI API 客户端
├── AIConfigParser.java    # AI 响应解析器
├── config.json            # 配置文件
├── .env                   # API Key 配置（自行创建）
├── run_gui.bat            # GUI 启动脚本
├── run_comsol.bat         # COMSOL 仿真脚本
└── README.md              # 本文档
```

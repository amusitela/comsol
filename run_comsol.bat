@echo off
REM ============================================
REM COMSOL Java API Cylinder Flow Simulation
REM ============================================

set COMSOL_ROOT=D:\COMSOL60\Multiphysics
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot
set WORK_DIR=c:\Users\Admin\Desktop\kamen\comsol

REM Change to work directory
cd /d "%WORK_DIR%"

echo ============================================
echo COMSOL Cylinder Flow Simulation
echo Working directory: %CD%
echo ============================================
echo.

echo [Step 1/3] Compiling Java source code...
echo ============================================
echo.

REM Use COMSOL Java for compilation (prioritized)
REM Priority: COMSOL JDK > COMSOL JRE > System Java > JAVA_HOME
set JAVAC_CMD="%COMSOL_ROOT%\java\win64\jdk\bin\javac.exe"

if not exist %JAVAC_CMD% (
    echo COMSOL JDK not found, trying JRE...
    set JAVAC_CMD="%COMSOL_ROOT%\java\win64\jre\bin\javac.exe"
    if not exist %JAVAC_CMD% (
        echo COMSOL javac not found, trying system PATH...
        set JAVAC_CMD=javac
        where javac >nul 2>&1
        if errorlevel 1 (
            echo System javac not found, trying JAVA_HOME...
            set JAVAC_CMD="%JAVA_HOME%\bin\javac.exe"
            if not exist %JAVAC_CMD% (
                echo [ERROR] javac not found anywhere!
                echo Please ensure COMSOL JDK is installed or Java JDK is in PATH.
                pause
                exit /b 1
            )
        )
    )
)

echo Using javac: %JAVAC_CMD%
echo.

REM 编译依赖类
echo Compiling SimulationConfig.java...
%JAVAC_CMD% -encoding UTF-8 --release 11 "%WORK_DIR%\SimulationConfig.java"
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Failed to compile SimulationConfig.java
    pause
    exit /b 1
)

echo Compiling ConfigManager.java...
%JAVAC_CMD% -encoding UTF-8 --release 11 "%WORK_DIR%\ConfigManager.java"
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Failed to compile ConfigManager.java
    pause
    exit /b 1
)

REM 编译主程序（需要依赖 COMSOL 库和已编译的类）
echo Compiling CylinderFlow.java...
set COMSOL_CLASSPATH=".;%COMSOL_ROOT%\plugins\*"
%JAVAC_CMD% -encoding UTF-8 --release 11 -cp %COMSOL_CLASSPATH% "%WORK_DIR%\CylinderFlow.java"
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Failed to compile CylinderFlow.java
    pause
    exit /b 1
)

echo.
echo [SUCCESS] All .class files generated
echo.

echo [Step 2/3] Running Simulation...
echo ============================================
"%COMSOL_ROOT%\bin\win64\comsolbatch" -inputfile "%WORK_DIR%\CylinderFlow.class"
echo.

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Simulation execution failed!
    pause
    exit /b 1
)

echo [SUCCESS] Simulation finished. Results saved to CylinderFlow.mph
echo.
pause

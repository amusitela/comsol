@echo off
REM ============================================
REM COMSOL GUI 启动脚本
REM ============================================

setlocal enabledelayedexpansion

REM 设置 COMSOL 路径
set COMSOL_PATH=D:\COMSOL60\Multiphysics
set COMPILE_CMD="%COMSOL_PATH%\bin\win64\comsolcompile.exe"

REM 检查 COMSOL 安装
if not exist %COMPILE_CMD% (
    echo [ERROR] COMSOL compiler not found at:
    echo         %COMPILE_CMD%
    echo Please check your COMSOL installation path.
    pause
    exit /b 1
)

REM 切换到脚本所在目录
cd /d "%~dp0"
echo Working directory: %CD%
echo.

REM ============================================
REM Compile using COMSOL Java (prioritized)
REM ============================================
echo Compiling CylinderFlowGUI.java...

REM Priority: COMSOL JDK > COMSOL JRE > System Java
set JAVAC_CMD="%COMSOL_PATH%\java\win64\jdk\bin\javac.exe"
if not exist %JAVAC_CMD% (
    echo COMSOL JDK not found, trying JRE...
    set JAVAC_CMD="%COMSOL_PATH%\java\win64\jre\bin\javac.exe"
    if not exist %JAVAC_CMD% (
        echo COMSOL javac not found, trying system javac...
        set JAVAC_CMD=javac
        where javac >nul 2>&1
        if errorlevel 1 (
            echo [ERROR] javac not found anywhere!
            echo Please ensure COMSOL JDK is installed or Java JDK is in PATH.
            pause
            exit /b 1
        )
    )
)

echo Using javac: %JAVAC_CMD%

REM Compile with UTF-8 encoding support
%JAVAC_CMD% -encoding UTF-8 CylinderFlowGUI.java
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Failed to compile CylinderFlowGUI.java
    echo.
    echo If javac is not found, please ensure Java JDK is installed.
    pause
    exit /b 1
)

echo.
echo ============================================
echo Compilation successful!
echo ============================================
echo.

REM ============================================
REM 运行 GUI
REM ============================================
echo Starting CylinderFlowGUI...
echo.

REM Use COMSOL Java runtime (prioritized)
REM Priority: COMSOL JDK > COMSOL JRE > System Java
set JAVA_CMD="%COMSOL_PATH%\java\win64\jdk\bin\java.exe"
if not exist %JAVA_CMD% (
    echo COMSOL JDK java not found, trying JRE...
    set JAVA_CMD="%COMSOL_PATH%\java\win64\jre\bin\java.exe"
    if not exist %JAVA_CMD% (
        echo [WARNING] COMSOL Java not found, trying system Java...
        set JAVA_CMD=java
    )
)

echo Using java: %JAVA_CMD%

REM 设置 classpath（当前目录 + COMSOL 插件）
set CLASSPATH=.;%COMSOL_PATH%\plugins\*

%JAVA_CMD% -cp "%CLASSPATH%" CylinderFlowGUI

if %ERRORLEVEL% neq 0 (
    echo.
    echo [ERROR] Failed to start GUI
    echo Error code: %ERRORLEVEL%
    pause
)

endlocal

@echo off
title Build RFID Virtual
echo Gerando JAR...
call "C:\tools\apache-maven-3.9.9\bin\mvn.cmd" clean package -q
if %ERRORLEVEL% == 0 (
    copy /Y target\rfid-virtual-1.3.0.jar rfid-virtual.jar >nul
    echo.
    echo JAR gerado: rfid-virtual.jar
) else (
    echo.
    echo ERRO no build!
)
pause

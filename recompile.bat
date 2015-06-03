
@call :folder %CD%
@set P_JAR=%CURDIRNAME%lib
@echo off

@call :compile 1_rest rest
@call :compile 2_base base
@call :compile 3_connector connector
@call :compile 4_crypto crypto
@call :compile 5_busit busit
@call :compile 6_broker broker
@goto :eof

:compile
@set compileX=n
@set /P compileX=Compile [%2.jar] ? [y/N]: 
@if /i "%compileX%"=="y" (
	cd %1
	call compile %2
	cd ..
)
@goto :eof

:folder
@SET CURDIRNAME=%~dp0
@goto :eof

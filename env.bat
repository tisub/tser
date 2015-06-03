
@call :folder %CD%
@set P_JAR=%CURDIRNAME%lib

@echo off
set classpath=.
for %%f in ("%P_JAR%\*.jar") do (
	call :ask %%f a b
)
@echo on
@goto :eof

:ask
@set addjar=n
@set /P addjar=Add %1 to classpath ? [y/N]: 
@if /i "%addjar%"=="y" (
	set classpath=%classpath%;%1
)
@goto :eof

:folder
@SET CURDIRNAME=%~dp0
@goto :eof

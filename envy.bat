
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
set classpath=%classpath%;%1
@goto :eof

:folder
@SET CURDIRNAME=%~dp0
@goto :eof

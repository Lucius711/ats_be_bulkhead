@echo off
rem Demo Semaphore Bulkhead. Cach dung: demo\run-demo.bat [so_request] [no-bulkhead]
rem   demo\run-demo.bat              -> 15 request, CO bulkhead
rem   demo\run-demo.bat 15 no-bulkhead  -> 15 request, KHONG co bulkhead (de so sanh)
cd /d "%~dp0.."
set N=%~1
if "%N%"=="" set N=15
java demo\BulkheadLoadTest.java %N% %~2
pause

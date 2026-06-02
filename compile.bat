@echo off
set CP=lib\gson-2.10.1.jar;lib\sqlite-jdbc-3.36.0.3.jar

if not exist out mkdir out

javac -cp %CP% -d out -encoding UTF-8 ^
  src\com\nosql2sql\model\ColumnDef.java ^
  src\com\nosql2sql\model\TableSchema.java ^
  src\com\nosql2sql\parser\JsonAnalyzer.java ^
  src\com\nosql2sql\engine\DatabaseEngine.java ^
  src\com\nosql2sql\ui\JsonTreePanel.java ^
  src\com\nosql2sql\ui\SqlTablesPanel.java ^
  src\com\nosql2sql\ui\MainWindow.java ^
  src\com\nosql2sql\Main.java

if %ERRORLEVEL% == 0 (
    echo Derleme basarili!
) else (
    echo Derleme hatasi!
)

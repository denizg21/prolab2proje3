@echo off
set CP=out;lib\gson-2.10.1.jar;lib\sqlite-jdbc-3.36.0.3.jar
java -cp %CP% com.nosql2sql.Main

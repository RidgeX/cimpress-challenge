@echo off
java -cp bin/;lib/jackson-annotations-2.5.3.jar;lib/jackson-core-2.5.3.jar;lib/jackson-databind-2.5.3.jar cimpress.Solver %*

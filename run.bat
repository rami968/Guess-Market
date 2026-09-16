@echo off
echo Starting Guess Market JavaFX Application (Exercise 2)...
java --module-path "%~dp0lib\javafx-sdk\lib" --add-modules javafx.controls,javafx.fxml -cp "%~dp0out\production\UI;%~dp0out\production\Engine;%~dp0lib\*" ui.MainApp
pause

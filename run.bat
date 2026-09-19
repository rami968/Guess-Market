@echo off
echo Starting Guess Market JavaFX Application (Exercise 2)...
xcopy /Y /S "%~dp0UI\src\ui\resources\*" "%~dp0out\production\UI\ui\resources\" >nul 2>&1
java --module-path "%~dp0lib\javafx-sdk\lib" --add-modules javafx.controls,javafx.fxml -cp "%~dp0out\production\UI;%~dp0out\production\Engine;%~dp0lib\*" ui.MainApp
pause

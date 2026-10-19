@echo off
echo Starting Guess Market JavaFX Client (Exercise 3)...
echo Note: Please ensure Apache Tomcat is running on port 8080 with guess_market.war deployed.
java --enable-native-access=javafx.graphics --module-path "%~dp0lib\javafx-sdk\lib" --add-modules javafx.controls,javafx.fxml -cp "%~dp0out\production\UI;%~dp0out\production\Engine;%~dp0UI.jar;%~dp0Engine.jar;%~dp0lib\*" ui.MainApp
pause

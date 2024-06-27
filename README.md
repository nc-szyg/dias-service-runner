# General information
```text
This is program for invoking ./service.sh, ./update-dap.sh scripts with specific parameters (this is UI for script). And also open DIAS application links.
```
# Requirements
```text
Installed Java 11 is required.
Installed 7-zip from Software Center is required.
```
# Settings
### settings.csv
```text
File which contains settings which can be provided (but application should work also on default settings).
```
### service_commands.csv
```text
File which contains commands with description and order base on which buttons will be visible in UI.
Commands from this file will be invoked in dias-docker directory.
```
### update_dap_for_test_commands.csv
```text
File which contains commands with description and order base on which buttons will be visible in UI.
Commands from this file will be invoked in update-content-in-dias-tests directory.
```
### links.csv
```text
File which contains links with description and order base on which buttons will be visible in UI.
Links will be open in default browser.
```

### open_remote_apps.csv
```text
File which contains commands how open remote apps should be invoked.
Commands from this file will be invoked in open-remote-apps directory.
```

### How to build app to JAR file?
```text
If you want to build app to JAR you need:
1. Choose maven on the right side of Intellij
2. Choose Lifecycle directory
3. Double click clean
4. Double click install
5. Your jar app will be visible in directory /target
```

### How to run from bash console
```text
If you want to run from bash console (to f.e. see logs) you need:
java -jar script_control_panel-1.0.jar
```
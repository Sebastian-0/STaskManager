# STaskManager
The aim of this project is to create a task manager similar to the one in Windows 10 but which works on multiple platforms. Additionally the functionally is extended with several new features to make it even better.

Extra functionality includes:
* Historical memory/CPU usage of all processes
* Advanced filtering for the process list
* A list of processes that died recently
* The graphs can be adjusted to display different time spans using an interactive timeline  
* The graphs will display their values at the mouse cursor position
* Read/write speed graphs are logarithmic to make them easier to interpret

## Supported platforms
Currently the task manager supports Windows, Linux (distros with procfs) and Mac OS.

## Building
This project is built using Gradle, running `gradle build` will generate a zip containing the program in the `build` folder.

## License
This program is licensed under GPLv3 (see the LICENSE file for more details).

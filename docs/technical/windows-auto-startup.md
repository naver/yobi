This document let you know how to start Yobi automatically in Windows.

1. Press `Win+R` and type `shell:startup`. Then press `Enter`. Then you can see `Startup` folder.
1. Create batch file like `yobi.bat` in the folder.
1. Fill the file with following shell scripts.

```
cd "[ABSOLUTE_ROUTE_OF_YOBI]\yobi"
del "RUNNING_PID"  
..\play.bat start -DapplyEvolutions.default=true -Dhttp.port=[YOBI_PORT]
```  
  
The example of `[ABSOLUTE_ROUTE_OF_YOBI]` is like this: `C:\Users\Newheart\Desktop\play-2.1.0\yobi`  
The exmaple of `[YOBI_PORT]` is like this: `9000`

The script will be executed after rebooting.

- You can modify the command options of `play.bat` at your flavour.

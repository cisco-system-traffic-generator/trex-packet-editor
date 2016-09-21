Packet editor

### Build
    gradle build

### Run
    gradle run

### Run Test with scapy server
    gradle cleanTest test intTest
    open ./build/reports/tests/index.html

### Run UI tests
    gradle uiTest

### External Scapy server
```
#set SCAPY_SERVER env variable to use external server. default value is
SCAPY_SERVER='localhost:4507'
```

### Enable Debug logging
```
cat <<ENDL > logging.properties
handlers= java.util.logging.ConsoleHandler
.level= DEBUG
ENDL
```


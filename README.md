Packet editor

### Build
    gradle build

### Run

    gradle run

### Run Test with scapy server

    SCAPY_SERVER='localhost:4507' gradle cleanTest test intTest
    open ./build/reports/tests/index.html

### Debug logging

```
cat <<ENDL > logging.properties
handlers= java.util.logging.ConsoleHandler
.level= DEBUG
ENDL
```


# TRex Packet editor [![Build Status](https://travis-ci.org/kisel/trex-packet-editor-gui.svg?branch=master)](https://travis-ci.org/kisel/trex-packet-editor-gui)

Network packed editor GUI for TRex.

### Quickstart
    # Run scapy server
    ./scripts/run_scapy_server &
    
    # Run all tests, including headless UI tests (optional) 
    ./gradlew -Pheadless test intTest uiTest
    
    # Run application
    ./gradlew run

### Run integration and UI tests
make sure the Scapy server is started. see `./scripts/run_scapy_server` helper script
you can also specify external server with an environment variable `export SCAPY_SERVER=localhost:4507`

    ./gradlew intTest uiTest
    # or run tests in headless mode
    ./gradlew -Pheadless intTest uiTest
    # to see test reports:
    # open ./build/reports/tests/index.html


### Howto

##### Build standalone jar file
this builds standalone jar file for UI. scapy_service is not included.
jar is located in **./build/libs/TRexPacketCraftingTool.jar**

    ./gradlew jar

##### Run scapy_server with python3
`PYTHON=python3 ./scripts/run_scapy_server -v --scapy-port 4507`

##### Enable Debug logging for packed editor UI
```
cat <<ENDL > logging.properties
handlers= java.util.logging.ConsoleHandler
.level= DEBUG
ENDL
```


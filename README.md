Packet editor
Requires TRex scapy server

### Build
    gradle build

### Run
    gradle run

### Run Test with scapy server
    gradle cleanTest test intTest
    open ./build/reports/tests/index.html

### Run UI tests
    gradle uiTest

### Run UI tests(headless)
    gradle -Pheadless uiTest

### External Scapy server
By default, UI connects to localhost:4507 scapy server
```
#set SCAPY_SERVER env variable to use external server. default value is
SCAPY_SERVER='localhost:4507'
```

### Getting scapy_server from trex-core
get trex-core from git
if on Mac, apply a fix to use local zmq library
```
pip install zmq # or pip3 install zmq for python3
sed -i bak '/pyzmq.*arch-dep/d' $(find trex-core -name trex_stl_ext.py)
```

##### Run scapy_server with python
you can use `python` or `python3` to run scapy_server
```
(cd $(find trex-core -name scapy_server) && python scapy_zmq_server.py -v --scapy-port 4507)
```

### Enable Debug logging for packed editor UI
```
cat <<ENDL > logging.properties
handlers= java.util.logging.ConsoleHandler
.level= DEBUG
ENDL
```


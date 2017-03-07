#!/bin/sh -x -e

# test script for continuous integration

echo "Downloading scapy server, w/o running"
DOWNLOAD_ONLY=true ./scripts/run_scapy_server

echo "Running scapy server tests"
RUN_SCAPY_TESTS_ONLY=true ./scripts/run_scapy_server

start-stop-daemon --start --quiet --pidfile /var/run/scapy-server.pid -b --exec $PWD/scripts/run_scapy_server

./gradlew --no-daemon -Pheadless jar test intTest $@


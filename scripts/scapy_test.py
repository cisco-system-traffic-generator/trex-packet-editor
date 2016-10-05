#!/usr/bin/env python

# simple scapy server tests

# pip install pcapy scapy zmq nose
# to run use nosetests scapy_test.py

import zmq
import json
import base64
from scapy.all import *

context = zmq.Context()
socket = context.socket(zmq.REQ)
socket.connect('tcp://localhost:4507')

id = 0
global v_handler

def pretty_json(obj):
    return json.dumps(obj, indent=4)

def pprint(obj):
    print(pretty_json(obj))

def cmd(method, params):
    global id
    id += 1
    request = {"jsonrpc": "2.0", "method": method, "params": params, "id": id}
    payload = json.dumps(request)
    print("Request: \n" + pretty_json(request))
    socket.send(payload)
    buf = socket.recv()
    res = json.loads(buf)
    print("Response: \n" + pretty_json(res))
    if res.get('error'):
        raise Exception(res.get('error'))
    return res['result']


v_handler = cmd('get_version_handler', ['1','01'])

# build Ether(dst="10:10:10:10:10:10")/IP(src='127.0.0.1')/TCP(sport=80)
packet_scapy=Ether(dst="10:10:10:10:10:10")/IP(src='127.0.0.1')/TCP(sport=80)
packet= [
        {
            "id": "Ether",
            "fields": [
                {
                    "id": "dst",
                    "value": "10:10:10:10:10:10",
                }
            ]
        },
        {
            "id": "IP",
            "fields": [
                {
                    "id": "dst",
                    "value": "127.0.0.1",
                }
            ]
        },
        {
            "id": "TCP",
            "fields": [
                {
                    "id": "sport",
                    "value": 80,
                }
            ]
        }
]
pcap_path='/tmp/pcaptest.tmp.pcap'

def test_pcap_read():
    wrpcap(pcap_path, [packet_scapy])

    pcap_bin = None
    with open(pcap_path, mode='rb') as file:
        pcap_bin = file.read()
        cmd('read_pcap', [v_handler, base64.b64encode(pcap_bin)])

def test_pcap_write():
    pcap_bin = cmd('write_pcap', [v_handler, [base64.b64encode(bytes(packet_scapy))]]).decode('base64')
    with open('result.pcap', 'w+b') as f:
        f.write(pcap_bin)
        print("Wrote pcap result.pcap")
    wrpcap('result-orig.pcap', [packet_scapy])
    print("Wrote orig pcap file to result-orig.pcap")

def test_reconstruct_pkt():
    cmd('reconstruct_pkt', [v_handler, base64.b64encode(bytes(packet_scapy)), None])

def test_layer_del():
    modif = [
            {"id": "Ether"},
            {"id": "IP"},
            {"id": "TCP", "delete": True},
    ]
    cmd('reconstruct_pkt', [v_handler, base64.b64encode(bytes(packet_scapy)), modif])

def test_layer_field_edit():
    modif = [
            {"id": "Ether"},
            {"id": "IP"},
            {"id": "TCP", "fields": [{"id": "dport", "value": 777}]},
    ]
    cmd('reconstruct_pkt', [v_handler, base64.b64encode(bytes(packet_scapy)), modif])

def test_layer_add():
    modif = [
            {"id": "Ether"},
            {"id": "IP"},
            {"id": "TCP"},
            {"id": "Raw", "fields": [{"id": "load", "value": "GET /helloworld HTTP/1.0\n\n"}]},
    ]
    cmd('reconstruct_pkt', [v_handler, base64.b64encode(bytes(packet_scapy)), modif])

def test_get_all():
    cmd('get_all', [v_handler])

def test_get_definitions_all():
    cmd('get_definitions', [v_handler, None])

def test_get_definitions():
    cmd('get_definitions', [v_handler, ["Ether"]])

def test_get_payload_classes():
    cmd('get_payload_classes', [v_handler, [{"id":"Ether"}]])
    cmd('get_payload_classes', [v_handler, [{"id":"IP"}]])

#test_layer_field_edit()
#test_layer_add()
#test_get_all()
#test_get_definitions()
#test_get_payload_classes()



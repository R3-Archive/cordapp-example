# Example python (via jython) code to use the client RPC interface
# Works in conjunction with the Example CorDapp nodes.
# The nodes must have transacted for you to see the IOU states.

import sys
from com.google.common.net import HostAndPort
from net.corda.client.rpc import CordaRPCClient

if len(sys.argv) != 2:
    print("USAGE: ./jython.sh ExampleClientRPC.py [HOST:ARTEMIS_PORT]")
    exit()

client = CordaRPCClient(HostAndPort.fromString(sys.argv[1]), None, None)
client.start("user1", "test")
proxy = client.proxy(None,0)
print "Proxy is",proxy
txs = proxy.verifiedTransactions().first

print "There are %s 'unspent' IOUs on 'NodeA'" % (len(txs))

if len(txs):
    for txn in txs:
        print(txn.tx.outputs[0].data.iou)
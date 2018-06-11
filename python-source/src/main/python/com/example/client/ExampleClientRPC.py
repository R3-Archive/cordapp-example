import sys
from net.corda.client.rpc import CordaRPCClient
from net.corda.core.contracts import ContractState
from net.corda.core.utilities import NetworkHostAndPort
from org.slf4j import LoggerFactory

class ExampleClientRPC:
    logger = LoggerFactory.getLogger(__name__)

    def __init__(self):
        pass

    def log_state(self, state):
        self.logger.info("{}", state.state.data)

    def main(self):
        if len(sys.argv) != 2:
            raise TypeError("USAGE: ./jython.sh src/main/python/com/example/client/ExampleClientRPC.py [HOST:ARTEMIS_PORT]")

        node_address = NetworkHostAndPort.parse(sys.argv[1])
        client = CordaRPCClient(node_address)
        proxy = client.start("user1", "test").getProxy()

        snapshot, updates = proxy.vaultTrack(ContractState)

        print(snapshot)

if __name__ == '__main__':
    ExampleClientRPC().main()
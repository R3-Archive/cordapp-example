from net.corda.core.contracts import LinearState
from net.corda.core.schemas import QueryableState

class IOUState(LinearState, QueryableState):
    def __init__(self, value, lender, borrower, linearId):
        self.value = value
        self.lender = lender
        self.borrower = borrower
        self.linearId = linearId
        # The public keys of the involved parties.
        self.participants = [lender, borrower]

    def generateMappedObject(schema):
        return when (schema) {
            is IOUSchemaV1 -> IOUSchemaV1.PersistentIOU(
            this.lender.name.toString(),
            this.borrower.name.toString(),
            this.value,
            this.linearId.id
        }
        else -> raise TypeError("Unrecognised schema $schema")

    def supportedSchemas():
        return list(IOUSchemaV1)
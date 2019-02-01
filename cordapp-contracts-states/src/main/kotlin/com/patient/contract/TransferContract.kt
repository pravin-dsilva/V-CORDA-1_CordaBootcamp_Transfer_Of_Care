package com.patient.contract

import com.patient.state.PatientState
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction

class TransferContract : Contract {
    companion object {
        val ID = "com.patient.contract.TransferContract"
    }

    interface Commands : CommandData {
        class Request : TypeOnlyCommandData(), Commands
        class Approval : TypeOnlyCommandData(), Commands
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()
        when (command.value) {
            is Commands.Request -> verifyRequest(tx, command)
            is Commands.Approval -> verifyApproval(tx, command)

        }
    }

    private fun verifyRequest(tx: LedgerTransaction, command: CommandWithParties<Commands>) {
        requireThat {
            "Transaction should have zero inputs" using (tx.inputs.isEmpty())
            "Transaction should have one output" using (tx.outputs.size == 1)
            val outputState = tx.outputStates.get(0) as PatientState
            //val outputState2 = tx.outputsOfType<InvoiceState>().get(0)
            "Admission should be signed by hospital and municipality" using (command.signers.containsAll(listOf(outputState.hospital.owningKey)))
            "The ehr value should be positive" using (outputState.ehr > 0)
        }
    }

    private fun verifyApproval(tx: LedgerTransaction, command: CommandWithParties<Commands>) {
        requireThat {
            "Transaction should have zero inputs" using (tx.inputs.isEmpty())
            "Transaction should have one output" using (tx.outputs.size == 1)
            val outputState = tx.outputStates.get(0) as PatientState
            //val outputState2 = tx.outputsOfType<InvoiceState>().get(0)
            "Admission should be signed by hospital and municipality" using (command.signers.containsAll(listOf(outputState.hospital.owningKey, outputState.municipality.owningKey)))
            "The ehr value should be positive" using (outputState.ehr > 0)
        }

    }
}
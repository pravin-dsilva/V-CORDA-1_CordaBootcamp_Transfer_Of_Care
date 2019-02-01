
package com.patient.contract

import com.patient.state.PatientState
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction

class DischargeContract: Contract {
    companion object {
        val ID = "com.template.contract.DischargeContract"
    }

    interface Commands: CommandData {
        class Discharge: TypeOnlyCommandData(), Commands
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()
        when (command.value){
            is Commands.Discharge -> verifyDischarge(tx, command)

        }
    }

    private fun verifyDischarge(tx: LedgerTransaction, command: CommandWithParties<Commands>) {
        requireThat {
            "Transaction should have zero inputs" using (tx.inputs.isEmpty())
            "Transaction should have one output" using (tx.outputs.size == 1)
            val outputState = tx.outputStates.get(0) as PatientState
            //val outputState2 = tx.outputsOfType<InvoiceState>().get(0)
            "Admission should be signed by hospital" using (command.signers.contains(outputState.hospital.owningKey))
            "The ehr value should be positive" using (outputState.ehr > 0)
           // "The event description should not be blank" using (outputState.eventDescription.isNotBlank())
        }
    }
}
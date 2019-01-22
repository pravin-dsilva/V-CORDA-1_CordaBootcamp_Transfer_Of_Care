package com.template.contract

import com.template.state.Admission
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction

class AdmissionContract: Contract {
    companion object {
        val ID = "com.template.contract.AdmissionContract"
    }

    interface Commands: CommandData {
        class Admit: TypeOnlyCommandData(), Commands
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()
        when (command.value){
            is Commands.Admit -> verifyAdmission(tx, command)

        }
    }

    private fun verifyAdmission(tx: LedgerTransaction, command: CommandWithParties<Commands>) {
        requireThat {
            "Transaction should have zero inputs" using (tx.inputs.isEmpty())
            "Transaction should have one output" using (tx.outputs.size == 1)
           val outputState = tx.outputStates.get(0) as Admission
            //val outputState2 = tx.outputsOfType<InvoiceState>().get(0)
            "Admission should be signed by hospital" using (command.signers.contains(outputState.hospital.owningKey))
            "The ehr value should be positive" using (outputState.ehr > 0)
        }

    }
}
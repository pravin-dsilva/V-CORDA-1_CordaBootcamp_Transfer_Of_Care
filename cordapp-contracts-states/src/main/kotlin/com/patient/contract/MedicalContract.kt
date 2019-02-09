package com.patient.contract


import com.patient.state.PatientState
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction

class MedicalContract : Contract {
    companion object {
        val ID = "com.patient.contract.MedicalContract"
    }

    interface Commands : CommandData {
        class Admit : TypeOnlyCommandData(), Commands
        class Update : TypeOnlyCommandData(), Commands
        class RequestCare : TypeOnlyCommandData(), Commands
        class ApproveCare : TypeOnlyCommandData(), Commands
        class Discharge : TypeOnlyCommandData(), Commands
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()
        when (command.value) {
            is Commands.Admit -> verifyAdmission(tx, command)
            is Commands.Update -> verifyUpdate(tx, command)
            is Commands.RequestCare -> verifyRequest(tx, command)
            is Commands.ApproveCare -> verifyApproval(tx, command)
            is Commands.Discharge -> verifyDischarge(tx, command)
        }
    }

    private fun verifyAdmission(tx: LedgerTransaction, command: CommandWithParties<Commands>) {
        requireThat {
            "Transaction should have zero inputs" using (tx.inputs.isEmpty())
            "Transaction should have one output" using (tx.outputs.size == 1)
            val outputState = tx.outputStates.get(0) as PatientState
            "Admission should be signed by hospital and municipality" using (command.signers.containsAll(listOf(outputState.hospital.owningKey, outputState.municipality.owningKey)))
            "The ehr value should be positive" using (outputState.ehr > 0)
            "The patient ID  value should be positive" using (outputState.patientId > 0)
        }

    }

    private fun verifyUpdate(tx: LedgerTransaction, command: CommandWithParties<Commands>) {
        requireThat {
            "Transaction should have one input" using (tx.inputs.size == 1)
            "Transaction should have one output" using (tx.outputs.size == 1)
            val outputState = tx.outputStates.get(0) as PatientState
            "Update should be signed by hospital" using (command.signers.contains(outputState.hospital.owningKey))
            "The event description should not be blank" using (outputState.event.isNotBlank())
        }
    }

    private fun verifyRequest(tx: LedgerTransaction, command: CommandWithParties<Commands>) {
        requireThat {
            "Transaction should have one input" using (tx.inputs.size == 1)
            "Transaction should have one output" using (tx.outputs.size == 1)
            val outputState = tx.outputStates.get(0) as PatientState
            "Admission should be signed by hospital" using (command.signers.contains(outputState.hospital.owningKey))
        }
    }

    private fun verifyApproval(tx: LedgerTransaction, command: CommandWithParties<Commands>) {
        requireThat {
            "Transaction should have one input" using (tx.inputs.size == 1)
            "Transaction should have one output" using (tx.outputs.size == 1)
            val outputState = tx.outputStates.get(0) as PatientState
            "Admission should be signed by municipality" using (command.signers.contains(outputState.municipality.owningKey))
        }
    }

    private fun verifyDischarge(tx: LedgerTransaction, command: CommandWithParties<Commands>) {
        requireThat {
            "Transaction should have one input" using (tx.inputs.size == 1)
            "Transaction should have one output" using (tx.outputs.size == 1)
            val outputState = tx.outputStates.get(0) as PatientState
            "Discharge should be signed by municipality and Hospital" using (command.signers.containsAll(listOf(outputState.hospital.owningKey, outputState.municipality.owningKey)))
            "Transaction should have a single discharge attachment" using (tx.attachments.filter { it !is ContractAttachment }.size == 1)
        }
    }
}
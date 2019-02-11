package com.patient

import co.paralleluniverse.fibers.Suspendable
import com.patient.contract.MedicalContract
import com.patient.state.PatientState
import com.template.MedicalSchemaV1
import net.corda.core.contracts.requireThat
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

@InitiatingFlow
@StartableByRPC
class DischargeFlow(private val patientId: Int,
                    private val dischargeAttachment: SecureHash
) : FlowLogic<SignedTransaction>() {

    override val progressTracker: ProgressTracker? = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {
        // Get the notary
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        //Verify there is an existing Patient ID to make updates.

        val patientStatusIndex = builder { MedicalSchemaV1.PersistentMedicalState::patientId.equal(patientId) }
        val patientIdCriteria = QueryCriteria.VaultCustomQueryCriteria(patientStatusIndex)

        val patientAdmitIndex = builder { MedicalSchemaV1.PersistentMedicalState::admitStatus.equal("ADMITTED") }
        val patientAdmitCriteria = QueryCriteria.VaultCustomQueryCriteria(patientAdmitIndex)
        val patientCriteria = patientIdCriteria.and(patientAdmitCriteria)

        val inputState = serviceHub.vaultService.queryBy<PatientState>(patientCriteria).states.first()
        try {
            logger.info("Results:" + inputState)
            if (inputState.state.data.patientId != this.patientId)
                throw FlowException("No Patient exists")
        } catch (e: NoSuchElementException) {
            throw FlowException("List is empty. Cannot Update")
        }
        val outputState = inputState.state.data.copy(status = "DISCHARGED")

        // Building the transaction
        val transactionBuilder = TransactionBuilder(notary)
                .addInputState(inputState)
                .addOutputState(outputState, MedicalContract.ID)
                .addCommand(MedicalContract.Commands.Discharge(), ourIdentity.owningKey, outputState.municipality.owningKey)
                .addAttachment(dischargeAttachment)

        val partSignedTx = serviceHub.signInitialTransaction(transactionBuilder)

        val counterpartySession = initiateFlow(outputState.municipality)
        val signedTx = subFlow(CollectSignaturesFlow(partSignedTx, listOf(counterpartySession)))

        return subFlow(FinalityFlow(signedTx))
    }
}

@InitiatedBy(DischargeFlow::class)
class AgreeFlow(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val signTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                // We ensure that the transaction contains an AgreementContract.
                if (stx.toLedgerTransaction(serviceHub, false).outputsOfType<PatientState>().isEmpty()) {
                    throw FlowException("Agreement transaction did not contain an output PatientState")
                }
                // We delegate checking entirely to the AgreementContract.
            }
        }
        subFlow(signTransactionFlow)
    }
}
package com.patient

import co.paralleluniverse.fibers.Suspendable
import com.patient.contract.MedicalContract
import com.patient.state.PatientState
import com.template.MedicalSchemaV1
import net.corda.core.flows.*
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

@InitiatingFlow
@StartableByRPC
class TransferApprovalFlow(val patientId: Int,
                           val approval: String
) : FlowLogic<SignedTransaction>() {

    override val progressTracker: ProgressTracker? = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {
        // Get the notary
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        //Verify there is an existing Patient ID to make updates.
        val patientStatusIndex = builder { MedicalSchemaV1.PersistentMedicalState::patientId.equal(patientId) }
        val patientIdCriteria = QueryCriteria.VaultCustomQueryCriteria(patientStatusIndex)
        val careRequestIndex = builder { MedicalSchemaV1.PersistentMedicalState::careStatus.equal("Care Requested") }
        val careRequestCriteria = QueryCriteria.VaultCustomQueryCriteria(careRequestIndex)
        val patientCriteria = patientIdCriteria.and(careRequestCriteria)
        val inputState = serviceHub.vaultService.queryBy<PatientState>(patientCriteria).states.first()
        try {
            val inputStateData = inputState.state.data
            logger.info("Results:" + inputStateData)
            if (inputStateData.patientId != this.patientId)
                throw FlowException("No Patient exists")
        } catch (e: NoSuchElementException) {
            throw FlowException("List is empty. Cannot Update")
        }
        val outputState: PatientState
        if (approval.equals("APPROVE")) {
            //outputState = listOf(inputState.state.data.copy(care = "APPROVED")
            outputState = inputState.state.data.copy(participants = listOf(ourIdentity), care = "APPROVED")
        } else {
            outputState = inputState.state.data.copy(care = "REJECTED")
        }

        // Building the transaction
        val transactionBuilder = TransactionBuilder(notary)
                .addInputState(inputState)
                .addOutputState(outputState, MedicalContract.ID)
                .addCommand(MedicalContract.Commands.ApproveCare(), ourIdentity.owningKey)
        // Verify transaction Builder
        transactionBuilder.verify(serviceHub)

        // Sign the transaction
        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)

        // Notarize and commit
        return subFlow(FinalityFlow(signedTransaction))
    }
}
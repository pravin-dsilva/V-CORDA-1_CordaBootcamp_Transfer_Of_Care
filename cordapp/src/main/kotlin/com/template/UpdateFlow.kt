package com.template

import co.paralleluniverse.fibers.Suspendable
import com.patient.contract.UpdateContract
import com.patient.state.PatientState
import net.corda.core.flows.*
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

@InitiatingFlow
@StartableByRPC
class UpdateFlow(val patientId: Int,
                 val event: String
                 ) : FlowLogic<SignedTransaction>() {

    override val progressTracker: ProgressTracker? = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {
        // Get the notary
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        //Verify there is an existing Patient ID to make updates.

        val patientStatusIndex = builder { MedicalSchemaV1.PersistentMedicalState::patientId.equal(patientId) }
        val patientIdCriteria = QueryCriteria.VaultCustomQueryCriteria(patientStatusIndex)
        try {
             val inputState = serviceHub.vaultService.queryBy<PatientState>(patientIdCriteria).states.single().state.data
            logger.info("Results:" + inputState)
            if (inputState.patientId != this.patientId)
                throw FlowException("No Patient exists")
        } catch (e: NoSuchElementException) {
            throw FlowException("List is empty. Cannot Update")
        }

        val inputState = serviceHub.vaultService.queryBy<PatientState>(patientIdCriteria).states.single()
        val outputState = inputState.state.data.copy(event = event)

        // Building the transaction
        val transactionBuilder = TransactionBuilder(notary).addOutputState(outputState, UpdateContract.ID).addCommand(UpdateContract.Commands.Update(), ourIdentity.owningKey)
        // Verify transaction Builder
        transactionBuilder.verify(serviceHub)

        // Sign the transaction
        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)

        // Notarize and commit
        return subFlow(FinalityFlow(signedTransaction))
    }
}
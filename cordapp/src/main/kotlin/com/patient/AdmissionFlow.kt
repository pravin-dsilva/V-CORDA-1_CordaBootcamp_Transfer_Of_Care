package com.patient

import co.paralleluniverse.fibers.Suspendable
import com.patient.contract.MedicalContract
import com.patient.state.PatientState
import com.template.MedicalSchemaV1
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.*
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

import net.corda.core.node.services.vault.QueryCriteria.VaultCustomQueryCriteria


@InitiatingFlow
@StartableByRPC
class AdmissionFlow(val municipality: Party,
                    val patientId: Int,
                    val ehr: Int,
                    val status: String,
                    val event: String,
                    val care: String) : FlowLogic<SignedTransaction>() {

    override val progressTracker: ProgressTracker? = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {
        // Get the notary
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        // Create the output state
        val outputState = PatientState(ourIdentity,  municipality, patientId, ehr, status, event,care,
                UniqueIdentifier(), listOf(ourIdentity, municipality))

        // Building the transaction
        val transactionBuilder = TransactionBuilder(notary)
                .addOutputState(outputState, MedicalContract.ID)
                .addCommand(MedicalContract.Commands.Admit(), ourIdentity.owningKey, outputState.municipality.owningKey)


        // Verify transaction Builder
        transactionBuilder.verify(serviceHub)

        // Sign the transaction
        val partiallySignedTransaction = serviceHub.signInitialTransaction(transactionBuilder)

        // Send transaction to the municipality node for signing
        val otherPartySession = initiateFlow(outputState.municipality)
        val completelySignedTransaction = subFlow(CollectSignaturesFlow(partiallySignedTransaction, listOf(otherPartySession)))
        // Notarize and commit
        return subFlow(FinalityFlow(completelySignedTransaction))

    }
}

@InitiatedBy(AdmissionFlow::class)
class AdmissionResponderFlow(val otherPartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
         subFlow (object : SignTransactionFlow(otherPartySession) {
            override fun checkTransaction(stx: SignedTransaction) {
                //verify if EHR already exists or add EHR
                val ledgerTx: LedgerTransaction = stx.toLedgerTransaction(serviceHub, false)
                val outputState: PatientState = ledgerTx.outputsOfType<PatientState>().single()
                val ehrIndex = builder { MedicalSchemaV1.PersistentMedicalState::ehr.equal(outputState.ehr) }
                val patientStatusIndex = builder { MedicalSchemaV1.PersistentMedicalState::admitStatus.equal("ADMITTED") }
                val ehrCriteria = VaultCustomQueryCriteria(ehrIndex)
                val admitCriteria = VaultCustomQueryCriteria(patientStatusIndex)
                val patientCriteria = ehrCriteria.and(admitCriteria)
                try {
                    val results = serviceHub.vaultService.queryBy<PatientState>(patientCriteria).states.single().state.data
                    logger.info("Results:" + results)
                    if(results.ehr == outputState.ehr)
                        throw FlowException("This EHR is admitted currently")
                }
                catch (e: NoSuchElementException){
                    logger.info("List is empty")
                }
                val patientIdIndex = builder { MedicalSchemaV1.PersistentMedicalState::ehr.equal(outputState.patientId) }
                val patientIdCriteria = VaultCustomQueryCriteria(patientIdIndex)
                try {
                    val results = serviceHub.vaultService.queryBy<PatientState>(patientIdCriteria).states.single().state.data
                    logger.info("Results:" + results)
                    if(results.patientId == outputState.patientId)
                        throw FlowException("This patient ID is already used")
                }
                catch (e: NoSuchElementException){
                    logger.info("List is empty")
                }
            }
        })

    }

}

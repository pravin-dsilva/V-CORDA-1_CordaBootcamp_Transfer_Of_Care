package com.template

import co.paralleluniverse.fibers.Suspendable
import com.template.contract.TransferContract
import com.template.state.Admission
import com.template.state.Transfer
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

@InitiatingFlow
@StartableByRPC
class TransferRequestFlow(val municipality: Party,
                           val ehr: Int,
                           val transferStatus: String,
                           val transferDescription: String) : FlowLogic<SignedTransaction>() {

    override val progressTracker: ProgressTracker? = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {
        // Get the notary
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        // Create the output state
        val outputState = Transfer(ourIdentity, municipality, ehr, transferStatus, transferDescription,
                UniqueIdentifier(), listOf(ourIdentity, municipality))

        //Verify there is an existing EHR to request transfer.
        val ccyIndex = builder { MedicalSchemaV1.PersistentMedicalState::ehr.equal(outputState.ehr) }
        val criteria = QueryCriteria.VaultCustomQueryCriteria(ccyIndex)
        try {
            val results = serviceHub.vaultService.queryBy<Admission>(criteria).states.single().state.data
            logger.info("Results:" + results)
            if (results.ehr != outputState.ehr)
                throw FlowException("Cannot Add as EHR does not exist.")
        } catch (e: NoSuchElementException) {
            throw FlowException("List is empty. Cannot Update")
        }

        // Building the transaction
        val transactionBuilder = TransactionBuilder(notary).addOutputState(outputState, TransferContract.ID).addCommand(TransferContract.Commands.Request(), ourIdentity.owningKey)
        // Verify transaction Builder
        transactionBuilder.verify(serviceHub)

        // Sign the transaction
        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)

        // Notarize and commit
        return subFlow(FinalityFlow(signedTransaction))
    }
}
package com.template

import co.paralleluniverse.fibers.Suspendable
import com.template.contract.AdmissionContract
import com.template.state.Admission
import net.corda.core.contracts.FungibleAsset

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
class AdmissionFlow(val municipality: Party,
                    val ehr: Int,
                    val status: String) : FlowLogic<SignedTransaction>() {

    override val progressTracker: ProgressTracker? = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {
        // Get the notary
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        // Create the output state
        val outputState = Admission(ourIdentity, municipality, ehr, status,
                UniqueIdentifier(), listOf(ourIdentity, municipality))



        val ccyIndex = builder { MedicalSchemaV1.PersistentMedicalState::ehr.equal(ehr) }
        val criteria = QueryCriteria.VaultCustomQueryCriteria(ccyIndex)
        val result = serviceHub.vaultService.queryBy<Admission>(criteria)
        logger.info("MYRESULT PRAVIN" + result)

        //verify if EHR already exists
        val customVaultQueryService = serviceHub.cordaService(CustomVaultQuery.Service::class.java)
        val getCount = customVaultQueryService.getPatientEHR(outputState.ehr)

        if  (getCount > 0){
            throw FlowException("ehr already exists")
        }

        // Building the transaction
        val transactionBuilder = TransactionBuilder(notary).addOutputState(outputState, AdmissionContract.ID).addCommand(AdmissionContract.Commands.Admit(), ourIdentity.owningKey)


        // Verify transaction Builder
        transactionBuilder.verify(serviceHub)

        // Sign the transaction
        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)

        // Notarize and commit
        return subFlow(FinalityFlow(signedTransaction))
    }
}
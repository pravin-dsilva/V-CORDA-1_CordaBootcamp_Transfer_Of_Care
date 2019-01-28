//package com.template
//
//import co.paralleluniverse.fibers.Suspendable
//import com.template.contract.AdmissionContract
//import com.template.state.Admission
//import net.corda.core.contracts.UniqueIdentifier
//import net.corda.core.flows.*
//import net.corda.core.identity.Party
//import net.corda.core.node.services.queryBy
//import net.corda.core.node.services.vault.*
//import net.corda.core.transactions.LedgerTransaction
//import net.corda.core.transactions.SignedTransaction
//import net.corda.core.transactions.TransactionBuilder
//import net.corda.core.utilities.ProgressTracker
//
//import net.corda.core.node.services.vault.QueryCriteria.VaultCustomQueryCriteria
//
//
//@InitiatingFlow
//@StartableByRPC
//class AdmissionFlow(val municipality: Party,
//                    val ehr: Int,
//                   val hospitalAttachment: Hash) : FlowLogic<SignedTransaction>() {
//
//    override val progressTracker: ProgressTracker? = ProgressTracker()
//
//    @Suspendable
//    override fun call(): SignedTransaction {
//        // Get the notary
//        val notary = serviceHub.networkMapCache.notaryIdentities.first()
//        // Create the output state
//        val outputState = Admission(ourIdentity, municipality, ehr,
//                UniqueIdentifier(), listOf(ourIdentity, municipality))
//
//        // Building the transaction
//        val transactionBuilder = TransactionBuilder(notary)
//                .addOutputState(outputState, AdmissionContract.ID)
//                .addCommand(AdmissionContract.Commands.Admit(), ourIdentity.owningKey, outputState.municipality.owningKey)
//                .addAttachment(hospitalAttachment)
//
//
//        // Verify transaction Builder
//        transactionBuilder.verify(serviceHub)
//
//        // Sign the transaction
//        val partiallySignedTransaction = serviceHub.signInitialTransaction(transactionBuilder)
//
//        // Send transaction to the municipality node for signing
//        val otherPartySession = initiateFlow(outputState.municipality)
//        val completelySignedTransaction = subFlow(CollectSignaturesFlow(partiallySignedTransaction, listOf(otherPartySession)))
//        // Notarize and commit
//        return subFlow(FinalityFlow(completelySignedTransaction))
//
//    }
//}
//
//@InitiatedBy(AdmissionFlow::class)
//class AdmissionResponderFlow(val otherPartySession: FlowSession) : FlowLogic<Unit>() {
//    @Suspendable
//    override fun call() {
//         subFlow (object : SignTransactionFlow(otherPartySession) {
//            override fun checkTransaction(stx: SignedTransaction) {
//                //verify if EHR already exists or add EHR
//                val ledgerTx: LedgerTransaction = stx.toLedgerTransaction(serviceHub, false)
//                val outputState: Admission = ledgerTx.outputsOfType<Admission>().single()
//                val ccyIndex = builder { MedicalSchemaV1.PersistentMedicalState::ehr.equal(outputState.ehr) }
//                val criteria = VaultCustomQueryCriteria(ccyIndex)
//                try {
//                    val results = serviceHub.vaultService.queryBy<Admission>(criteria).states.single().state.data
//                    logger.info("Results:" + results)
//                    if(results.ehr == outputState.ehr)
//                        throw FlowException("EHR already exists")
//                }
//                catch (e: NoSuchElementException){
//                    logger.info("List is empty")
//                }
//            }
//
//        })
//
//    }
//
//}

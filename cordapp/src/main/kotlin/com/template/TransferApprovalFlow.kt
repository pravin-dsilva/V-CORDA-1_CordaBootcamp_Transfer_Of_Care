package com.template

import co.paralleluniverse.fibers.Suspendable
import com.template.contract.TransferContract
import com.template.state.Transfer
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

@InitiatingFlow
@StartableByRPC
class TransferApprovalFlow(val linearIdentifier: UniqueIdentifier) : FlowLogic<SignedTransaction>() {

    override val progressTracker: ProgressTracker? = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {
        // Get the notary
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        // Query the state by linear id
        val vaultQueryCriteria = QueryCriteria.LinearStateQueryCriteria(listOf(ourIdentity), listOf(linearIdentifier))
        val inputState = serviceHub.vaultService.queryBy<Transfer>(vaultQueryCriteria).states.first()


        // 2. Create new transfer of care state
        val outputState = inputState.state.data.copy(transferStatus = "TRANSFER APPROVED")

        // Building the transaction
        val transactionBuilder = TransactionBuilder(notary).addOutputState(outputState, TransferContract.ID).addCommand(TransferContract.Commands.Approval(), ourIdentity.owningKey, outputState.hospital.owningKey)


        // Verify transaction Builder
        transactionBuilder.verify(serviceHub)

        // Sign the transaction
        val partiallySignedTransaction = serviceHub.signInitialTransaction(transactionBuilder)

        // Send transaction to the hospital node for signing
        val otherPartySession = initiateFlow(outputState.hospital)
        val completelySignedTransaction = subFlow(CollectSignaturesFlow(partiallySignedTransaction, listOf(otherPartySession)))

        // Notarize and commit
        return subFlow(FinalityFlow(completelySignedTransaction))

    }
}

    @InitiatedBy(TransferApprovalFlow::class)
    class TransferApprovalResponderFlow(val otherpartySession: FlowSession): FlowLogic<Unit>(){
        @Suspendable
        override fun call() {
            val flow = object : SignTransactionFlow(otherpartySession){
                override fun checkTransaction(stx: SignedTransaction) {
                    // sanity checks on this transaction
                }
            }
            subFlow(flow)
        }
}
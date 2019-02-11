package com.patient


import net.corda.testing.core.singleIdentity
import net.corda.testing.internal.chooseIdentity
import org.junit.Test
import java.util.concurrent.ExecutionException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith


class FlowTests : BaseFlowTests() {

    @Test
    fun `Admission Flow tests`() {
        val stx = hospitalAdmit(municipal.info.chooseIdentity(), 1, 1, "ADMITTED", "", "")
        stx.verifyRequiredSignatures()
        stx.verifySignaturesExcept(listOf(hospital.info.singleIdentity().owningKey, municipal.info.singleIdentity().owningKey))
    }

    @Test
    fun `Admission Flow Trying to admit same patient again`() {
        hospitalAdmit(municipal.info.chooseIdentity(), 1, 1, "ADMITTED", "", "")
        assertFailsWith<ExecutionException> {
            hospitalAdmit(municipal.info.chooseIdentity(), 1, 1, "ADMITTED", "", "")
        }
        //     stx.verifySignaturesExcept(listOf(hospital.info.singleIdentity().owningKey, municipal.info.singleIdentity().owningKey))
    }

    @Test
    fun `Transfer request flow tests`() {
        hospitalAdmit(municipal.info.chooseIdentity(), 1, 1, "DISCHARGED", "", "")
        val stx = hospitalRequestTransfer(1)
        stx.verifyRequiredSignatures()
        listOf(hospital, municipal).forEach { node ->
            assertEquals(stx, node.services.validatedTransactions.getTransaction(stx.id))

            val ltx = node.transaction {
                stx.toLedgerTransaction(node.services)
            }
            assertEquals(1, ltx.inputs.size)

        }
    }

    @Test
    fun `Transfer request for non existing patient`() {
        hospitalAdmit(municipal.info.chooseIdentity(), 1, 1, "ADMITTED", "", "")
        assertFailsWith<ExecutionException> {
            hospitalRequestTransfer(2)
        }
    }

    @Test
    fun `Transfer request for discharged patient`() {
        hospitalAdmit(municipal.info.chooseIdentity(), 1, 1, "ADMITTED", "", "")
        assertFailsWith<ExecutionException> {
            hospitalRequestTransfer(1)
        }
    }


    @Test
    fun `Transfer Approval flow tests`() {
        hospitalAdmit(municipal.info.chooseIdentity(), 1, 1, "DISCHARGED", "", "")
        hospitalRequestTransfer(1)
        val stx = hospitalApprovalTransfer(1, "APPROVE")
        stx.verifyRequiredSignatures()
        listOf(hospital, municipal).forEach { node ->
            assertEquals(stx, node.services.validatedTransactions.getTransaction(stx.id))
            val ltx = node.transaction {
                stx.toLedgerTransaction(node.services)
            }
            assertEquals(1, ltx.inputs.size)
        }
    }

    @Test
    fun `Transfer approval for non existing patient`() {
        hospitalAdmit(municipal.info.chooseIdentity(), 1, 1, "DISCHARGED", "", "")
        hospitalRequestTransfer(1)
        assertFailsWith<ExecutionException> {
            hospitalApprovalTransfer(2, "APPROVE")
        }
    }

    @Test
    fun `Transfer approval for patient who never requested for care`() {
        hospitalAdmit(municipal.info.chooseIdentity(), 1, 1, "ADMITTED", "", "")
        assertFailsWith<ExecutionException> {
            hospitalApprovalTransfer(2, "APPROVE")
        }
    }

    @Test
    fun `Update flow tests`() {
        hospitalAdmit(municipal.info.chooseIdentity(), 1, 1, "ADMITTED", "", "")
        val stx = hospitalUpdate(1, "test event")
        stx.verifyRequiredSignatures()
        listOf(hospital, municipal).forEach { node ->
            assertEquals(stx, node.services.validatedTransactions.getTransaction(stx.id))
            val ltx = node.transaction {
                stx.toLedgerTransaction(node.services)
            }
            assertEquals(1, ltx.inputs.size)
        }
    }

    @Test
    fun `Update flow trying to update for non existing patient`() {
        hospitalAdmit(municipal.info.chooseIdentity(), 1, 1, "ADMITTED", "", "")
        assertFailsWith<ExecutionException> {
            hospitalUpdate(2, "test event")
        }
    }

    @Test
    fun `Discharge flow tests`() {
        hospitalAdmit(municipal.info.chooseIdentity(), 1, 1, "ADMITTED", "", "")
        val stx = hospitalDischarge(1, medicalAttachment)
        stx.verifyRequiredSignatures()
        listOf(hospital, municipal).forEach { node ->
            assertEquals(stx, node.services.validatedTransactions.getTransaction(stx.id))
            val ltx = node.transaction {
                stx.toLedgerTransaction(node.services)
            }
            assertEquals(1, ltx.inputs.size)
        }
    }
}
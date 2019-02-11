package com.patient



import net.corda.core.crypto.SecureHash
import net.corda.core.identity.Party
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.transactions.SignedTransaction
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.StartedMockNode
import org.junit.After
import org.junit.Before
import java.io.File

abstract class BaseFlowTests {
    lateinit var network: MockNetwork
    lateinit var hospital: StartedMockNode
    lateinit var municipal: StartedMockNode
    lateinit var medicalAttachment: SecureHash
    private lateinit var rpc: CordaRPCOps

    val testJar = "C:\\Users\\Administrator\\Documents\\cordapp-transfer-care\\cordapp\\src\\test\\resources\\medical.jar"

    @Before
    fun setup() {
        network = MockNetwork(listOf("com.patient.contract.MedicalContract", "com.template.schema", "com.patient.state.PatientState", "com.patient", "com.template"))
        hospital = network.createPartyNode()
        municipal = network.createPartyNode()

        val attachmentInputStream = File(testJar).inputStream()
        hospital.transaction {
            medicalAttachment = hospital.services.attachments.importAttachment(attachmentInputStream, "user", "medical")
        }
        // For real nodes this happens automatically, but we have to manually register the flow for tests.
        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    fun hospitalAdmit(counterParty: Party, patientId: Int, ehr: Int, status: String, event: String, care: String): SignedTransaction {
        val flow = AdmissionFlow(counterParty, patientId, ehr, status, event, care)
        val future = hospital.startFlow(flow)
        network.runNetwork()
        return future.get()
    }

    fun hospitalRequestTransfer(patientId: Int): SignedTransaction {
        val transferReqFlow = TransferRequestFlow(patientId)
        val flowTx = hospital.startFlow(transferReqFlow)
        network.runNetwork()
        return flowTx.get()
    }

    fun hospitalApprovalTransfer(patientId: Int, approval: String): SignedTransaction {
        val transferApprovalFlow = TransferApprovalFlow(patientId, approval)
        val flowTx = municipal.startFlow(transferApprovalFlow)
        network.runNetwork()
        return flowTx.get()
    }

    fun hospitalUpdate(patientId: Int, event: String): SignedTransaction {
        val updateFlow = UpdateFlow(patientId, event)
        val flowTx = hospital.startFlow(updateFlow)
        network.runNetwork()
        return flowTx.get()
    }

    fun hospitalDischarge(patientId: Int, dischargeAttachment: SecureHash): SignedTransaction{
        val dischargeFlow = DischargeFlow(patientId, dischargeAttachment)
        val flowTx = hospital.startFlow(dischargeFlow)
        network.runNetwork()
        return flowTx.get()
    }
}
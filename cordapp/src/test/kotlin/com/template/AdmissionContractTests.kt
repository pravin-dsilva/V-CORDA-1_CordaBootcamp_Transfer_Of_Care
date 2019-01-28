//package com.template
//
//import com.template.contract.AdmissionContract
//import com.template.state.Admission
//import net.corda.core.contracts.Contract
//import com.template.contract.AdmissionContract.Companion.ID
//import net.corda.core.identity.CordaX500Name
//import net.corda.core.identity.Party
//
//
//import net.corda.testing.core.TestIdentity
//import net.corda.testing.node.MockServices
//import net.corda.testing.node.ledger
//
//import org.junit.Test
//import java.lang.Compiler.command
//import net.corda.core.contracts.UniqueIdentifier
//import jdk.nashorn.tools.ShellFunctions.input
//import net.corda.core.identity.AbstractParty
//
//
//class AdmissionContractTests {
//    private val ledgerServices = MockServices()
//      private val partyA = TestIdentity(CordaX500Name("PartyA", "", "GB"))
//      private val partyB = TestIdentity(CordaX500Name("PartyB", "", "GB"))
//
//
//    @Test
//    fun tokenContractImplementsContract() {
//        assert((AdmissionContract() is Contract))
//    }
//@Test
//fun `transaction must have no inputs`() {
//    ledgerServices.ledger {
//        transaction {    public List<AbstractParty> getParticipants() {
//            return ImmutableList.of();
//        }
//            input( ID, Admission(  partyA.party, partyB.party, 1, "", linearId,List<AbstractParty>))
//            command(listOf(partyA.publicKey, partyB.publicKey), AdmissionContract.Commands.Admit())
//            `fails with`("No inputs should be consumed when issuing an IOU.")
//        }
//    }
//}
//}
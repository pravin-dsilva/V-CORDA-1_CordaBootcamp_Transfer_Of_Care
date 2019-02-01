package com.template

import com.patient.contract.AdmissionContract
import net.corda.core.contracts.Contract
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger

import org.junit.Test


class AdmissionContractTests {
    private val ledgerServices = MockServices()
    private val partyA = TestIdentity(CordaX500Name("PartyA", "", "GB"))
    private val partyB = TestIdentity(CordaX500Name("PartyB", "", "GB"))
    private val linearId = UniqueIdentifier()
    private val participants = listOf(partyA.party,partyB.party)
    private val admissionState = Admission(partyA.party, partyB.party, 1, "", linearId, participants)
    @Test
    fun tokenContractImplementsContract() {
        assert((AdmissionContract() is Contract))
    }


    @Test
    fun `transaction must have 0 inputs`() {
        ledgerServices.ledger {
            transaction {
                output(AdmissionContract.ID, admissionState)
                command(listOf(partyA.publicKey, partyB.publicKey), AdmissionContract.Commands.Admit())
                verifies()
            }
        }
    }

    @Test
    fun `transaction must have 1 output`() {
        ledgerServices.ledger {
            transaction {
                output(AdmissionContract.ID, admissionState)
                output(AdmissionContract.ID, admissionState)
                command(listOf(partyA.publicKey, partyB.publicKey), AdmissionContract.Commands.Admit())
                fails()
            }
            transaction {
                output(AdmissionContract.ID, admissionState)
                command(listOf(partyA.publicKey, partyB.publicKey), AdmissionContract.Commands.Admit())
                verifies()
            }
        }
    }
    @Test
    fun `transaction must have hospital and municipality as required signer`() {
        ledgerServices.ledger {
            transaction {
                // Municipality is not a required signer, will fail.
                output(AdmissionContract.ID, admissionState)
                command(partyA.publicKey, AdmissionContract.Commands.Admit())
                fails()
            }

            transaction {
                // Municipality is a required signer, will verify.
                output(AdmissionContract.ID, admissionState)
                command(listOf(partyA.publicKey, partyB.publicKey), AdmissionContract.Commands.Admit())
                verifies()
            }
            transaction {
                // Municipality is also a required signer, will verify.
                output(AdmissionContract.ID, admissionState)
                command(partyB.publicKey, AdmissionContract.Commands.Admit())
                fails()
            }
        }
    }
}
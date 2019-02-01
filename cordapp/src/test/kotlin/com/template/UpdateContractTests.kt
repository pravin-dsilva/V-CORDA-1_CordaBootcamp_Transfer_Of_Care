package com.template

import com.patient.contract.UpdateContract
import net.corda.core.contracts.Contract
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger

import org.junit.Test


class UpdateContractTests {
    private val ledgerServices = MockServices()
    private val partyA = TestIdentity(CordaX500Name("PartyA", "", "GB"))
    private val partyB = TestIdentity(CordaX500Name("PartyB", "", "GB"))
    private val linearId = UniqueIdentifier()
    private val participants = listOf(partyA.party,partyB.party)
    private val updateState = Update(partyA.party, partyB.party, 1, "abc", "mock description", linearId, participants)
    @Test
    fun updateContractImplementsContract() {
        assert((UpdateContract() is Contract))
    }


    @Test
    fun `transaction must have 0 inputs`() {
        ledgerServices.ledger {
            transaction {
                output(UpdateContract.ID, updateState)
                command(listOf(partyA.publicKey, partyB.publicKey), UpdateContract.Commands.Update())
                verifies()
            }
        }
    }

    @Test
    fun `transaction must have 1 output`() {
        ledgerServices.ledger {
            transaction {
                output(UpdateContract.ID, updateState)
                output(UpdateContract.ID, updateState)
                command(listOf(partyA.publicKey, partyB.publicKey), UpdateContract.Commands.Update())
                fails()
            }
            transaction {
                output(UpdateContract.ID, updateState)
                command(listOf(partyA.publicKey, partyB.publicKey), UpdateContract.Commands.Update())
                verifies()
            }
        }
    }
    @Test
    fun `transaction must have hospital as required signer`() {
        ledgerServices.ledger {
             transaction {
                // Municipality is a required signer, will verify.
                output(UpdateContract.ID, updateState)
                command(listOf(partyA.publicKey), UpdateContract.Commands.Update())
                verifies()
            }

        }
    }
}
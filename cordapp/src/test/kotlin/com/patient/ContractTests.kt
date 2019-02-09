package com.patient


import com.patient.contract.MedicalContract
import com.patient.state.PatientState
import net.corda.core.contracts.Contract
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test
import java.io.File

class ContractTests {
    private val ledgerServices = MockServices()
    private val hospital = TestIdentity(CordaX500Name("Hospital", "", "GB"))
    private val municipality = TestIdentity(CordaX500Name("Municipal", "", "GB"))
    private val linearId = UniqueIdentifier()
    private val participants = listOf(hospital.party, municipality.party)
    private val patientState = PatientState(hospital.party, municipality.party, 1, 1, "ADMITTED", "sdf", "", linearId, participants)

    @Test
    fun tokenContractImplementsContract() {
        assert((MedicalContract() is Contract))
    }


    @Test
    fun `Admission transaction must have 0 inputs`() {
        ledgerServices.ledger {
            transaction {
                output(com.patient.contract.MedicalContract.ID, patientState)
                command(listOf(hospital.publicKey, municipality.publicKey), MedicalContract.Commands.Admit())
                verifies()
            }
        }
    }

    @Test
    fun `Admission transaction must have 1 output`() {
        ledgerServices.ledger {
            transaction {
                output(MedicalContract.ID, patientState)
                output(MedicalContract.ID, patientState)
                command(listOf(hospital.publicKey, municipality.publicKey), MedicalContract.Commands.Admit())
                fails()
            }
            transaction {
                output(MedicalContract.ID, patientState)
                command(listOf(hospital.publicKey, municipality.publicKey), MedicalContract.Commands.Admit())
                verifies()
            }
        }
    }

    @Test
    fun `Admission transaction must have hospital and municipality as required signer`() {
        ledgerServices.ledger {
            transaction {
                // Municipality is not a required signer, will fail.
                output(MedicalContract.ID, patientState)
                command(hospital.publicKey, MedicalContract.Commands.Admit())
                fails()
            }

            transaction {
                // Municipality is a required signer, will verify.
                output(MedicalContract.ID, patientState)
                command(listOf(hospital.publicKey, municipality.publicKey), MedicalContract.Commands.Admit())
                verifies()
            }
            transaction {
                // Municipality is also a required signer, will verify.
                output(MedicalContract.ID, patientState)
                command(municipality.publicKey, MedicalContract.Commands.Admit())
                fails()
            }
        }
    }

    @Test
    fun `Admission transaction must have ehr non zero and patient id non zero`() {
        ledgerServices.ledger {
            transaction {
                // Municipality is not a required signer, will fail.
                tweak {
                    output(MedicalContract.ID, PatientState(hospital.party, municipality.party, 1, -1, "ADMITTED", "", "", linearId, participants))
                    command(hospital.publicKey, MedicalContract.Commands.Admit())
                    fails()
                }
                output(MedicalContract.ID, PatientState(hospital.party, municipality.party, -1, 1, "ADMITTED", "", "", linearId, participants))
                command(hospital.publicKey, MedicalContract.Commands.Admit())
                fails()
            }
        }
    }

    @Test
    fun `Update transaction must have 1 input`() {
        ledgerServices.ledger {
            transaction {
                input(MedicalContract.ID, patientState)
                output(com.patient.contract.MedicalContract.ID, patientState)
                command(listOf(hospital.publicKey, municipality.publicKey), MedicalContract.Commands.Update())
                verifies()
            }
        }
    }

    @Test
    fun `Update transaction must have 1 output`() {
        ledgerServices.ledger {
            transaction {
                input(MedicalContract.ID, patientState)
                output(MedicalContract.ID, patientState)
                output(MedicalContract.ID, patientState)
                command(listOf(hospital.publicKey, municipality.publicKey), MedicalContract.Commands.Update())
                fails()
            }
            transaction {
                input(MedicalContract.ID, patientState)
                output(MedicalContract.ID, patientState)
                command(listOf(hospital.publicKey, municipality.publicKey), MedicalContract.Commands.Update())
                verifies()
            }
        }
    }

    @Test
    fun `Update transaction cannot have empty input`() {
        ledgerServices.ledger {
            transaction {
                // Event field is empty, should fail
                output(MedicalContract.ID, PatientState(hospital.party, municipality.party, 1, 1, "ADMITTED", "", "", linearId, participants))
                command(hospital.publicKey, MedicalContract.Commands.Update())
                fails()
            }
        }
    }

    @Test
    fun `Update transaction must have hospital as required signer`() {
        ledgerServices.ledger {
            transaction {
                // Hospital is required signer, will verify
                input(MedicalContract.ID, patientState)
                output(MedicalContract.ID, patientState)
                command(hospital.publicKey, MedicalContract.Commands.Update())
                verifies()
            }

            transaction {
                // Municipality is not required signer, will fail.
                input(MedicalContract.ID, patientState)
                output(MedicalContract.ID, patientState)
                command(municipality.publicKey, MedicalContract.Commands.Update())
                fails()
            }
        }
    }

    @Test
    fun `Transfer request transaction must have 1 input`() {
        ledgerServices.ledger {
            transaction {
                input(MedicalContract.ID, patientState)
                output(com.patient.contract.MedicalContract.ID, patientState)
                command(listOf(hospital.publicKey, municipality.publicKey), MedicalContract.Commands.RequestCare())
                verifies()
            }
        }
    }

    @Test
    fun `Transfer request must have 1 output`() {
        ledgerServices.ledger {
            transaction {
                input(MedicalContract.ID, patientState)
                output(MedicalContract.ID, patientState)
                output(MedicalContract.ID, patientState)
                command(listOf(hospital.publicKey, municipality.publicKey), MedicalContract.Commands.RequestCare())
                fails()
            }
            transaction {
                input(MedicalContract.ID, patientState)
                output(MedicalContract.ID, patientState)
                command(listOf(hospital.publicKey, municipality.publicKey), MedicalContract.Commands.RequestCare())
                verifies()
            }
        }
    }

    @Test
    fun `Transfer request must have hospital as required signer`() {
        ledgerServices.ledger {
            transaction {
                // Municipality is not a required signer, will fail.
                input(MedicalContract.ID, patientState)
                output(MedicalContract.ID, patientState)
                command(hospital.publicKey, MedicalContract.Commands.RequestCare())
                verifies()
            }

            transaction {
                // Municipality is also a required signer, will verify.
                input(MedicalContract.ID, patientState)
                output(MedicalContract.ID, patientState)
                command(municipality.publicKey, MedicalContract.Commands.RequestCare())
                fails()
            }
        }
    }

    @Test
    fun `Transfer appoval transaction must have 1 input`() {
        ledgerServices.ledger {
            transaction {
                input(MedicalContract.ID, patientState)
                output(com.patient.contract.MedicalContract.ID, patientState)
                command(listOf(hospital.publicKey, municipality.publicKey), MedicalContract.Commands.ApproveCare())
                verifies()
            }
        }
    }

    @Test
    fun `Transfer approval must have 1 output`() {
        ledgerServices.ledger {
            transaction {
                input(MedicalContract.ID, patientState)
                output(MedicalContract.ID, patientState)
                output(MedicalContract.ID, patientState)
                command(listOf(municipality.publicKey), MedicalContract.Commands.ApproveCare())
                fails()
            }
            transaction {
                input(MedicalContract.ID, patientState)
                output(MedicalContract.ID, patientState)
                command(listOf(municipality.publicKey), MedicalContract.Commands.ApproveCare())
                verifies()
            }
        }
    }

    @Test
    fun `Transfer approval must have municipality as required signer`() {
        ledgerServices.ledger {
            transaction {
                // Municipality is not a required signer, will fail.
                input(MedicalContract.ID, patientState)
                output(MedicalContract.ID, patientState)
                command(municipality.publicKey, MedicalContract.Commands.ApproveCare())
                verifies()
            }

            transaction {
                // Municipality is also a required signer, will verify.
                input(MedicalContract.ID, patientState)
                output(MedicalContract.ID, patientState)
                command(hospital.publicKey, MedicalContract.Commands.ApproveCare())
                fails()
            }
        }
    }

    @Test
    fun `Discharge transaction must have 1 input`() {
        ledgerServices.ledger {
            val attachmentHash = attachment(File("C:\\Users\\Administrator\\Documents\\cordapp-transfer-care\\cordapp\\src\\test\\resources\\medical.jar").inputStream())
            transaction {
                input(MedicalContract.ID, patientState)
                output(MedicalContract.ID, patientState)
                attachment(attachmentHash)
                command(listOf(hospital.publicKey, municipality.publicKey), MedicalContract.Commands.Discharge())
                verifies()
            }
        }
    }

    @Test
    fun `Discharge must have 1 output`() {
        ledgerServices.ledger {
            val attachmentHash = attachment(File("C:\\Users\\Administrator\\Documents\\cordapp-transfer-care\\cordapp\\src\\test\\resources\\medical.jar").inputStream())
            transaction {
                input(MedicalContract.ID, patientState)
                output(MedicalContract.ID, patientState)
                output(MedicalContract.ID, patientState)
                attachment(attachmentHash)
                command(listOf(municipality.publicKey), MedicalContract.Commands.Discharge())
                fails()
            }
            transaction {
                input(MedicalContract.ID, patientState)
                output(MedicalContract.ID, patientState)
                attachment(attachmentHash)
                command(listOf(hospital.publicKey, municipality.publicKey), MedicalContract.Commands.Discharge())
                verifies()
            }
        }
    }

    @Test
    fun `Discharge must have hospital as required signer`() {
        ledgerServices.ledger {
            val attachmentHash = attachment(File("C:\\Users\\Administrator\\Documents\\cordapp-transfer-care\\cordapp\\src\\test\\resources\\medical.jar").inputStream())
            transaction {
                // Municipality is not a required signer, will fail.
                input(MedicalContract.ID, patientState)
                output(MedicalContract.ID, patientState)
                attachment(attachmentHash)
                command(listOf(hospital.publicKey, municipality.publicKey), MedicalContract.Commands.Discharge())
                verifies()
            }

            transaction {
                // Municipality is also a required signer, will verify.
                input(MedicalContract.ID, patientState)
                output(MedicalContract.ID, patientState)
                attachment(attachmentHash)
                command(municipality.publicKey, MedicalContract.Commands.Discharge())
                fails()
            }
        }
    }
}
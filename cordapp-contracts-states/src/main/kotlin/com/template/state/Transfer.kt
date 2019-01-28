package com.template.state

import com.template.MedicalSchemaV1
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState


data class Transfer(val hospital: Party,
                    val municipality: Party,
                    val ehr: Int,
                    val transferStatus: String,
                    val transferDescription: String,
                    override val linearId: UniqueIdentifier,
                    override val participants: List<AbstractParty>) : LinearState, QueryableState {

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is MedicalSchemaV1 -> MedicalSchemaV1.PersistentTransferState(
                    ehr = this.ehr,
                    transferStatus =  this.transferStatus,
                    transferDescription = this.transferDescription
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }
    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(MedicalSchemaV1)
}




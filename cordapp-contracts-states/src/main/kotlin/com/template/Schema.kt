package com.template

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

object MedicalSchema
object MedicalSchemaV1 : MappedSchema(
        schemaFamily = MedicalSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentMedicalState::class.java)) {

    @Entity
    @Table(name = "patient")
    class PersistentMedicalState(
            @Column(name = "patient_ehr", nullable = false)
            var ehr: Int,

            @Column(name = "admit_status", nullable = false)
            var admitStatus: String

    ) : PersistentState()

    @Entity
    @Table(name = "patient_event")
    class PersistentEventState(
            @Column(name = "patient_ehr", nullable = false)
            var ehr: Int,

            @Column(name = "event_type", nullable = false)
            var eventType: String,

            @Column(name = "event_description", nullable = false)
             var eventDescription: String

    ) : PersistentState()
}
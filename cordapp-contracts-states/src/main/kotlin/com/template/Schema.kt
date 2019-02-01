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
            @Column(name = "patient_id",nullable = false)
            var patientId: Int = 0,

            @Column(name = "patient_ehr", nullable = false)
            var ehr: Int = 0,

            @Column(name = "admit_status")
            var admitStatus: String = "",

            @Column(name="event")
            var event: String = "",

            @Column(name = "care_status")
            var careStatus: String = ""



    ) : PersistentState()
}
package com.template

import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.utilities.contextLogger

object CustomVaultQuery {

    @CordaService
    class Service(val services: AppServiceHub) : SingletonSerializeAsToken() {
        private companion object {
            private val log = contextLogger()
        }

        fun getPatientEHR(ehr: Int): Int {
            val nativeQuery = """
                select
                    count(*)
                from
                    patient
                where
                    patient_ehr = """ + ehr
            log.info("SQL to execute: $nativeQuery")
            val session = services.jdbcSession()
            val resultSet = session.prepareStatement(nativeQuery).executeQuery()
            if (resultSet.next()) {
                log.info("Result set is" + resultSet.getInt(1))
            }
            return resultSet.getInt(1)
        }
    }
}


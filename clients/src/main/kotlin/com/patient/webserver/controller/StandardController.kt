package com.patient.webserver.controller

import com.patient.*
import com.patient.state.PatientState
import net.corda.core.messaging.vaultQueryBy
import com.patient.webserver.NodeRPCConnection
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.getOrThrow
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*
import java.io.File
import javax.ws.rs.QueryParam
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status.BAD_REQUEST
import javax.ws.rs.core.Response.Status.CREATED

/**
 * A CorDapp-agnostic controller that exposes standard endpoints.
 */
@RestController
@RequestMapping("/") // The paths for GET and POST requests are relative to this base path.
class StandardController(
        private val rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val proxy = rpc.proxy

    @GetMapping(value = "/status", produces = arrayOf("text/plain"))
    private fun status() = "200"

    @GetMapping(value = "/identities", produces = arrayOf("text/plain"))
    private fun identities() = proxy.nodeInfo().legalIdentities.toString()

    @GetMapping(value = "/peers", produces = arrayOf("text/plain"))
    private fun peers() = proxy.networkMapSnapshot().flatMap { it.legalIdentities }.toString()

    @GetMapping(value = "/notaries", produces = arrayOf("text/plain"))
    private fun notaries() = proxy.notaryIdentities().toString()

    @GetMapping(value = "/flows", produces = arrayOf("text/plain"))
    private fun flows() = proxy.registeredFlows().toString()

    @GetMapping(value = "/patient_states", produces = arrayOf("text/plain"))
    fun getIOUs() = proxy.vaultQueryBy<PatientState>().states.toString()

    @PostMapping(value = "/admit", produces = arrayOf("application/json"))
    fun admit(@QueryParam("counterparty") counterpartyName: String,
              @QueryParam("patientid") patientId: Int,
              @QueryParam("ehr") ehr: Int,
              @QueryParam("status") status: String): Response {
        val counterParty = proxy.partiesFromName(counterpartyName, exactMatch = false).singleOrNull()
                ?: return Response
                        .status(BAD_REQUEST)
                        .entity("Couldn't lookup node identity for $counterpartyName.")
                        .build()

        return try {
            proxy.startFlow(::AdmissionFlow, counterParty, patientId, ehr, status, "", "").returnValue.getOrThrow()
            Response.status(CREATED).entity("Patient Admitted").build()
        } catch (ex: Throwable) {
            Response.status(BAD_REQUEST).entity(ex.message).build()
        }
    }

    @PutMapping(value = "/update", produces = arrayOf("application/json"))
    fun update(@QueryParam("patientId") patientId: Int,
               @QueryParam("event") event: String): Response {

        return try {
            proxy.startFlow(::UpdateFlow, patientId, event).returnValue.getOrThrow()
            Response.status(CREATED).entity("Record updated successfully").build()
        } catch (ex: Throwable) {
            Response.status(BAD_REQUEST).entity(ex.message).build()
        }
    }

    @PostMapping(value = "/discharge", produces = arrayOf("application/json"))
    fun discharge(@QueryParam("patientId") patientId: Int,
                  @QueryParam("filePath") filePath: String): Response {

        return try {
            val attachmentInputStream = File(filePath).inputStream()
            val attachmentHash = proxy.uploadAttachment(attachmentInputStream)
            proxy.startFlow(::DischargeFlow, patientId, attachmentHash).returnValue.getOrThrow()
            Response.status(CREATED).entity("Patient Discharged").build()
        } catch (ex: Throwable) {
            Response.status(BAD_REQUEST).entity(ex.message).build()
        }
    }

    @PostMapping(value = "/request", produces = arrayOf("application/json"))
    fun transferRequest(@QueryParam("patientId") patientId: Int): Response {
        return try {
            proxy.startFlow(::TransferRequestFlow, patientId).returnValue.getOrThrow()
            Response.status(CREATED).entity("Transfer of care requested successfully").build()
        } catch (ex: Throwable) {
            Response.status(BAD_REQUEST).entity(ex.message).build()
        }
    }

    @PostMapping(value = "/approve", produces = arrayOf("application/json"))
    fun transferApproval(
            @QueryParam("patientId") patientId: Int,
            @QueryParam("approval") approval: String): Response {
        return try {
            proxy.startFlow(::TransferApprovalFlow, patientId, approval).returnValue.getOrThrow()
            Response.status(CREATED).entity("Transfer of care approved").build()
        } catch (ex: Throwable) {
            Response.status(BAD_REQUEST).entity(ex.message).build()
        }
    }

}
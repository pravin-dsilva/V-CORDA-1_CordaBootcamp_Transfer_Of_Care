package com.template.webserver.controllers

import com.template.AdmissionFlow
import com.template.state.Admission
import net.corda.core.contracts.ContractState
import net.corda.core.messaging.vaultQueryBy
import com.template.webserver.NodeRPCConnection
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.getOrThrow
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.time.ZoneId
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

    @GetMapping(value = "/servertime", produces = arrayOf("text/plain"))
    private fun serverTime() = LocalDateTime.ofInstant(proxy.currentNodeTime(), ZoneId.of("UTC")).toString()

    @GetMapping(value = "/addresses", produces = arrayOf("text/plain"))
    private fun addresses() = proxy.nodeInfo().addresses.toString()

    @GetMapping(value = "/identities", produces = arrayOf("text/plain"))
    private fun identities() = proxy.nodeInfo().legalIdentities.toString()

    @GetMapping(value = "/platformversion", produces = arrayOf("text/plain"))
    private fun platformVersion() = proxy.nodeInfo().platformVersion.toString()

    @GetMapping(value = "/peers", produces = arrayOf("text/plain"))
    private fun peers() = proxy.networkMapSnapshot().flatMap { it.legalIdentities }.toString()

    @GetMapping(value = "/notaries", produces = arrayOf("text/plain"))
    private fun notaries() = proxy.notaryIdentities().toString()

    @GetMapping(value = "/flows", produces = arrayOf("text/plain"))
    private fun flows() = proxy.registeredFlows().toString()

    @GetMapping(value = "/states", produces = arrayOf("text/plain"))
    private fun states() = proxy.vaultQueryBy<ContractState>().states.toString()

    @GetMapping(value = "/ioustates", produces = arrayOf("text/plain"))
    fun getIOUs() = proxy.vaultQueryBy<Admission>().states.toString()

    @PostMapping(value = "/admit", produces = arrayOf("application/json"))
    fun admit(@QueryParam("counterparty") counterpartyName: String,
              @QueryParam( "ehr") ehr :Int,
              @QueryParam("status") status: String): Response {
        val counterParty = proxy.partiesFromName(counterpartyName, exactMatch = false).singleOrNull()
                ?: return Response
                        .status(BAD_REQUEST)
                        .entity("Couldn't lookup node identity for $counterpartyName.")
                        .build()

        return try {
            proxy.startFlow(::AdmissionFlow, counterParty, ehr, status).returnValue.getOrThrow()
            Response.status(CREATED).entity("Patient Admitted").build()
        } catch (ex: Throwable) {
            Response.status(BAD_REQUEST).entity(ex.message).build()
        }
    }
}

package com.template

import net.corda.testing.node.MockNetwork
import net.corda.testing.node.StartedMockNode
import org.junit.After
import org.junit.Before


class AdmissionFlowTests {
    lateinit var network: MockNetwork
    lateinit var a: StartedMockNode
    lateinit var b: StartedMockNode
    @Before
    fun setup() {
        network = MockNetwork(listOf("com.template.contract.MedicalContract", "com.template.schema"))
        a = network.createPartyNode()
        b = network.createPartyNode()
        // For real nodes this happens automatically, but we have to manually register the flow for tests.
        listOf(a, b).forEach { it.registerInitiatedFlow(AdmissionFlow::class.java) }
        network.runNetwork()
    }


    @After
    fun tearDown() {
        network.stopNodes()
    }

}
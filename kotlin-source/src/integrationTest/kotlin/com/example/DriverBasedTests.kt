package com.example

import net.corda.core.identity.CordaX500Name
import net.corda.core.internal.concurrent.map
import net.corda.core.utilities.getOrThrow
import net.corda.testing.core.TestIdentity
import net.corda.testing.core.singleIdentity
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.driver
import okhttp3.*
import org.eclipse.jetty.http.HttpStatus
import org.junit.Test
import kotlin.test.assertEquals

class DriverBasedTests {
    val bankA = TestIdentity(CordaX500Name("BankA", "", "GB"))
    val bankB = TestIdentity(CordaX500Name("BankB", "", "US"))
    val bankC = TestIdentity(CordaX500Name("BankC", "", "GB"))

    @Test
    fun `node test`() {
        driver(DriverParameters(isDebug = true, startNodesInProcess = true)) {
            // This starts two nodes simultaneously with startNode, which returns a future that completes when the node
            // has completed startup. Then these are all resolved with getOrThrow which returns the NodeHandle list.
            val (partyAHandle, partyBHandle) = listOf(
                    startNode(providedName = bankA.name),
                    startNode(providedName = bankB.name)
            ).map { it.getOrThrow() }

            // This test makes an RPC call to retrieve another node's name from the network map, to verify that the
            // nodes have started and can communicate. This is a very basic test, in practice tests would be starting
            // flows, and verifying the states in the vault and other important metrics to ensure that your CorDapp is
            // working as intended.
            assertEquals(partyAHandle.rpc.wellKnownPartyFromX500Name(bankB.name)!!.name, bankB.name)
            assertEquals(partyBHandle.rpc.wellKnownPartyFromX500Name(bankA.name)!!.name, bankA.name)
        }
    }

    @Test
    fun `node webserver test`() {
        driver(DriverParameters(isDebug = true, startNodesInProcess = true)) {
            val nodeHandles = listOf(
                    startNode(providedName = bankA.name),
                    startNode(providedName = bankB.name)
            ).map { it.getOrThrow() }

            // This test starts each node's webserver and makes an HTTP call to retrieve the body of a GET endpoint on
            // the node's webserver, to verify that the nodes' webservers have started and have loaded the API.
            nodeHandles.forEach { nodeHandle ->
                val webserverHandle = startWebserver(nodeHandle).getOrThrow()

                val nodeAddress = webserverHandle.listenAddress
                val url = "http://$nodeAddress/api/example/ious"

                val request = Request.Builder().url(url).build()
                val client = OkHttpClient()
                val response = client.newCall(request).execute()

                assertEquals("[ ]", response.body().string())
            }
        }
    }

    @Test
    fun `create IOU from http`() {
        driver(DriverParameters(isDebug = true, startNodesInProcess = false)) {
            val (nodeA, webserverA) = startNode(providedName = bankA.name).map { it to startWebserver(it).getOrThrow() }.getOrThrow()
            val (nodeB, webserverB) = startNode(providedName = bankB.name).map { it to startWebserver(it).getOrThrow() }.getOrThrow()
            val (nodeC, webserverC) = startNode(providedName = bankC.name).map { it to startWebserver(it).getOrThrow() }.getOrThrow()

            val url = HttpUrl.Builder()
                    .scheme("http")
                    .host(webserverA.listenAddress.host)
                    .port(webserverA.listenAddress.port)
                    .addPathSegments("api/example/create-iou")
                    .addQueryParameter("iouValue", "40")
                    .addQueryParameter("partyName", nodeB.nodeInfo.singleIdentity().name.toString())
                    .build()

            val request = Request.Builder().url(url).put(RequestBody.create(MediaType.parse("application/json"), "")).build()

            val client = OkHttpClient()
            val response = client.newCall(request).execute()

            assertEquals(HttpStatus.CREATED_201, response.code())
        }
    }
}
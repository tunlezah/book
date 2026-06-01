package com.dogear.reader.feature.upload.server

import fi.iki.elonen.NanoHTTPD
import java.util.Collections
import java.util.concurrent.Executors

/**
 * Caps concurrent upload connections with a fixed thread pool (Security Review §E: resource
 * exhaustion). NanoHTTPD's default runner spawns an unbounded thread per connection; this bounds
 * it so a flood of connections can't exhaust the device.
 */
internal class BoundedRunner(maxConnections: Int) : NanoHTTPD.AsyncRunner {

    private val pool = Executors.newFixedThreadPool(maxConnections)
    private val running = Collections.synchronizedList(mutableListOf<NanoHTTPD.ClientHandler>())

    override fun closeAll() {
        synchronized(running) { running.toList() }.forEach { it.close() }
    }

    override fun closed(clientHandler: NanoHTTPD.ClientHandler) {
        running.remove(clientHandler)
    }

    override fun exec(clientHandler: NanoHTTPD.ClientHandler) {
        running.add(clientHandler)
        pool.submit(clientHandler)
    }
}

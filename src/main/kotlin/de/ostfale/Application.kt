package de.ostfale

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.p
import java.net.URI


// instead of a database
val urlMap = mutableMapOf<String, String>()

// HTML-Code for from
val shortenForm = """
    <html><body><form action='/shorten' method='post'>
    <p><label for='url'>Geben Sie hier Ihre URL an:</label>
    <p><input type='text' name='url' id='url' size='100'>
    <p><input type='submit' value='OK'>
    </form></body></html>
""".trimIndent()

fun main(args: Array<String>): Unit = EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@JvmOverloads
fun Application.module(testing: Boolean = false) {
    routing {
        get("/") {
            call.respondText(shortenForm, ContentType.Text.Html)  // show form
        }
        get("/{id}") {
            lookupUrl()                                           // go to this url
        }
        post("/shorten") {
            shortenUrl()                                          // process form
        }
    }
}


inline suspend fun PipelineContext<Unit, ApplicationCall>.shortenUrl() {
    val req = call.request
    val host = "%s://%s:%s".format(req.origin.scheme, req.host(), req.port())
    val data = call.receiveParameters()
    val url = data["url"]
    if (url is String) {
        val result = runCatching { URI(url).toURL() }
        if (result.isSuccess) {
            val rid: String
            val keys = urlMap.filterValues { it == url }.keys
            if (keys.isNotEmpty())
                rid = keys.first()
            else {
                rid = randomId()
                urlMap[rid] = url
            }
            val href = "$host/$rid"
            call.respondHtml {
                body {
                    p {
                        +"Original URL: "
                        a(url) { +url }
                    }
                    p {
                        +"Shortened URL: "
                        a(href) { +href }
                    }
                    p { +"Shortened URL as text: $href" }
                }
            }
            return
        }
    }
    call.respondText("Invalid data.")
}

suspend inline fun PipelineContext<Unit, ApplicationCall>.lookupUrl() {
    val id = call.parameters["id"]
    if (id is String && urlMap.containsKey(id)) {
        val href = urlMap[id]!!
        call.respondHtml {
            body {
                p {
                    +"Go to: "
                    a(href) { +href }
                }
            }
        }
    } else {
        call.respondText { "Invalid link." }
    }

}

fun randomId(): String {
    val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
    var id: String
    do {
        id = (1..6).map { chars.random() }.joinToString("")
    } while (urlMap.containsKey(id))
    urlMap[id] = "reserved"
    return id
}

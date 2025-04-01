import java.io.BufferedReader //Imports the BufferedReader class, which allows efficient reading of text line by line from an input stream. we use it to read an HTTP response.
import java.io.InputStreamReader //Imports InputStreamReader, which converts a binary input stream (InputStream) to a character stream (Reader). We need it to make Socket.getInputStream() a read stream for BufferedReader.
import java.io.OutputStreamWriter //Imports OutputStreamWriter, which converts character output to a binary stream. We use it to send an HTTP request as plain text via Socket.getOutputStream()
import java.net.Socket //Import the Socket class, which represents the TCP connection between our program and the server. We use Socket(host, port) to establish a connection to the web server.

fun sendHttpRequest(host: String, port: Int, path: String = "/") { //this function accepts a hostname or ipaddress a port number and a path to the web page.
    println("Connecting to $host on port $port...") //print the connection message
    val socket = Socket(host, port) //create tcp  connection to the server using the host and port number. if connection is refuesd, it will throw an exception.
    val writer = OutputStreamWriter(socket.getOutputStream()) //create an output stream writer to send data to the server. we use it to send the HTTP request.
    val reader = BufferedReader(InputStreamReader(socket.getInputStream())) //create a buffered reader to read the response from the server. we use it to read the HTTP response.

    // sending http get
    writer.write("GET $path HTTP/1.1\r\n") //Request the content of the page at $path. HTTP/1.1 is the protocol version. \r\n means end of line (required in HTTP protocol).
    writer.write("Host: $host\r\n") //HTTP/1.1 requires us to specify a "Host" (e.g. "127.0.0.1").
    writer.write("Connection: close\r\n") //We request that the server close the connection after the response. This tells us when the response is complete.
    writer.write("\r\n") // The final empty \r\n signifies the "end of the header" of the HTTP request. Without it, the server will not process the request.
    writer.flush() //We send the content we wrote in `writer` over the network to the server.

    println("---- HTTP RESPONSE START ----") //print the start of the response message
    var line: String? = reader.readLine() //read the first line of the response. This is usually the status line, which contains the HTTP version and status code. Reading with reader.readLine() works until the server sends an empty line (which indicates the end of the HTTP header).

    var redirectLocation: String? = null //reserve space for the redirect address if the server returns a Location header (e.g. Location: http://127.0.0.1/dashboard/). use it later if you need to follow a redirect (302, 301).
    var statusCode: Int? = null // Create a variable for the HTTP status code, e.g. 200, 404, 302 ..

    while (line != null) { //A loop that reads each line of an HTTP response as long as line is non-null. null means the end of the stream or that the server has closed the connection.
        println(line) //print the line of the response

        // Preveri statusno vrstico
        if (line.startsWith("HTTP/1.1")) { //check if the line starts with "HTTP/1.1" If Yes, it means we have a status bar and can extract the status code from it (e.g. 200, 404, 302).
            statusCode = line.split(" ")[1].toInt() //with .split(" ") it is split into parts: ["HTTP/1.1", "200", "OK"]
            //second element ("200") changes to Int → stores in statusCode
        }



        // Preveri header Location
        if (line.lowercase().startsWith("location:")) { //If the line starts with Location: (even if it's lowercase)
            redirectLocation = line.substringAfter("location:", "").trim() //then:  takes the content for location: removes any spaces (trim()) stores the URL for possible redirection
        }

        // Prazen header -> konec glave
        if (line.isEmpty()) break

        //If you encounter a blank line (""), this means the end of the HTTP header.
        //The HTTP standard says that the header is followed by a blank line, then comes the body (e.g. HTML).

        line = reader.readLine() //read the next line of the response
    }

    println("---- HTTP RESPONSE END ----")

    println("---- HTTP RESPONSE BODY START ----")
    while (true) { //An infinite loop begins that will run until the condition is true
        val bodyLine = reader.readLine() ?: break //read one line from the reader If the readLine() method returns null (meaning there is no more data), the break command is immediately executed
        println(bodyLine) //The line read is printed to the console.
    }
    println("---- HTTP RESPONSE BODY END ----")


    //Close all resources to release the connection:


    reader.close() //   reader: for reading data
    writer.close() // writer: for writing
    socket.close() //socket: TCP connection

    // Če je status 301 ali 302 → sledi preusmeritvi
    if (statusCode == 301 || statusCode == 302) { //Checks whether the HTTP response status code is 301 or 302. If it is, the block inside the if is executed.
        println("Redirect detected to: $redirectLocation") //prints the redirect address (from the Location header) to which the client should redirect.
        redirectLocation?.let { //If redirectLocation is not null, the let block is executed.
            val url = if (it.startsWith("http")) { //Creates a URL object that represents the redirect address:
                java.net.URL(it) //If Location already contains a full address use it directly.
            } else {
                java.net.URL("http://$host$it") //If it only contains a relative path, construct the URL using the current host.
            }

            sendHttpRequest(url.host, url.port.takeIf { it != -1 } ?: 80, url.path)

            //Re-sends the HTTP request to the new address:
            //url.host: the domain of the new server.
            //url.port: uses the specified port if it exists, otherwise uses the default port 80.
            //url.path: the path to the new content.
        }
    }
}

fun main() {
   // sendHttpRequest("127.0.0.1", 80, "/projectCN/index.html") //main function
    //sendHttpRequest("127.0.0.1", 80, "/dashboard/")
    sendHttpRequest("www.google.com", 80)

}

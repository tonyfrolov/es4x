# Writing HTTP servers and clients

Vert.x allows you to easily write non blocking HTTP clients and servers.

Vert.x supports the HTTP/1.0, HTTP/1.1 and HTTP/2 protocols.

The base API for HTTP is the same for HTTP/1.x and HTTP/2, specific API
features are available for dealing with the HTTP/2 protocol.

## Creating an HTTP Server

The simplest way to create an HTTP server, using all default options is
as follows:

``` js
let server = vertx.createHttpServer();
```

## Configuring an HTTP server

If you don’t want the default, a server can be configured by passing in
a `HttpServerOptions` instance when creating it:

``` js
let options = new HttpServerOptions()
  .setMaxWebSocketFrameSize(1000000);

let server = vertx.createHttpServer(options);
```

## Configuring an HTTP/2 server

Vert.x supports HTTP/2 over TLS `h2` and over TCP `h2c`.

  - `h2` identifies the HTTP/2 protocol when used over TLS negotiated by
    [Application-Layer Protocol
    Negotiation](https://en.wikipedia.org/wiki/Application-Layer_Protocol_Negotiation)
    (ALPN)

  - `h2c` identifies the HTTP/2 protocol when using in clear text over
    TCP, such connections are established either with an HTTP/1.1
    upgraded request or directly

To handle `h2` requests, TLS must be enabled along with `setUseAlpn`:

``` js
let options = new HttpServerOptions()
  .setUseAlpn(true)
  .setSsl(true)
  .setKeyStoreOptions(new JksOptions()
    .setPath("/path/to/my/keystore"));

let server = vertx.createHttpServer(options);
```

ALPN is a TLS extension that negotiates the protocol before the client
and the server start to exchange data.

Clients that don’t support ALPN will still be able to do a *classic* SSL
handshake.

ALPN will usually agree on the `h2` protocol, although `http/1.1` can be
used if the server or the client decides so.

To handle `h2c` requests, TLS must be disabled, the server will upgrade
to HTTP/2 any request HTTP/1.1 that wants to upgrade to HTTP/2. It will
also accept a direct `h2c` connection beginning with the `PRI *
HTTP/2.0\r\nSM\r\n` preface.

> **Warning**
> 
> most browsers won’t support `h2c`, so for serving web sites you should
> use `h2` and not `h2c`.

When a server accepts an HTTP/2 connection, it sends to the client its
`initial settings`. The settings define how the client can use the
connection, the default initial settings for a server are:

  - `getMaxConcurrentStreams`: `100` as recommended by the HTTP/2 RFC

  - the default HTTP/2 settings values for the others

> **Note**
> 
> Worker Verticles are not compatible with HTTP/2

## Logging network server activity

For debugging purposes, network activity can be logged.

``` js
let options = new HttpServerOptions()
  .setLogActivity(true);

let server = vertx.createHttpServer(options);
```

See the chapter on [logging network activity](#logging_network_activity)
for a detailed explanation.

## Start the Server Listening

To tell the server to listen for incoming requests you use one of the
`listen` alternatives.

To tell the server to listen at the host and port as specified in the
options:

``` js
let server = vertx.createHttpServer();
server.listen();
```

Or to specify the host and port in the call to listen, ignoring what is
configured in the options:

``` js
let server = vertx.createHttpServer();
server.listen(8080, "myhost.com");
```

The default host is `0.0.0.0` which means 'listen on all available
addresses' and the default port is `80`.

The actual bind is asynchronous so the server might not actually be
listening until some time **after** the call to listen has returned.

If you want to be notified when the server is actually listening you can
provide a handler to the `listen` call. For example:

``` js
let server = vertx.createHttpServer();
server.listen(8080, "myhost.com", (res) => {
  if (res.succeeded()) {
    console.log("Server is now listening!");
  } else {
    console.log("Failed to bind!");
  }
});
```

## Getting notified of incoming requests

To be notified when a request arrives you need to set a
`requestHandler`:

``` js
let server = vertx.createHttpServer();
server.requestHandler((request) => {
  // Handle the request in here
});
```

## Handling requests

When a request arrives, the request handler is called passing in an
instance of `HttpServerRequest`. This object represents the server side
HTTP request.

The handler is called when the headers of the request have been fully
read.

If the request contains a body, that body will arrive at the server some
time after the request handler has been called.

The server request object allows you to retrieve the `uri`, `path`,
`params` and `headers`, amongst other things.

Each server request object is associated with one server response
object. You use `response` to get a reference to the
`HttpServerResponse` object.

Here’s a simple example of a server handling a request and replying with
"hello world" to it.

``` js
vertx.createHttpServer().requestHandler((request) => {
  request.response().end("Hello world");
}).listen(8080);
```

### Request version

The version of HTTP specified in the request can be retrieved with
`version`

### Request method

Use `method` to retrieve the HTTP method of the request. (i.e. whether
it’s GET, POST, PUT, DELETE, HEAD, OPTIONS, etc).

### Request URI

Use `uri` to retrieve the URI of the request.

Note that this is the actual URI as passed in the HTTP request, and it’s
almost always a relative URI.

The URI is as defined in [Section 5.1.2 of the HTTP specification -
Request-URI](http://www.w3.org/Protocols/rfc2616/rfc2616-sec5.html)

### Request path

Use `path` to return the path part of the URI

For example, if the request URI was:

a/b/c/page.html?param1=abc\&param2=xyz

Then the path would be

/a/b/c/page.html

### Request query

Use `query` to return the query part of the URI

For example, if the request URI was:

a/b/c/page.html?param1=abc\&param2=xyz

Then the query would be

param1=abc\&param2=xyz

### Request headers

Use `headers` to return the headers of the HTTP request.

This returns an instance of `MultiMap` - which is like a normal Map or
Hash but allows multiple values for the same key - this is because HTTP
allows multiple header values with the same key.

It also has case-insensitive keys, that means you can do the following:

``` js
let headers = request.headers();

// Get the User-Agent:
console.log("User agent is " + headers.get("user-agent"));

// You can also do this and get the same result:
console.log("User agent is " + headers.get("User-Agent"));
```

### Request host

Use `host` to return the host of the HTTP request.

For HTTP/1.x requests the `host` header is returned, for HTTP/1 requests
the `:authority` pseudo header is returned.

### Request parameters

Use `params` to return the parameters of the HTTP request.

Just like `headers` this returns an instance of `MultiMap` as there can
be more than one parameter with the same name.

Request parameters are sent on the request URI, after the path. For
example if the URI was:

/page.html?param1=abc\&param2=xyz

Then the parameters would contain the following:

    param1: 'abc'
    param2: 'xyz

Note that these request parameters are retrieved from the URL of the
request. If you have form attributes that have been sent as part of the
submission of an HTML form submitted in the body of a
`multi-part/form-data` request then they will not appear in the params
here.

### Remote address

The address of the sender of the request can be retrieved with
`remoteAddress`.

### Absolute URI

The URI passed in an HTTP request is usually relative. If you wish to
retrieve the absolute URI corresponding to the request, you can get it
with `absoluteURI`

### End handler

The `endHandler` of the request is invoked when the entire request,
including any body has been fully read.

### Reading Data from the Request Body

Often an HTTP request contains a body that we want to read. As
previously mentioned the request handler is called when just the headers
of the request have arrived so the request object does not have a body
at that point.

This is because the body may be very large (e.g. a file upload) and we
don’t generally want to buffer the entire body in memory before handing
it to you, as that could cause the server to exhaust available memory.

To receive the body, you can use the `handler` on the request, this will
get called every time a chunk of the request body arrives. Here’s an
example:

``` js
request.handler((buffer) => {
  console.log("I have received a chunk of the body of length " + buffer.length());
});
```

The object passed into the handler is a `Buffer`, and the handler can be
called multiple times as data arrives from the network, depending on the
size of the body.

In some cases (e.g. if the body is small) you will want to aggregate the
entire body in memory, so you could do the aggregation yourself as
follows:

``` js
import { Buffer } from "@vertx/core"

// Create an empty buffer
let totalBuffer = Buffer.buffer();

request.handler((buffer) => {
  console.log("I have received a chunk of the body of length " + buffer.length());
  totalBuffer.appendBuffer(buffer);
});

request.endHandler((v) => {
  console.log("Full body received, length = " + totalBuffer.length());
});
```

This is such a common case, that Vert.x provides a `bodyHandler` to do
this for you. The body handler is called once when all the body has been
received:

``` js
request.bodyHandler((totalBuffer) => {
  console.log("Full body received, length = " + totalBuffer.length());
});
```

### Streaming requests

The request object is a `ReadStream` so you can pipe the request body to
any `WriteStream` instance.

See the chapter on [streams](#streams) for a detailed explanation.

### Handling HTML forms

HTML forms can be submitted with either a content type of
`application/x-www-form-urlencoded` or `multipart/form-data`.

For url encoded forms, the form attributes are encoded in the url, just
like normal query parameters.

For multi-part forms they are encoded in the request body, and as such
are not available until the entire body has been read from the wire.

Multi-part forms can also contain file uploads.

If you want to retrieve the attributes of a multi-part form you should
tell Vert.x that you expect to receive such a form **before** any of the
body is read by calling `setExpectMultipart` with true, and then you
should retrieve the actual attributes using `formAttributes` once the
entire body has been read:

``` js
server.requestHandler((request) => {
  request.setExpectMultipart(true);
  request.endHandler((v) => {
    // The body has now been fully read, so retrieve the form attributes
    let formAttributes = request.formAttributes();
  });
});
```

### Handling form file uploads

Vert.x can also handle file uploads which are encoded in a multi-part
request body.

To receive file uploads you tell Vert.x to expect a multi-part form and
set an `uploadHandler` on the request.

This handler will be called once for every upload that arrives on the
server.

The object passed into the handler is a `HttpServerFileUpload` instance.

``` js
server.requestHandler((request) => {
  request.setExpectMultipart(true);
  request.uploadHandler((upload) => {
    console.log("Got a file upload " + upload.name());
  });
});
```

File uploads can be large we don’t provide the entire upload in a single
buffer as that might result in memory exhaustion, instead, the upload
data is received in chunks:

``` js
request.uploadHandler((upload) => {
  upload.handler((chunk) => {
    console.log("Received a chunk of the upload of length " + chunk.length());
  });
});
```

The upload object is a `ReadStream` so you can pipe the request body to
any `WriteStream` instance. See the chapter on [streams](#streams) for a
detailed explanation.

If you just want to upload the file to disk somewhere you can use
`streamToFileSystem`:

``` js
request.uploadHandler((upload) => {
  upload.streamToFileSystem("myuploads_directory/" + upload.filename());
});
```

> **Warning**
> 
> Make sure you check the filename in a production system to avoid
> malicious clients uploading files to arbitrary places on your
> filesystem. See [security notes](#Security%20notes) for more
> information.

### Handling cookies

You use `getCookie` to retrieve a cookie by name, or use `cookieMap` to
retrieve all the cookies.

To remove a cookie, use `removeCookie`.

To add a cookie use `addCookie`.

The set of cookies will be written back in the response automatically
when the response headers are written so the browser can store them.

Cookies are described by instances of `Cookie`. This allows you to
retrieve the name, value, domain, path and other normal cookie
properties.

Same Site Cookies let servers require that a cookie shouldn’t be sent
with cross-site (where Site is defined by the registrable domain)
requests, which provides some protection against cross-site request
forgery attacks. This kind of cookies are enabled using the setter:
`setSameSite`.

Same site cookies can have one of 3 values:

  - None - The browser will send cookies with both cross-site requests
    and same-site requests.

  - Strict - he browser will only send cookies for same-site requests
    (requests originating from the site that set the cookie). If the
    request originated from a different URL than the URL of the current
    location, none of the cookies tagged with the Strict attribute will
    be included.

  - Lax - Same-site cookies are withheld on cross-site subrequests, such
    as calls to load images or frames, but will be sent when a user
    navigates to the URL from an external site; for example, by
    following a link.

Here’s an example of querying and adding cookies:

``` js
import { Cookie } from "@vertx/core"
let someCookie = request.getCookie("mycookie");
let cookieValue = someCookie.getValue();

// Do something with cookie...

// Add a cookie - this will get written back in the response automatically
request.response().addCookie(Cookie.cookie("othercookie", "somevalue"));
```

### Handling compressed body

Vert.x can handle compressed body payloads which are encoded by the
client with the *deflate* or *gzip* algorithms.

To enable decompression set `setDecompressionSupported` on the options
when creating the server.

By default decompression is disabled.

### Receiving custom HTTP/2 frames

HTTP/2 is a framed protocol with various frames for the HTTP
request/response model. The protocol allows other kind of frames to be
sent and received.

To receive custom frames, you can use the `customFrameHandler` on the
request, this will get called every time a custom frame arrives. Here’s
an example:

``` js
request.customFrameHandler((frame) => {

  console.log("Received a frame type=" + frame.type() + " payload" + frame.payload().toString());
});
```

HTTP/2 frames are not subject to flow control - the frame handler will
be called immediatly when a custom frame is received whether the request
is paused or is not

### Non standard HTTP methods

The `OTHER` HTTP method is used for non standard methods, in this case
`rawMethod` returns the HTTP method as sent by the client.

## Sending back responses

The server response object is an instance of `HttpServerResponse` and is
obtained from the request with `response`.

You use the response object to write a response back to the HTTP client.

### Setting status code and message

The default HTTP status code for a response is `200`, representing `OK`.

Use `setStatusCode` to set a different code.

You can also specify a custom status message with `setStatusMessage`.

If you don’t specify a status message, the default one corresponding to
the status code will be used.

> **Note**
> 
> for HTTP/2 the status won’t be present in the response since the
> protocol won’t transmit the message to the client

### Writing HTTP responses

To write data to an HTTP response, you use one of the `write`
operations.

These can be invoked multiple times before the response is ended. They
can be invoked in a few ways:

With a single buffer:

``` js
let response = request.response();
response.write(buffer);
```

With a string. In this case the string will encoded using UTF-8 and the
result written to the wire.

``` js
let response = request.response();
response.write("hello world!");
```

With a string and an encoding. In this case the string will encoded
using the specified encoding and the result written to the wire.

``` js
let response = request.response();
response.write("hello world!", "UTF-16");
```

Writing to a response is asynchronous and always returns immediately
after the write has been queued.

If you are just writing a single string or buffer to the HTTP response
you can write it and end the response in a single call to the `end`

The first call to write results in the response header being written to
the response. Consequently, if you are not using HTTP chunking then you
must set the `Content-Length` header before writing to the response,
since it will be too late otherwise. If you are using HTTP chunking you
do not have to worry.

### Ending HTTP responses

Once you have finished with the HTTP response you should `end` it.

This can be done in several ways:

With no arguments, the response is simply ended.

``` js
let response = request.response();
response.write("hello world!");
response.end();
```

It can also be called with a string or buffer in the same way `write` is
called. In this case it’s just the same as calling write with a string
or buffer followed by calling end with no arguments. For example:

``` js
let response = request.response();
response.end("hello world!");
```

### Closing the underlying connection

You can close the underlying TCP connection with `close`.

Non keep-alive connections will be automatically closed by Vert.x when
the response is ended.

Keep-alive connections are not automatically closed by Vert.x by
default. If you want keep-alive connections to be closed after an idle
time, then you configure `setIdleTimeout`.

HTTP/2 connections send a {@literal GOAWAY} frame before closing the
response.

### Setting response headers

HTTP response headers can be added to the response by adding them
directly to the `headers`:

``` js
let response = request.response();
let headers = response.headers();
headers.set("content-type", "text/html");
headers.set("other-header", "wibble");
```

Or you can use `putHeader`

``` js
let response = request.response();
response.putHeader("content-type", "text/html").putHeader("other-header", "wibble");
```

Headers must all be added before any parts of the response body are
written.

### Chunked HTTP responses and trailers

Vert.x supports [HTTP Chunked Transfer
Encoding](http://en.wikipedia.org/wiki/Chunked_transfer_encoding).

This allows the HTTP response body to be written in chunks, and is
normally used when a large response body is being streamed to a client
and the total size is not known in advance.

You put the HTTP response into chunked mode as follows:

``` js
let response = request.response();
response.setChunked(true);
```

Default is non-chunked. When in chunked mode, each call to one of the
`write` methods will result in a new HTTP chunk being written out.

When in chunked mode you can also write HTTP response trailers to the
response. These are actually written in the final chunk of the response.

> **Note**
> 
> chunked response has no effect for an HTTP/2 stream

To add trailers to the response, add them directly to the `trailers`.

``` js
let response = request.response();
response.setChunked(true);
let trailers = response.trailers();
trailers.set("X-wibble", "woobble").set("X-quux", "flooble");
```

Or use `putTrailer`.

``` js
let response = request.response();
response.setChunked(true);
response.putTrailer("X-wibble", "woobble").putTrailer("X-quux", "flooble");
```

### Serving files directly from disk or the classpath

If you were writing a web server, one way to serve a file from disk
would be to open it as an `AsyncFile` and pipe it to the HTTP response.

Or you could load it it one go using `readFile` and write it straight to
the response.

Alternatively, Vert.x provides a method which allows you to serve a file
from disk or the filesystem to an HTTP response in one operation. Where
supported by the underlying operating system this may result in the OS
directly transferring bytes from the file to the socket without being
copied through user-space at all.

This is done by using `sendFile`, and is usually more efficient for
large files, but may be slower for small files.

Here’s a very simple web server that serves files from the file system
using sendFile:

``` js
vertx.createHttpServer().requestHandler((request) => {
  let file = "";
  if (request.path() == "/") {
    file = "index.html";
  } else if (!request.path().contains("..")) {
    file = request.path();
  }
  request.response().sendFile("web/" + file);
}).listen(8080);
```

Sending a file is asynchronous and may not complete until some time
after the call has returned. If you want to be notified when the file
has been writen you can use `sendFile`

Please see the chapter about [serving files from the
classpath](#classpath) for restrictions about the classpath resolution
or disabling it.

> **Note**
> 
> If you use `sendFile` while using HTTPS it will copy through
> user-space, since if the kernel is copying data directly from disk to
> socket it doesn’t give us an opportunity to apply any encryption.

> **Warning**
> 
> If you’re going to write web servers directly using Vert.x be careful
> that users cannot exploit the path to access files outside the
> directory from which you want to serve them or the classpath It may be
> safer instead to use Vert.x Web.

When there is a need to serve just a segment of a file, say starting
from a given byte, you can achieve this by doing:

``` js
vertx.createHttpServer().requestHandler((request) => {
  let offset = 0;
  try {
    offset = Java.type("java.lang.Long").parseLong(request.getParam("start"));
  } catch(err) {
    // error handling...
  }


  let end = Java.type("java.lang.Long").MAX_VALUE;
  try {
    end = Java.type("java.lang.Long").parseLong(request.getParam("end"));
  } catch(err) {
    // error handling...
  }


  request.response().sendFile("web/mybigfile.txt", offset, end);
}).listen(8080);
```

You are not required to supply the length if you want to send a file
starting from an offset until the end, in this case you can just do:

``` js
vertx.createHttpServer().requestHandler((request) => {
  let offset = 0;
  try {
    offset = Java.type("java.lang.Long").parseLong(request.getParam("start"));
  } catch(err) {
    // error handling...
  }


  request.response().sendFile("web/mybigfile.txt", offset);
}).listen(8080);
```

### Piping responses

The server response is a `WriteStream` instance so you can pipe to it
from any `ReadStream`, e.g. `AsyncFile`, `NetSocket`, `WebSocket` or
`HttpServerRequest`.

Here’s an example which echoes the request body back in the response for
any PUT methods. It uses a pipe for the body, so it will work even if
the HTTP request body is much larger than can fit in memory at any one
time:

``` js
vertx.createHttpServer().requestHandler((request) => {
  let response = request.response();
  if (request.method() === HttpMethod.PUT) {
    response.setChunked(true);
    request.pipeTo(response);
  } else {
    response.setStatusCode(400).end();
  }
}).listen(8080);
```

### Writing HTTP/2 frames

HTTP/2 is a framed protocol with various frames for the HTTP
request/response model. The protocol allows other kind of frames to be
sent and received.

To send such frames, you can use the `writeCustomFrame` on the response.
Here’s an example:

``` js
import { Buffer } from "@vertx/core"

let frameType = 40;
let frameStatus = 10;
let payload = Buffer.buffer("some data");

// Sending a frame to the client
response.writeCustomFrame(frameType, frameStatus, payload);
```

These frames are sent immediately and are not subject to flow control -
when such frame is sent there it may be done before other {@literal
DATA} frames.

### Stream reset

HTTP/1.x does not allow a clean reset of a request or a response stream,
for example when a client uploads a resource already present on the
server, the server needs to accept the entire response.

HTTP/2 supports stream reset at any time during the request/response:

``` js
// Reset the stream
request.response().reset();
```

By default the `NO_ERROR` (0) error code is sent, another code can sent
instead:

``` js
// Cancel the stream
request.response().reset(8);
```

The HTTP/2 specification defines the list of [error
codes](http://httpwg.org/specs/rfc7540.html#ErrorCodes) one can use.

The request handler are notified of stream reset events with the
`request handler` and `response handler`:

``` js
request.response().exceptionHandler((err) => {
  if (err instanceof StreamResetException) {
    let reset = err;
    console.log("Stream reset " + reset.getCode());
  }
});
```

### Server push

Server push is a new feature of HTTP/2 that enables sending multiple
responses in parallel for a single client request.

When a server process a request, it can push a request/response to the
client:

``` js
let response = request.response();

// Push main.js to the client
response.push(HttpMethod.GET, "/main.js", (ar) => {

  if (ar.succeeded()) {

    // The server is ready to push the response
    let pushedResponse = ar.result();

    // Send main.js response
    pushedResponse.putHeader("content-type", "application/json").end("alert(\"Push response hello\")");
  } else {
    console.log("Could not push client resource " + ar.cause());
  }
});

// Send the requested resource
response.sendFile("<html><head><script src=\"/main.js\"></script></head><body></body></html>");
```

When the server is ready to push the response, the push response handler
is called and the handler can send the response.

The push response handler may receive a failure, for instance the client
may cancel the push because it already has `main.js` in its cache and
does not want it anymore.

The `push` method must be called before the initiating response ends,
however the pushed response can be written after.

### Handling exceptions

You can set an `exceptionHandler` to receive any exceptions that happens
before the connection is passed to the `requestHandler` or to the
`webSocketHandler`, e.g during the TLS handshake.

## HTTP Compression

Vert.x comes with support for HTTP Compression out of the box.

This means you are able to automatically compress the body of the
responses before they are sent back to the client.

If the client does not support HTTP compression the responses are sent
back without compressing the body.

This allows to handle Client that support HTTP Compression and those
that not support it at the same time.

To enable compression use can configure it with
`setCompressionSupported`.

By default compression is not enabled.

When HTTP compression is enabled the server will check if the client
includes an `Accept-Encoding` header which includes the supported
compressions. Commonly used are deflate and gzip. Both are supported by
Vert.x.

If such a header is found the server will automatically compress the
body of the response with one of the supported compressions and send it
back to the client.

Whenever the response needs to be sent without compression you can set
the header `content-encoding` to `identity`:

``` js
// Disable compression and send an image
request.response().putHeader(Java.type("io.vertx.core.http.HttpHeaders").CONTENT_ENCODING, Java.type("io.vertx.core.http.HttpHeaders").IDENTITY).sendFile("/path/to/image.jpg");
```

Be aware that compression may be able to reduce network traffic but is
more CPU-intensive.

To address this latter issue Vert.x allows you to tune the 'compression
level' parameter that is native of the gzip/deflate compression
algorithms.

Compression level allows to configure gizp/deflate algorithms in terms
of the compression ratio of the resulting data and the computational
cost of the compress/decompress operation.

The compression level is an integer value ranged from '1' to '9', where
'1' means lower compression ratio but fastest algorithm and '9' means
maximum compression ratio available but a slower algorithm.

Using compression levels higher that 1-2 usually allows to save just
some bytes in size - the gain is not linear, and depends on the specific
data to be compressed - but it comports a non-trascurable cost in term
of CPU cycles required to the server while generating the compressed
response data ( Note that at moment Vert.x doesn’t support any form
caching of compressed response data, even for static files, so the
compression is done on-the-fly at every request body generation ) and in
the same way it affects client(s) while decoding (inflating) received
responses, operation that becomes more CPU-intensive the more the level
increases.

By default - if compression is enabled via `setCompressionSupported` -
Vert.x will use '6' as compression level, but the parameter can be
configured to address any case with `setCompressionLevel`.

## Creating an HTTP client

You create an `HttpClient` instance with default options as follows:

``` js
let client = vertx.createHttpClient();
```

If you want to configure options for the client, you create it as
follows:

``` js
let options = new HttpClientOptions()
  .setKeepAlive(false);
let client = vertx.createHttpClient(options);
```

Vert.x supports HTTP/2 over TLS `h2` and over TCP `h2c`.

By default the http client performs HTTP/1.1 requests, to perform HTTP/2
requests the `setProtocolVersion` must be set to `HTTP_2`.

For `h2` requests, TLS must be enabled with *Application-Layer Protocol
Negotiation*:

``` js
let options = new HttpClientOptions()
  .setProtocolVersion("HTTP_2")
  .setSsl(true)
  .setUseAlpn(true)
  .setTrustAll(true);

let client = vertx.createHttpClient(options);
```

For `h2c` requests, TLS must be disabled, the client will do an HTTP/1.1
requests and try an upgrade to HTTP/2:

``` js
let options = new HttpClientOptions()
  .setProtocolVersion("HTTP_2");

let client = vertx.createHttpClient(options);
```

`h2c` connections can also be established directly, i.e connection
started with a prior knowledge, when `setHttp2ClearTextUpgrade` options
is set to false: after the connection is established, the client will
send the HTTP/2 connection preface and expect to receive the same
preface from the server.

The http server may not support HTTP/2, the actual version can be
checked with `version` when the response arrives.

When a clients connects to an HTTP/2 server, it sends to the server its
`initial settings`. The settings define how the server can use the
connection, the default initial settings for a client are the default
values defined by the HTTP/2 RFC.

## Logging network client activity

For debugging purposes, network activity can be logged.

``` js
let options = new HttpClientOptions()
  .setLogActivity(true);
let client = vertx.createHttpClient(options);
```

See the chapter on [logging network activity](#logging_network_activity)
for a detailed explanation.

## Making requests

The http client is very flexible and there are various ways you can make
requests with it.

Often you want to make many requests to the same host/port with an http
client. To avoid you repeating the host/port every time you make a
request you can configure the client with a default host/port:

``` js
// Set the default host
let options = new HttpClientOptions()
  .setDefaultHost("wibble.com");
// Can also set default port if you want...
let client = vertx.createHttpClient(options);
client.getNow("/some-uri", (response) => {
  console.log("Received response with status code " + response.statusCode());
});
```

Alternatively if you find yourself making lots of requests to different
host/ports with the same client you can simply specify the host/port
when doing the request.

``` js
let client = vertx.createHttpClient();

// Specify both port and host name
client.getNow(8080, "myserver.mycompany.com", "/some-uri", (response) => {
  console.log("Received response with status code " + response.statusCode());
});

// This time use the default port 80 but specify the host name
client.getNow("foo.othercompany.com", "/other-uri", (response) => {
  console.log("Received response with status code " + response.statusCode());
});
```

Both methods of specifying host/port are supported for all the different
ways of making requests with the client.

### Simple requests with no request body

Often, you’ll want to make HTTP requests with no request body. This is
usually the case with HTTP GET, OPTIONS and HEAD requests.

The simplest way to do this with the Vert.x http client is using the
methods suffixed with `Now`. For example `getNow`.

These methods create the http request and send it in a single method
call and allow you to provide a handler that will be called with the
http response when it comes back.

``` js
let client = vertx.createHttpClient();

// Send a GET request
client.getNow("/some-uri", (response) => {
  console.log("Received response with status code " + response.statusCode());
});

// Send a GET request
client.headNow("/other-uri", (response) => {
  console.log("Received response with status code " + response.statusCode());
});
```

### Writing general requests

At other times you don’t know the request method you want to send until
run-time. For that use case we provide general purpose request methods
such as `request` which allow you to specify the HTTP method at
run-time:

``` js
let client = vertx.createHttpClient();

client.request(HttpMethod.GET, "some-uri", (response) => {
  console.log("Received response with status code " + response.statusCode());
}).end();

client.request(HttpMethod.POST, "foo-uri", (response) => {
  console.log("Received response with status code " + response.statusCode());
}).end("some-data");
```

### Writing request bodies

Sometimes you’ll want to write requests which have a body, or perhaps
you want to write headers to a request before sending it.

To do this you can call one of the specific request methods such as
`post` or one of the general purpose request methods such as `request`.

These methods don’t send the request immediately, but instead return an
instance of `HttpClientRequest` which can be used to write to the
request body or write headers.

Here are some examples of writing a POST request with a body: m

``` js
let client = vertx.createHttpClient();

let request = client.post("some-uri", (response) => {
  console.log("Received response with status code " + response.statusCode());
});

// Now do stuff with the request
request.putHeader("content-length", "1000");
request.putHeader("content-type", "text/plain");
request.write(body);

// Make sure the request is ended when you're done with it
request.end();

// Or fluently:

client.post("some-uri", (response) => {
  console.log("Received response with status code " + response.statusCode());
}).putHeader("content-length", "1000").putHeader("content-type", "text/plain").write(body).end();

// Or event more simply:

client.post("some-uri", (response) => {
  console.log("Received response with status code " + response.statusCode());
}).putHeader("content-type", "text/plain").end(body);
```

Methods exist to write strings in UTF-8 encoding and in any specific
encoding and to write buffers:

``` js
import { Buffer } from "@vertx/core"

// Write string encoded in UTF-8
request.write("some data");

// Write string encoded in specific encoding
request.write("some other data", "UTF-16");

// Write a buffer
let buffer = Buffer.buffer();
buffer.appendInt(123).appendLong(245);
request.write(buffer);
```

If you are just writing a single string or buffer to the HTTP request
you can write it and end the request in a single call to the `end`
function.

``` js
import { Buffer } from "@vertx/core"

// Write string and end the request (send it) in a single call
request.end("some simple data");

// Write buffer and end the request (send it) in a single call
let buffer = Buffer.buffer().appendDouble(12.34).appendLong(432);
request.end(buffer);
```

When you’re writing to a request, the first call to `write` will result
in the request headers being written out to the wire.

The actual write is asynchronous and might not occur until some time
after the call has returned.

Non-chunked HTTP requests with a request body require a `Content-Length`
header to be provided.

Consequently, if you are not using chunked HTTP then you must set the
`Content-Length` header before writing to the request, as it will be too
late otherwise.

If you are calling one of the `end` methods that take a string or buffer
then Vert.x will automatically calculate and set the `Content-Length`
header before writing the request body.

If you are using HTTP chunking a a `Content-Length` header is not
required, so you do not have to calculate the size up-front.

### Writing request headers

You can write headers to a request using the `headers` multi-map as
follows:

``` js
// Write some headers using the headers() multimap

let headers = request.headers();
headers.set("content-type", "application/json").set("other-header", "foo");
```

The headers are an instance of `MultiMap` which provides operations for
adding, setting and removing entries. Http headers allow more than one
value for a specific key.

You can also write headers using `putHeader`

``` js
// Write some headers using the putHeader method

request.putHeader("content-type", "application/json").putHeader("other-header", "foo");
```

If you wish to write headers to the request you must do so before any
part of the request body is written.

### Non standard HTTP methods

The `OTHER` HTTP method is used for non standard methods, when this
method is used, `setRawMethod` must be used to set the raw method to
send to the server.

### Ending HTTP requests

Once you have finished with the HTTP request you must end it with one of
the `end` operations.

Ending a request causes any headers to be written, if they have not
already been written and the request to be marked as complete.

Requests can be ended in several ways. With no arguments the request is
simply ended:

``` js
request.end();
```

Or a string or buffer can be provided in the call to `end`. This is like
calling `write` with the string or buffer before calling `end` with no
arguments

``` js
import { Buffer } from "@vertx/core"
// End the request with a string
request.end("some-data");

// End it with a buffer
let buffer = Buffer.buffer().appendFloat(12.3).appendInt(321);
request.end(buffer);
```

### Chunked HTTP requests

Vert.x supports [HTTP Chunked Transfer
Encoding](http://en.wikipedia.org/wiki/Chunked_transfer_encoding) for
requests.

This allows the HTTP request body to be written in chunks, and is
normally used when a large request body is being streamed to the server,
whose size is not known in advance.

You put the HTTP request into chunked mode using `setChunked`.

In chunked mode each call to write will cause a new chunk to be written
to the wire. In chunked mode there is no need to set the
`Content-Length` of the request up-front.

``` js
request.setChunked(true);

// Write some chunks
for (let i = 0;i < 10;i++) {
  request.write("this-is-chunk-" + i);
}

request.end();
```

### Request timeouts

You can set a timeout for a specific http request using `setTimeout`.

If the request does not return any data within the timeout period an
exception will be passed to the exception handler (if provided) and the
request will be closed.

### Handling exceptions

You can handle exceptions corresponding to a request by setting an
exception handler on the `HttpClientRequest` instance:

``` js
let request = client.post("some-uri", (response) => {
  console.log("Received response with status code " + response.statusCode());
});
request.exceptionHandler((e) => {
  console.log("Received exception: " + e.getMessage());
  e.printStackTrace();
});
```

This does not handle non *2xx* response that need to be handled in the
`HttpClientResponse` code:

``` js
let request = client.post("some-uri", (response) => {
  if (response.statusCode() === 200) {
    console.log("Everything fine");
    return
  }
  if (response.statusCode() === 500) {
    console.log("Unexpected behavior on the server side");
    return
  }
});
request.end();
```

> **Important**
> 
> `XXXNow` methods cannot receive an exception handler.

### Specifying a handler on the client request

Instead of providing a response handler in the call to create the client
request object, alternatively, you can not provide a handler when the
request is created and set it later on the request object itself, using
`handler`, for example:

``` js
let request = client.post("some-uri");
request.handler((response) => {
  console.log("Received response with status code " + response.statusCode());
});
```

### Using the request as a stream

The `HttpClientRequest` instance is also a `WriteStream` which means you
can pump to it from any `ReadStream` instance.

For, example, you could pump a file on disk to a http request body as
follows:

``` js
import { Pump } from "@vertx/core"

request.setChunked(true);
let pump = Pump.pump(file, request);
file.endHandler((v) => {
  request.end();
});
pump.start();
```

### Writing HTTP/2 frames

HTTP/2 is a framed protocol with various frames for the HTTP
request/response model. The protocol allows other kind of frames to be
sent and received.

To send such frames, you can use the `write` on the request. Here’s an
example:

``` js
import { Buffer } from "@vertx/core"

let frameType = 40;
let frameStatus = 10;
let payload = Buffer.buffer("some data");

// Sending a frame to the server
request.writeCustomFrame(frameType, frameStatus, payload);
```

### Stream reset

HTTP/1.x does not allow a clean reset of a request or a response stream,
for example when a client uploads a resource already present on the
server, the server needs to accept the entire response.

HTTP/2 supports stream reset at any time during the request/response:

``` js
request.reset();
```

By default the NO\_ERROR (0) error code is sent, another code can sent
instead:

``` js
request.reset(8);
```

The HTTP/2 specification defines the list of [error
codes](http://httpwg.org/specs/rfc7540.html#ErrorCodes) one can use.

The request handler are notified of stream reset events with the
`request handler` and `response handler`:

``` js
request.exceptionHandler((err) => {
  if (err instanceof StreamResetException) {
    let reset = err;
    console.log("Stream reset " + reset.getCode());
  }
});
```

## Handling HTTP responses

You receive an instance of `HttpClientResponse` into the handler that
you specify in of the request methods or by setting a handler directly
on the `HttpClientRequest` object.

You can query the status code and the status message of the response
with `statusCode` and `statusMessage`.

``` js
client.getNow("some-uri", (response) => {
  // the status code - e.g. 200 or 404
  console.log("Status code is " + response.statusCode());

  // the status message e.g. "OK" or "Not Found".
  console.log("Status message is " + response.statusMessage());
});
```

### Using the response as a stream

The `HttpClientResponse` instance is also a `ReadStream` which means you
can pipe it to any `WriteStream` instance.

### Response headers and trailers

Http responses can contain headers. Use `headers` to get the headers.

The object returned is a `MultiMap` as HTTP headers can contain multiple
values for single keys.

``` js
let contentType = response.headers().get("content-type");
let contentLength = response.headers().get("content-lengh");
```

Chunked HTTP responses can also contain trailers - these are sent in the
last chunk of the response body.

You use `trailers` to get the trailers. Trailers are also a `MultiMap`.

### Reading the request body

The response handler is called when the headers of the response have
been read from the wire.

If the response has a body this might arrive in several pieces some time
after the headers have been read. We don’t wait for all the body to
arrive before calling the response handler as the response could be very
large and we might be waiting a long time, or run out of memory for
large responses.

As parts of the response body arrive, the `handler` is called with a
`Buffer` representing the piece of the body:

``` js
client.getNow("some-uri", (response) => {

  response.handler((buffer) => {
    console.log("Received a part of the response body: " + buffer);
  });
});
```

If you know the response body is not very large and want to aggregate it
all in memory before handling it, you can either aggregate it yourself:

``` js
import { Buffer } from "@vertx/core"

client.getNow("some-uri", (response) => {

  // Create an empty buffer
  let totalBuffer = Buffer.buffer();

  response.handler((buffer) => {
    console.log("Received a part of the response body: " + buffer.length());

    totalBuffer.appendBuffer(buffer);
  });

  response.endHandler((v) => {
    // Now all the body has been read
    console.log("Total response body length is " + totalBuffer.length());
  });
});
```

Or you can use the convenience `bodyHandler` which is called with the
entire body when the response has been fully read:

``` js
client.getNow("some-uri", (response) => {

  response.bodyHandler((totalBuffer) => {
    // Now all the body has been read
    console.log("Total response body length is " + totalBuffer.length());
  });
});
```

### Response end handler

The response `endHandler` is called when the entire response body has
been read or immediately after the headers have been read and the
response handler has been called if there is no body.

### Reading cookies from the response

You can retrieve the list of cookies from a response using `cookies`.

Alternatively you can just parse the `Set-Cookie` headers yourself in
the response.

### 30x redirection handling

The client can be configured to follow HTTP redirections provided by the
`Location` response header when the client receives:

  - a `301`, `302`, `307` or `308` status code along with a HTTP GET or
    HEAD method

  - a `303` status code, in addition the directed request perform an
    HTTP GET methodn

Here’s an example:

``` js
client.get("some-uri", (response) => {
  console.log("Received response with status code " + response.statusCode());
}).setFollowRedirects(true).end();
```

The maximum redirects is `16` by default and can be changed with
`setMaxRedirects`.

``` js
let client = vertx.createHttpClient(new HttpClientOptions()
  .setMaxRedirects(32));

client.get("some-uri", (response) => {
  console.log("Received response with status code " + response.statusCode());
}).setFollowRedirects(true).end();
```

One size does not fit all and the default redirection policy may not be
adapted to your needs.

The default redirection policy can changed with a custom implementation:

``` js
import { Future } from "@vertx/core"

client.redirectHandler((response) => {

  // Only follow 301 code
  if (response.statusCode() === 301 && (response.getHeader("Location") !== null && response.getHeader("Location") !== undefined)) {

    // Compute the redirect URI
    let absoluteURI = resolveURI(response.request().absoluteURI(), response.getHeader("Location"));

    // Create a new ready to use request that the client will use
    return Future.succeededFuture(client.getAbs(absoluteURI))
  }

  // We don't redirect
  return null
});
```

The policy handles the original `HttpClientResponse` received and
returns either `null` or a `Future<HttpClientRequest>`.

  - when `null` is returned, the original response is processed

  - when a future is returned, the request will be sent on its
    successful completion

  - when a future is returned, the exception handler set on the request
    is called on its failure

The returned request must be unsent so the original request handlers can
be sent and the client can send it after.

Most of the original request settings will be propagated to the new
request:

  - request headers, unless if you have set some headers (including
    `setHost`)

  - request body unless the returned request uses a `GET` method

  - response handler

  - request exception handler

  - request timeout

### 100-Continue handling

According to the [HTTP 1.1
specification](http://www.w3.org/Protocols/rfc2616/rfc2616-sec8.html) a
client can set a header `Expect: 100-Continue` and send the request
header before sending the rest of the request body.

The server can then respond with an interim response status `Status: 100
(Continue)` to signify to the client that it is ok to send the rest of
the body.

The idea here is it allows the server to authorise and accept/reject the
request before large amounts of data are sent. Sending large amounts of
data if the request might not be accepted is a waste of bandwidth and
ties up the server in reading data that it will just discard.

Vert.x allows you to set a `continueHandler` on the client request
object

This will be called if the server sends back a `Status: 100 (Continue)`
response to signify that it is ok to send the rest of the request.

This is used in conjunction with
\`[sendHead](/es4x/@vertx/core/classes/httpclientrequest.html#sendhead)\`to
send the head of the request.

Here’s an example:

``` js
let request = client.put("some-uri", (response) => {
  console.log("Received response with status code " + response.statusCode());
});

request.putHeader("Expect", "100-Continue");

request.continueHandler((v) => {
  // OK to send rest of body
  request.write("Some data");
  request.write("Some more data");
  request.end();
});
```

On the server side a Vert.x http server can be configured to
automatically send back 100 Continue interim responses when it receives
an `Expect: 100-Continue` header.

This is done by setting the option `setHandle100ContinueAutomatically`.

If you’d prefer to decide whether to send back continue responses
manually, then this property should be set to `false` (the default),
then you can inspect the headers and call `writeContinue` to have the
client continue sending the body:

``` js
httpServer.requestHandler((request) => {
  if (request.getHeader("Expect").equalsIgnoreCase("100-Continue")) {

    // Send a 100 continue response
    request.response().writeContinue();

    // The client should send the body when it receives the 100 response
    request.bodyHandler((body) => {
      // Do something with body
    });

    request.endHandler((v) => {
      request.response().end();
    });
  }
});
```

You can also reject the request by sending back a failure status code
directly: in this case the body should either be ignored or the
connection should be closed (100-Continue is a performance hint and
cannot be a logical protocol constraint):

``` js
httpServer.requestHandler((request) => {
  if (request.getHeader("Expect").equalsIgnoreCase("100-Continue")) {

    //
    let rejectAndClose = true;
    if (rejectAndClose) {

      // Reject with a failure code and close the connection
      // this is probably best with persistent connection
      request.response().setStatusCode(405).putHeader("Connection", "close").end();
    } else {

      // Reject with a failure code and ignore the body
      // this may be appropriate if the body is small
      request.response().setStatusCode(405).end();
    }
  }
});
```

### Client push

Server push is a new feature of HTTP/2 that enables sending multiple
responses in parallel for a single client request.

A push handler can be set on a request to receive the request/response
pushed by the server:

``` js
let request = client.get("/index.html", (response) => {
  // Process index.html response
});

// Set a push handler to be aware of any resource pushed by the server
request.pushHandler((pushedRequest) => {

  // A resource is pushed for this request
  console.log("Server pushed " + pushedRequest.path());

  // Set an handler for the response
  pushedRequest.handler((pushedResponse) => {
    console.log("The response for the pushed request");
  });
});

// End the request
request.end();
```

If the client does not want to receive a pushed request, it can reset
the stream:

``` js
request.pushHandler((pushedRequest) => {
  if (pushedRequest.path() == "/main.js") {
    pushedRequest.reset();
  } else {
    // Handle it
  }
});
```

When no handler is set, any stream pushed will be automatically
cancelled by the client with a stream reset (`8` error code).

### Receiving custom HTTP/2 frames

HTTP/2 is a framed protocol with various frames for the HTTP
request/response model. The protocol allows other kind of frames to be
sent and received.

To receive custom frames, you can use the customFrameHandler on the
request, this will get called every time a custom frame arrives. Here’s
an example:

``` js
response.customFrameHandler((frame) => {

  console.log("Received a frame type=" + frame.type() + " payload" + frame.payload().toString());
});
```

## Enabling compression on the client

The http client comes with support for HTTP Compression out of the box.

This means the client can let the remote http server know that it
supports compression, and will be able to handle compressed response
bodies.

An http server is free to either compress with one of the supported
compression algorithms or to send the body back without compressing it
at all. So this is only a hint for the Http server which it may ignore
at will.

To tell the http server which compression is supported by the client it
will include an `Accept-Encoding` header with the supported compression
algorithm as value. Multiple compression algorithms are supported. In
case of Vert.x this will result in the following header added:

Accept-Encoding: gzip, deflate

The server will choose then from one of these. You can detect if a
server ompressed the body by checking for the `Content-Encoding` header
in the response sent back from it.

If the body of the response was compressed via gzip it will include for
example the following header:

Content-Encoding: gzip

To enable compression set `setTryUseCompression` on the options used
when creating the client.

By default compression is disabled.

## HTTP/1.x pooling and keep alive

Http keep alive allows http connections to be used for more than one
request. This can be a more efficient use of connections when you’re
making multiple requests to the same server.

For HTTP/1.x versions, the http client supports pooling of connections,
allowing you to reuse connections between requests.

For pooling to work, keep alive must be true using `setKeepAlive` on the
options used when configuring the client. The default value is true.

When keep alive is enabled. Vert.x will add a `Connection: Keep-Alive`
header to each HTTP/1.0 request sent. When keep alive is disabled.
Vert.x will add a `Connection: Close` header to each HTTP/1.1 request
sent to signal that the connection will be closed after completion of
the response.

The maximum number of connections to pool **for each server** is
configured using `setMaxPoolSize`

When making a request with pooling enabled, Vert.x will create a new
connection if there are less than the maximum number of connections
already created for that server, otherwise it will add the request to a
queue.

Keep alive connections will be closed by the client automatically after
a timeout. The timeout can be specified by the server using the
`keep-alive` header:

    keep-alive: timeout=30

You can set the default timeout using `setKeepAliveTimeout` - any
connections not used within this timeout will be closed. Please note the
timeout value is in seconds not milliseconds.

## HTTP/1.1 pipe-lining

The client also supports pipe-lining of requests on a connection.

Pipe-lining means another request is sent on the same connection before
the response from the preceding one has returned. Pipe-lining is not
appropriate for all requests.

To enable pipe-lining, it must be enabled using `setPipelining`. By
default pipe-lining is disabled.

When pipe-lining is enabled requests will be written to connections
without waiting for previous responses to return.

The number of pipe-lined requests over a single connection is limited by
`setPipeliningLimit`. This option defines the maximum number of http
requests sent to the server awaiting for a response. This limit ensures
the fairness of the distribution of the client requests over the
connections to the same server.

## HTTP/2 multiplexing

HTTP/2 advocates to use a single connection to a server, by default the
http client uses a single connection for each server, all the streams to
the same server are multiplexed over the same connection.

When the clients needs to use more than a single connection and use
pooling, the `setHttp2MaxPoolSize` shall be used.

When it is desirable to limit the number of multiplexed streams per
connection and use a connection pool instead of a single connection,
`setHttp2MultiplexingLimit` can be used.

``` js
let clientOptions = new HttpClientOptions()
  .setHttp2MultiplexingLimit(10)
  .setHttp2MaxPoolSize(3);

// Uses up to 3 connections and up to 10 streams per connection
let client = vertx.createHttpClient(clientOptions);
```

The multiplexing limit for a connection is a setting set on the client
that limits the number of streams of a single connection. The effective
value can be even lower if the server sets a lower limit with the
`SETTINGS_MAX_CONCURRENT_STREAMS` setting.

HTTP/2 connections will not be closed by the client automatically. To
close them you can call `close` or close the client instance.

Alternatively you can set idle timeout using `setIdleTimeout` - any
connections not used within this timeout will be closed. Please note the
idle timeout value is in seconds not milliseconds.

## HTTP connections

The `HttpConnection` offers the API for dealing with HTTP connection
events, lifecycle and settings.

HTTP/2 implements fully the `HttpConnection` API.

HTTP/1.x implements partially the `HttpConnection` API: only the close
operation, the close handler and exception handler are implemented. This
protocol does not provide semantics for the other operations.

### Server connections

The `connection` method returns the request connection on the server:

``` js
let connection = request.connection();
```

A connection handler can be set on the server to be notified of any
incoming connection:

``` js
let server = vertx.createHttpServer(http2Options);

server.connectionHandler((connection) => {
  console.log("A client connected");
});
```

### Client connections

The `connection` method returns the request connection on the client:

``` js
let connection = request.connection();
```

A connection handler can be set on the client to be notified when a
connection has been established happens:

``` js
client.connectionHandler((connection) => {
  console.log("Connected to the server");
});
```

### Connection settings

The configuration of an HTTP/2 is configured by the `Http2Settings` data
object.

Each endpoint must respect the settings sent by the other side of the
connection.

When a connection is established, the client and the server exchange
initial settings. Initial settings are configured by
`setInitialSettings` on the client and `setInitialSettings` on the
server.

The settings can be changed at any time after the connection is
established:

``` js
connection.updateSettings(new Http2Settings()
  .setMaxConcurrentStreams(100));
```

As the remote side should acknowledge on reception of the settings
update, it’s possible to give a callback to be notified of the
acknowledgment:

``` js
connection.updateSettings(new Http2Settings()
  .setMaxConcurrentStreams(100), (ar) => {
  if (ar.succeeded()) {
    console.log("The settings update has been acknowledged ");
  }
});
```

Conversely the `remoteSettingsHandler` is notified when the new remote
settings are received:

``` js
connection.remoteSettingsHandler((settings) => {
  console.log("Received new settings");
});
```

> **Note**
> 
> this only applies to the HTTP/2 protocol

### Connection ping

HTTP/2 connection ping is useful for determining the connection
round-trip time or check the connection validity: `ping` sends a
{@literal PING} frame to the remote endpoint:

``` js
import { Buffer } from "@vertx/core"
let data = Buffer.buffer();
for (let i = 0;i < 8;i++) {
  data.appendByte(i);
}
connection.ping(data, (pong) => {
  console.log("Remote side replied");
});
```

Vert.x will send automatically an acknowledgement when a {@literal PING}
frame is received, an handler can be set to be notified for each ping
received:

``` js
connection.pingHandler((ping) => {
  console.log("Got pinged by remote side");
});
```

The handler is just notified, the acknowledgement is sent whatsoever.
Such feature is aimed for implementing protocols on top of HTTP/2.

> **Note**
> 
> this only applies to the HTTP/2 protocol

### Connection shutdown and go away

Calling `shutdown` will send a {@literal GOAWAY} frame to the remote
side of the connection, asking it to stop creating streams: a client
will stop doing new requests and a server will stop pushing responses.
After the {@literal GOAWAY} frame is sent, the connection waits some
time (30 seconds by default) until all current streams closed and close
the connection:

``` js
connection.shutdown();
```

The `shutdownHandler` notifies when all streams have been closed, the
connection is not yet closed.

It’s possible to just send a {@literal GOAWAY} frame, the main
difference with a shutdown is that it will just tell the remote side of
the connection to stop creating new streams without scheduling a
connection close:

``` js
connection.goAway(0);
```

Conversely, it is also possible to be notified when {@literal GOAWAY}
are received:

``` js
connection.goAwayHandler((goAway) => {
  console.log("Received a go away frame");
});
```

The `shutdownHandler` will be called when all current streams have been
closed and the connection can be closed:

``` js
connection.goAway(0);
connection.shutdownHandler((v) => {

  // All streams are closed, close the connection
  connection.close();
});
```

This applies also when a {@literal GOAWAY} is received.

> **Note**
> 
> this only applies to the HTTP/2 protocol

### Connection close

Connection `close` closes the connection:

  - it closes the socket for HTTP/1.x

  - a shutdown with no delay for HTTP/2, the {@literal GOAWAY} frame
    will still be sent before the connection is closed. \*

The `closeHandler` notifies when a connection is closed.

## HttpClient usage

The HttpClient can be used in a Verticle or embedded.

When used in a Verticle, the Verticle **should use its own client
instance**.

More generally a client should not be shared between different Vert.x
contexts as it can lead to unexpected behavior.

For example a keep-alive connection will call the client handlers on the
context of the request that opened the connection, subsequent requests
will use the same context.

When this happen Vert.x detects it and log a warn:

    Reusing a connection with a different context: an HttpClient is probably shared between different Verticles

The HttpClient can be embedded in a non Vert.x thread like a unit test
or a plain java `main`: the client handlers will be called by different
Vert.x threads and contexts, such contexts are created as needed. For
production this usage is not recommended.

## Server sharing

When several HTTP servers listen on the same port, vert.x orchestrates
the request handling using a round-robin strategy.

Let’s take a verticle creating a HTTP server such as:

**io.vertx.examples.http.sharing.HttpServerVerticle.**

``` js
vertx.createHttpServer().requestHandler((request) => {
  request.response().end("Hello from server " + this);
}).listen(8080);
```

This service is listening on the port 8080. So, when this verticle is
instantiated multiple times as with: `vertx run
io.vertx.examples.http.sharing.HttpServerVerticle -instances 2`, what’s
happening ? If both verticles would bind to the same port, you would
receive a socket exception. Fortunately, vert.x is handling this case
for you. When you deploy another server on the same host and port as an
existing server it doesn’t actually try and create a new server
listening on the same host/port. It binds only once to the socket. When
receiving a request it calls the server handlers following a round robin
strategy.

Let’s now imagine a client such as:

``` js
vertx.setPeriodic(100, (l) => {
  vertx.createHttpClient().getNow(8080, "localhost", "/", (resp) => {
    resp.bodyHandler((body) => {
      console.log(body.toString("ISO-8859-1"));
    });
  });
});
```

Vert.x delegates the requests to one of the server sequentially:

    Hello from i.v.e.h.s.HttpServerVerticle@1
    Hello from i.v.e.h.s.HttpServerVerticle@2
    Hello from i.v.e.h.s.HttpServerVerticle@1
    Hello from i.v.e.h.s.HttpServerVerticle@2
    ...

Consequently the servers can scale over available cores while each
Vert.x verticle instance remains strictly single threaded, and you don’t
have to do any special tricks like writing load-balancers in order to
scale your server on your multi-core machine.

## Using HTTPS with Vert.x

Vert.x http servers and clients can be configured to use HTTPS in
exactly the same way as net servers.

Please see [configuring net servers to use SSL](#ssl) for more
information.

SSL can also be enabled/disabled per request with `RequestOptions` or
when specifying a scheme with `requestAbs` method.

``` js
client.getNow(new RequestOptions()
  .setHost("localhost")
  .setPort(8080)
  .setURI("/")
  .setSsl(true), (response) => {
  console.log("Received response with status code " + response.statusCode());
});
```

The `setSsl` setting acts as the default client setting.

The `setSsl` overrides the default client setting

  - setting the value to `false` will disable SSL/TLS even if the client
    is configured to use SSL/TLS

  - setting the value to `true` will enable SSL/TLS even if the client
    is configured to not use SSL/TLS, the actual client SSL/TLS (such as
    trust, key/certificate, ciphers, ALPN, …​) will be reused

Likewise `requestAbs` scheme also overrides the default client setting.

### Server Name Indication (SNI)

Vert.x http servers can be configured to use SNI in exactly the same way
as {@linkplain io.vertx.core.net net servers}.

Vert.x http client will present the actual hostname as *server name*
during the TLS handshake.

## WebSockets

[WebSockets](http://en.wikipedia.org/wiki/WebSocket) are a web
technology that allows a full duplex socket-like connection between HTTP
servers and HTTP clients (typically browsers).

Vert.x supports WebSockets on both the client and server-side.

### WebSockets on the server

There are two ways of handling WebSockets on the server side.

#### WebSocket handler

The first way involves providing a `webSocketHandler` on the server
instance.

When a WebSocket connection is made to the server, the handler will be
called, passing in an instance of `ServerWebSocket`.

``` js
server.webSocketHandler((webSocket) => {
  console.log("Connected!");
});
```

You can choose to reject the WebSocket by calling `reject`.

``` js
server.webSocketHandler((webSocket) => {
  if (webSocket.path() == "/myapi") {
    webSocket.reject();
  } else {
    // Do something
  }
});
```

You can perform an asynchronous handshake by calling `setHandshake` with
a `Future`:

``` js
import { Promise } from "@vertx/core"
server.webSocketHandler((webSocket) => {
  let promise = Promise.promise();
  webSocket.setHandshake(promise.future());
  authenticate(webSocket.headers(), (ar) => {
    if (ar.succeeded()) {
      // Terminate the handshake with the status code 101 (Switching Protocol)
      // Reject the handshake with 401 (Unauthorized)
      promise.complete(ar.succeeded() ? 101 : 401);
    } else {
      // Will send a 500 error
      promise.fail(ar.cause());
    }
  });
});
```

> **Note**
> 
> the WebSocket will be automatically accepted after the handler is
> called unless the WebSocket’s handshake has been set

#### Upgrading to WebSocket

The second way of handling WebSockets is to handle the HTTP Upgrade
request that was sent from the client, and call `upgrade` on the server
request.

``` js
server.requestHandler((request) => {
  if (request.path() == "/myapi") {

    let webSocket = request.upgrade();
    // Do something

  } else {
    // Reject
    request.response().setStatusCode(400).end();
  }
});
```

#### The server WebSocket

The `ServerWebSocket` instance enables you to retrieve the `headers`,
`path`, `query` and `URI` of the HTTP request of the WebSocket
handshake.

### WebSockets on the client

The Vert.x `HttpClient` supports WebSockets.

You can connect a WebSocket to a server using one of the `webSocket`
operations and providing a handler.

The handler will be called with an instance of `WebSocket` when the
connection has been made:

``` js
client.webSocket("/some-uri", (res) => {
  if (res.succeeded()) {
    let ws = res.result();
    console.log("Connected!");
  }
});
```

### Writing messages to WebSockets

If you wish to write a single WebSocket message to the WebSocket you can
do this with `writeBinaryMessage` or `writeTextMessage` :

``` js
import { Buffer } from "@vertx/core"
// Write a simple binary message
let buffer = Buffer.buffer().appendInt(123).appendFloat(1.23);
webSocket.writeBinaryMessage(buffer);

// Write a simple text message
let message = "hello";
webSocket.writeTextMessage(message);
```

If the WebSocket message is larger than the maximum WebSocket frame size
as configured with `setMaxWebSocketFrameSize` then Vert.x will split it
into multiple WebSocket frames before sending it on the wire.

### Writing frames to WebSockets

A WebSocket message can be composed of multiple frames. In this case the
first frame is either a *binary* or *text* frame followed by zero or
more *continuation* frames.

The last frame in the message is marked as *final*.

To send a message consisting of multiple frames you create frames using
`WebSocketFrame.binaryFrame` , `WebSocketFrame.textFrame` or
`WebSocketFrame.continuationFrame` and write them to the WebSocket using
`writeFrame`.

Here’s an example for binary frames:

``` js
import { WebSocketFrame } from "@vertx/core"

let frame1 = WebSocketFrame.binaryFrame(buffer1, false);
webSocket.writeFrame(frame1);

let frame2 = WebSocketFrame.continuationFrame(buffer2, false);
webSocket.writeFrame(frame2);

// Write the final frame
let frame3 = WebSocketFrame.continuationFrame(buffer2, true);
webSocket.writeFrame(frame3);
```

In many cases you just want to send a WebSocket message that consists of
a single final frame, so we provide a couple of shortcut methods to do
that with `writeFinalBinaryFrame` and `writeFinalTextFrame`.

Here’s an example:

``` js
import { Buffer } from "@vertx/core"

// Send a WebSocket messages consisting of a single final text frame:

webSocket.writeFinalTextFrame("Geronimo!");

// Send a WebSocket messages consisting of a single final binary frame:

let buff = Buffer.buffer().appendInt(12).appendString("foo");

webSocket.writeFinalBinaryFrame(buff);
```

### Reading frames from WebSockets

To read frames from a WebSocket you use the `frameHandler`.

The frame handler will be called with instances of `WebSocketFrame` when
a frame arrives, for example:

``` js
webSocket.frameHandler((frame) => {
  console.log("Received a frame of size!");
});
```

### Closing WebSockets

Use `close` to close the WebSocket connection when you have finished
with it.

### Piping WebSockets

The `WebSocket` instance is also a `ReadStream` and a `WriteStream` so
it can be used with pipes.

When using a WebSocket as a write stream or a read stream it can only be
used with WebSockets connections that are used with binary frames that
are no split over multiple frames.

### Event bus handlers

Every WebSocket automatically registers two handler on the event bus,
and when any data are received in this handler, it writes them to
itself. Those are local subscriptions not routed on the cluster.

This enables you to write data to a WebSocket which is potentially in a
completely different verticle sending data to the address of that
handler.

The addresses of the handlers are given by `binaryHandlerID` and
`textHandlerID`.

## Using a proxy for HTTP/HTTPS connections

The http client supports accessing http/https URLs via a HTTP proxy
(e.g. Squid) or *SOCKS4a* or *SOCKS5* proxy. The CONNECT protocol uses
HTTP/1.x but can connect to HTTP/1.x and HTTP/2 servers.

Connecting to h2c (unencrypted HTTP/2 servers) is likely not supported
by http proxies since they will support HTTP/1.1 only.

The proxy can be configured in the `HttpClientOptions` by setting a
`ProxyOptions` object containing proxy type, hostname, port and
optionally username and password.

Here’s an example of using an HTTP proxy:

``` js
let options = new HttpClientOptions()
  .setProxyOptions(new ProxyOptions()
    .setType("HTTP")
    .setHost("localhost")
    .setPort(3128)
    .setUsername("username")
    .setPassword("secret"));
let client = vertx.createHttpClient(options);
```

When the client connects to an http URL, it connects to the proxy server
and provides the full URL in the HTTP request ("GET
<http://www.somehost.com/path/file.html> HTTP/1.1").

When the client connects to an https URL, it asks the proxy to create a
tunnel to the remote host with the CONNECT method.

For a SOCKS5 proxy:

``` js
let options = new HttpClientOptions()
  .setProxyOptions(new ProxyOptions()
    .setType("SOCKS5")
    .setHost("localhost")
    .setPort(1080)
    .setUsername("username")
    .setPassword("secret"));
let client = vertx.createHttpClient(options);
```

The DNS resolution is always done on the proxy server, to achieve the
functionality of a SOCKS4 client, it is necessary to resolve the DNS
address locally.

### Handling of other protocols

The HTTP proxy implementation supports getting ftp:// urls if the proxy
supports that, which isn’t available in non-proxy getAbs requests.

``` js
let options = new HttpClientOptions()
  .setProxyOptions(new ProxyOptions()
    .setType("HTTP"));
let client = vertx.createHttpClient(options);
client.getAbs("ftp://ftp.gnu.org/gnu/", (response) => {
  console.log("Received response with status code " + response.statusCode());
});
```

Support for other protocols is not available since java.net.URL does not
support them (gopher:// for example).

## Automatic clean-up in verticles

If you’re creating http servers and clients from inside verticles, those
servers and clients will be automatically closed when the verticle is
undeployed.

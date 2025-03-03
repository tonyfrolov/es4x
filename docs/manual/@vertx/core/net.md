# Writing TCP servers and clients

Vert.x allows you to easily write non blocking TCP clients and servers.

## Creating a TCP server

The simplest way to create a TCP server, using all default options is as
follows:

``` js
let server = vertx.createNetServer();
```

## Configuring a TCP server

If you don’t want the default, a server can be configured by passing in
a `NetServerOptions` instance when creating it:

``` js
let options = new NetServerOptions()
  .setPort(4321);
let server = vertx.createNetServer(options);
```

## Start the Server Listening

To tell the server to listen for incoming requests you use one of the
`listen` alternatives.

To tell the server to listen at the host and port as specified in the
options:

``` js
let server = vertx.createNetServer();
server.listen();
```

Or to specify the host and port in the call to listen, ignoring what is
configured in the options:

``` js
let server = vertx.createNetServer();
server.listen(1234, "localhost");
```

The default host is `0.0.0.0` which means 'listen on all available
addresses' and the default port is `0`, which is a special value that
instructs the server to find a random unused local port and use that.

The actual bind is asynchronous so the server might not actually be
listening until some time **after** the call to listen has returned.

If you want to be notified when the server is actually listening you can
provide a handler to the `listen` call. For example:

``` js
let server = vertx.createNetServer();
server.listen(1234, "localhost", (res) => {
  if (res.succeeded()) {
    console.log("Server is now listening!");
  } else {
    console.log("Failed to bind!");
  }
});
```

## Listening on a random port

If `0` is used as the listening port, the server will find an unused
random port to listen on.

To find out the real port the server is listening on you can call
`actualPort`.

``` js
let server = vertx.createNetServer();
server.listen(0, "localhost", (res) => {
  if (res.succeeded()) {
    console.log("Server is now listening on actual port: " + server.actualPort());
  } else {
    console.log("Failed to bind!");
  }
});
```

## Getting notified of incoming connections

To be notified when a connection is made you need to set a
`connectHandler`:

``` js
let server = vertx.createNetServer();
server.connectHandler((socket) => {
  // Handle the connection in here
});
```

When a connection is made the handler will be called with an instance of
`NetSocket`.

This is a socket-like interface to the actual connection, and allows you
to read and write data as well as do various other things like close the
socket.

## Reading data from the socket

To read data from the socket you set the `handler` on the socket.

This handler will be called with an instance of `Buffer` every time data
is received on the socket.

``` js
let server = vertx.createNetServer();
server.connectHandler((socket) => {
  socket.handler((buffer) => {
    console.log("I received some bytes: " + buffer.length());
  });
});
```

## Writing data to a socket

You write to a socket using one of `write`.

``` js
import { Buffer } from "@vertx/core"

// Write a buffer
let buffer = Buffer.buffer().appendFloat(12.34).appendInt(123);
socket.write(buffer);

// Write a string in UTF-8 encoding
socket.write("some data");

// Write a string using the specified encoding
socket.write("some data", "UTF-16");
```

Write operations are asynchronous and may not occur until some time
after the call to write has returned.

## Closed handler

If you want to be notified when a socket is closed, you can set a
`closeHandler` on it:

``` js
socket.closeHandler((v) => {
  console.log("The socket has been closed");
});
```

## Handling exceptions

You can set an `exceptionHandler` to receive any exceptions that happen
on the socket.

You can set an `exceptionHandler` to receive any exceptions that happens
before the connection is passed to the `connectHandler` , e.g during the
TLS handshake.

## Event bus write handler

Every socket automatically registers a handler on the event bus, and
when any buffers are received in this handler, it writes them to itself.
Those are local subscriptions not routed on the cluster.

This enables you to write data to a socket which is potentially in a
completely different verticle by sending the buffer to the address of
that handler.

The address of the handler is given by `writeHandlerID`

## Local and remote addresses

The local address of a `NetSocket` can be retrieved using
`localAddress`.

The remote address, (i.e. the address of the other end of the
connection) of a `NetSocket` can be retrieved using `remoteAddress`.

## Sending files or resources from the classpath

Files and classpath resources can be written to the socket directly
using `sendFile`. This can be a very efficient way to send files, as it
can be handled by the OS kernel directly where supported by the
operating system.

Please see the chapter about [serving files from the
classpath](#classpath) for restrictions of the classpath resolution or
disabling it.

``` js
socket.sendFile("myfile.dat");
```

## Streaming sockets

Instances of `NetSocket` are also `ReadStream` and `WriteStream`
instances so they can be used to pipe data to or from other read and
write streams.

See the chapter on [streams](#streams) for more information.

## Upgrading connections to SSL/TLS

A non SSL/TLS connection can be upgraded to SSL/TLS using
`upgradeToSsl`.

The server or client must be configured for SSL/TLS for this to work
correctly. Please see the [chapter on SSL/TLS](#ssl) for more
information.

## Closing a TCP Server

Call `close` to close the server. Closing the server closes any open
connections and releases all server resources.

The close is actually asynchronous and might not complete until some
time after the call has returned. If you want to be notified when the
actual close has completed then you can pass in a handler.

This handler will then be called when the close has fully completed.

``` js
server.close((res) => {
  if (res.succeeded()) {
    console.log("Server is now closed");
  } else {
    console.log("close failed");
  }
});
```

## Automatic clean-up in verticles

If you’re creating TCP servers and clients from inside verticles, those
servers and clients will be automatically closed when the verticle is
undeployed.

## Scaling - sharing TCP servers

The handlers of any TCP server are always executed on the same event
loop thread.

This means that if you are running on a server with a lot of cores, and
you only have this one instance deployed then you will have at most one
core utilised on your server.

In order to utilise more cores of your server you will need to deploy
more instances of the server.

You can instantiate more instances programmatically in your code:

``` js
// Create a few instances so we can utilise cores

for (let i = 0;i < 10;i++) {
  let server = vertx.createNetServer();
  server.connectHandler((socket) => {
    socket.handler((buffer) => {
      // Just echo back the data
      socket.write(buffer);
    });
  });
  server.listen(1234, "localhost");
}
```

or, if you are using verticles you can simply deploy more instances of
your server verticle by using the `-instances` option on the command
line:

vertx run com.mycompany.MyVerticle -instances 10

or when programmatically deploying your verticle

``` js
let options = new DeploymentOptions()
  .setInstances(10);
vertx.deployVerticle("com.mycompany.MyVerticle", options);
```

Once you do this you will find the echo server works functionally
identically to before, but all your cores on your server can be utilised
and more work can be handled.

At this point you might be asking yourself **'How can you have more than
one server listening on the same host and port? Surely you will get port
conflicts as soon as you try and deploy more than one instance?'**

*Vert.x does a little magic here.\**

When you deploy another server on the same host and port as an existing
server it doesn’t actually try and create a new server listening on the
same host/port.

Instead it internally maintains just a single server, and, as incoming
connections arrive it distributes them in a round-robin fashion to any
of the connect handlers.

Consequently Vert.x TCP servers can scale over available cores while
each instance remains single threaded.

## Creating a TCP client

The simplest way to create a TCP client, using all default options is as
follows:

``` js
let client = vertx.createNetClient();
```

## Configuring a TCP client

If you don’t want the default, a client can be configured by passing in
a `NetClientOptions` instance when creating it:

``` js
let options = new NetClientOptions()
  .setConnectTimeout(10000);
let client = vertx.createNetClient(options);
```

## Making connections

To make a connection to a server you use `connect`, specifying the port
and host of the server and a handler that will be called with a result
containing the `NetSocket` when connection is successful or with a
failure if connection failed.

``` js
let options = new NetClientOptions()
  .setConnectTimeout(10000);
let client = vertx.createNetClient(options);
client.connect(4321, "localhost", (res) => {
  if (res.succeeded()) {
    console.log("Connected!");
    let socket = res.result();
  } else {
    console.log("Failed to connect: " + res.cause().getMessage());
  }
});
```

## Configuring connection attempts

A client can be configured to automatically retry connecting to the
server in the event that it cannot connect. This is configured with
`setReconnectInterval` and `setReconnectAttempts`.

> **Note**
> 
> Currently Vert.x will not attempt to reconnect if a connection fails,
> reconnect attempts and interval only apply to creating initial
> connections.

``` js
let options = new NetClientOptions()
  .setReconnectAttempts(10)
  .setReconnectInterval(500);

let client = vertx.createNetClient(options);
```

By default, multiple connection attempts are disabled.

## Logging network activity

For debugging purposes, network activity can be logged:

``` js
let options = new NetServerOptions()
  .setLogActivity(true);

let server = vertx.createNetServer(options);
```

for the client

``` js
let options = new NetClientOptions()
  .setLogActivity(true);

let client = vertx.createNetClient(options);
```

Network activity is logged by Netty with the `DEBUG` level and with the
`io.netty.handler.logging.LoggingHandler` name. When using network
activity logging there are a few things to keep in mind:

  - logging is not performed by Vert.x logging but by Netty

  - this is **not** a production feature

You should read the [???](#netty-logging) section.

## Configuring servers and clients to work with SSL/TLS

TCP clients and servers can be configured to use [Transport Layer
Security](http://en.wikipedia.org/wiki/Transport_Layer_Security) -
earlier versions of TLS were known as SSL.

The APIs of the servers and clients are identical whether or not SSL/TLS
is used, and it’s enabled by configuring the `NetClientOptions` or
`NetServerOptions` instances used to create the servers or clients.

### Enabling SSL/TLS on the server

SSL/TLS is enabled with `ssl`.

By default it is disabled.

### Specifying key/certificate for the server

SSL/TLS servers usually provide certificates to clients in order verify
their identity to clients.

Certificates/keys can be configured for servers in several ways:

The first method is by specifying the location of a Java key-store which
contains the certificate and private key.

Java key stores can be managed with the
[keytool](http://docs.oracle.com/javase/6/docs/technotes/tools/solaris/keytool.html)
utility which ships with the JDK.

The password for the key store should also be provided:

``` js
let options = new NetServerOptions()
  .setSsl(true)
  .setKeyStoreOptions(new JksOptions()
    .setPath("/path/to/your/server-keystore.jks")
    .setPassword("password-of-your-keystore"));
let server = vertx.createNetServer(options);
```

Alternatively you can read the key store yourself as a buffer and
provide that directly:

``` js
let myKeyStoreAsABuffer = vertx.fileSystem().readFileBlocking("/path/to/your/server-keystore.jks");
let jksOptions = new JksOptions()
  .setValue(myKeyStoreAsABuffer)
  .setPassword("password-of-your-keystore");
let options = new NetServerOptions()
  .setSsl(true)
  .setKeyStoreOptions(jksOptions);
let server = vertx.createNetServer(options);
```

Key/certificate in PKCS\#12 format
(<http://en.wikipedia.org/wiki/PKCS_12>), usually with the `.pfx` or the
`.p12` extension can also be loaded in a similar fashion than JKS key
stores:

``` js
let options = new NetServerOptions()
  .setSsl(true)
  .setPfxKeyCertOptions(new PfxOptions()
    .setPath("/path/to/your/server-keystore.pfx")
    .setPassword("password-of-your-keystore"));
let server = vertx.createNetServer(options);
```

Buffer configuration is also supported:

``` js
let myKeyStoreAsABuffer = vertx.fileSystem().readFileBlocking("/path/to/your/server-keystore.pfx");
let pfxOptions = new PfxOptions()
  .setValue(myKeyStoreAsABuffer)
  .setPassword("password-of-your-keystore");
let options = new NetServerOptions()
  .setSsl(true)
  .setPfxKeyCertOptions(pfxOptions);
let server = vertx.createNetServer(options);
```

Another way of providing server private key and certificate separately
using `.pem` files.

``` js
let options = new NetServerOptions()
  .setSsl(true)
  .setPemKeyCertOptions(new PemKeyCertOptions()
    .setKeyPath("/path/to/your/server-key.pem")
    .setCertPath("/path/to/your/server-cert.pem"));
let server = vertx.createNetServer(options);
```

Buffer configuration is also supported:

``` js
let myKeyAsABuffer = vertx.fileSystem().readFileBlocking("/path/to/your/server-key.pem");
let myCertAsABuffer = vertx.fileSystem().readFileBlocking("/path/to/your/server-cert.pem");
let pemOptions = new PemKeyCertOptions()
  .setKeyValue(myKeyAsABuffer)
  .setCertValue(myCertAsABuffer);
let options = new NetServerOptions()
  .setSsl(true)
  .setPemKeyCertOptions(pemOptions);
let server = vertx.createNetServer(options);
```

Vert.x supports reading of unencrypted RSA and/or ECC based private keys
from PKCS8 PEM files. RSA based private keys can also be read from PKCS1
PEM files. X.509 certificates can be read from PEM files containing a
textual encoding of the certificate as defined by [RFC 7468,
Section 5](https://tools.ietf.org/html/rfc7468#section-5).

> **Warning**
> 
> Keep in mind that the keys contained in an unencrypted PKCS8 or a
> PKCS1 PEM file can be extracted by anybody who can read the file.
> Thus, make sure to put proper access restrictions on such PEM files in
> order to prevent misuse.

### Specifying trust for the server

SSL/TLS servers can use a certificate authority in order to verify the
identity of the clients.

Certificate authorities can be configured for servers in several ways:

Java trust stores can be managed with the
[keytool](http://docs.oracle.com/javase/6/docs/technotes/tools/solaris/keytool.html)
utility which ships with the JDK.

The password for the trust store should also be provided:

``` js
let options = new NetServerOptions()
  .setSsl(true)
  .setClientAuth("REQUIRED")
  .setTrustStoreOptions(new JksOptions()
    .setPath("/path/to/your/truststore.jks")
    .setPassword("password-of-your-truststore"));
let server = vertx.createNetServer(options);
```

Alternatively you can read the trust store yourself as a buffer and
provide that directly:

``` js
let myTrustStoreAsABuffer = vertx.fileSystem().readFileBlocking("/path/to/your/truststore.jks");
let options = new NetServerOptions()
  .setSsl(true)
  .setClientAuth("REQUIRED")
  .setTrustStoreOptions(new JksOptions()
    .setValue(myTrustStoreAsABuffer)
    .setPassword("password-of-your-truststore"));
let server = vertx.createNetServer(options);
```

Certificate authority in PKCS\#12 format
(<http://en.wikipedia.org/wiki/PKCS_12>), usually with the `.pfx` or the
`.p12` extension can also be loaded in a similar fashion than JKS trust
stores:

``` js
let options = new NetServerOptions()
  .setSsl(true)
  .setClientAuth("REQUIRED")
  .setPfxTrustOptions(new PfxOptions()
    .setPath("/path/to/your/truststore.pfx")
    .setPassword("password-of-your-truststore"));
let server = vertx.createNetServer(options);
```

Buffer configuration is also supported:

``` js
let myTrustStoreAsABuffer = vertx.fileSystem().readFileBlocking("/path/to/your/truststore.pfx");
let options = new NetServerOptions()
  .setSsl(true)
  .setClientAuth("REQUIRED")
  .setPfxTrustOptions(new PfxOptions()
    .setValue(myTrustStoreAsABuffer)
    .setPassword("password-of-your-truststore"));
let server = vertx.createNetServer(options);
```

Another way of providing server certificate authority using a list
`.pem` files.

``` js
let options = new NetServerOptions()
  .setSsl(true)
  .setClientAuth("REQUIRED")
  .setPemTrustOptions(new PemTrustOptions()
    .setCertPaths(["/path/to/your/server-ca.pem"]));
let server = vertx.createNetServer(options);
```

Buffer configuration is also supported:

``` js
let myCaAsABuffer = vertx.fileSystem().readFileBlocking("/path/to/your/server-ca.pfx");
let options = new NetServerOptions()
  .setSsl(true)
  .setClientAuth("REQUIRED")
  .setPemTrustOptions(new PemTrustOptions()
    .setCertValues([myCaAsABuffer]));
let server = vertx.createNetServer(options);
```

### Enabling SSL/TLS on the client

Net Clients can also be easily configured to use SSL. They have the
exact same API when using SSL as when using standard sockets.

To enable SSL on a NetClient the function setSSL(true) is called.

### Client trust configuration

If the `trustALl` is set to true on the client, then the client will
trust all server certificates. The connection will still be encrypted
but this mode is vulnerable to 'man in the middle' attacks. I.e. you
can’t be sure who you are connecting to. Use this with caution.
Default value is false.

``` js
let options = new NetClientOptions()
  .setSsl(true)
  .setTrustAll(true);
let client = vertx.createNetClient(options);
```

If `trustAll` is not set then a client trust store must be configured
and should contain the certificates of the servers that the client
trusts.

By default, host verification is disabled on the client. To enable host
verification, set the algorithm to use on your client (only HTTPS and
LDAPS is currently supported):

``` js
let options = new NetClientOptions()
  .setSsl(true)
  .setHostnameVerificationAlgorithm("HTTPS");
let client = vertx.createNetClient(options);
```

Likewise server configuration, the client trust can be configured in
several ways:

The first method is by specifying the location of a Java trust-store
which contains the certificate authority.

It is just a standard Java key store, the same as the key stores on the
server side. The client trust store location is set by using the
function `path` on the `jks options`. If a server presents a certificate
during connection which is not in the client trust store, the connection
attempt will not succeed.

``` js
let options = new NetClientOptions()
  .setSsl(true)
  .setTrustStoreOptions(new JksOptions()
    .setPath("/path/to/your/truststore.jks")
    .setPassword("password-of-your-truststore"));
let client = vertx.createNetClient(options);
```

Buffer configuration is also supported:

``` js
let myTrustStoreAsABuffer = vertx.fileSystem().readFileBlocking("/path/to/your/truststore.jks");
let options = new NetClientOptions()
  .setSsl(true)
  .setTrustStoreOptions(new JksOptions()
    .setValue(myTrustStoreAsABuffer)
    .setPassword("password-of-your-truststore"));
let client = vertx.createNetClient(options);
```

Certificate authority in PKCS\#12 format
(<http://en.wikipedia.org/wiki/PKCS_12>), usually with the `.pfx` or the
`.p12` extension can also be loaded in a similar fashion than JKS trust
stores:

``` js
let options = new NetClientOptions()
  .setSsl(true)
  .setPfxTrustOptions(new PfxOptions()
    .setPath("/path/to/your/truststore.pfx")
    .setPassword("password-of-your-truststore"));
let client = vertx.createNetClient(options);
```

Buffer configuration is also supported:

``` js
let myTrustStoreAsABuffer = vertx.fileSystem().readFileBlocking("/path/to/your/truststore.pfx");
let options = new NetClientOptions()
  .setSsl(true)
  .setPfxTrustOptions(new PfxOptions()
    .setValue(myTrustStoreAsABuffer)
    .setPassword("password-of-your-truststore"));
let client = vertx.createNetClient(options);
```

Another way of providing server certificate authority using a list
`.pem` files.

``` js
let options = new NetClientOptions()
  .setSsl(true)
  .setPemTrustOptions(new PemTrustOptions()
    .setCertPaths(["/path/to/your/ca-cert.pem"]));
let client = vertx.createNetClient(options);
```

Buffer configuration is also supported:

``` js
let myTrustStoreAsABuffer = vertx.fileSystem().readFileBlocking("/path/to/your/ca-cert.pem");
let options = new NetClientOptions()
  .setSsl(true)
  .setPemTrustOptions(new PemTrustOptions()
    .setCertValues([myTrustStoreAsABuffer]));
let client = vertx.createNetClient(options);
```

### Specifying key/certificate for the client

If the server requires client authentication then the client must
present its own certificate to the server when connecting. The client
can be configured in several ways:

The first method is by specifying the location of a Java key-store which
contains the key and certificate. Again it’s just a regular Java key
store. The client keystore location is set by using the function `path`
on the `jks options`.

``` js
let options = new NetClientOptions()
  .setSsl(true)
  .setKeyStoreOptions(new JksOptions()
    .setPath("/path/to/your/client-keystore.jks")
    .setPassword("password-of-your-keystore"));
let client = vertx.createNetClient(options);
```

Buffer configuration is also supported:

``` js
let myKeyStoreAsABuffer = vertx.fileSystem().readFileBlocking("/path/to/your/client-keystore.jks");
let jksOptions = new JksOptions()
  .setValue(myKeyStoreAsABuffer)
  .setPassword("password-of-your-keystore");
let options = new NetClientOptions()
  .setSsl(true)
  .setKeyStoreOptions(jksOptions);
let client = vertx.createNetClient(options);
```

Key/certificate in PKCS\#12 format
(<http://en.wikipedia.org/wiki/PKCS_12>), usually with the `.pfx` or the
`.p12` extension can also be loaded in a similar fashion than JKS key
stores:

``` js
let options = new NetClientOptions()
  .setSsl(true)
  .setPfxKeyCertOptions(new PfxOptions()
    .setPath("/path/to/your/client-keystore.pfx")
    .setPassword("password-of-your-keystore"));
let client = vertx.createNetClient(options);
```

Buffer configuration is also supported:

``` js
let myKeyStoreAsABuffer = vertx.fileSystem().readFileBlocking("/path/to/your/client-keystore.pfx");
let pfxOptions = new PfxOptions()
  .setValue(myKeyStoreAsABuffer)
  .setPassword("password-of-your-keystore");
let options = new NetClientOptions()
  .setSsl(true)
  .setPfxKeyCertOptions(pfxOptions);
let client = vertx.createNetClient(options);
```

Another way of providing server private key and certificate separately
using `.pem` files.

``` js
let options = new NetClientOptions()
  .setSsl(true)
  .setPemKeyCertOptions(new PemKeyCertOptions()
    .setKeyPath("/path/to/your/client-key.pem")
    .setCertPath("/path/to/your/client-cert.pem"));
let client = vertx.createNetClient(options);
```

Buffer configuration is also supported:

``` js
let myKeyAsABuffer = vertx.fileSystem().readFileBlocking("/path/to/your/client-key.pem");
let myCertAsABuffer = vertx.fileSystem().readFileBlocking("/path/to/your/client-cert.pem");
let pemOptions = new PemKeyCertOptions()
  .setKeyValue(myKeyAsABuffer)
  .setCertValue(myCertAsABuffer);
let options = new NetClientOptions()
  .setSsl(true)
  .setPemKeyCertOptions(pemOptions);
let client = vertx.createNetClient(options);
```

Keep in mind that pem configuration, the private key is not crypted.

### Self-signed certificates for testing and development purposes

> **Caution**
> 
> Do not use this in production settings, and note that the generated
> keys are very insecure.

It is very often the case that self-signed certificates are required, be
it for unit / integration tests or for running a development version of
an application.

`SelfSignedCertificate` can be used to provide self-signed PEM
certificate helpers and give `KeyCertOptions` and `TrustOptions`
configurations:

``` js
import { SelfSignedCertificate } from "@vertx/core"
let certificate = SelfSignedCertificate.create();

let serverOptions = new NetServerOptions()
  .setSsl(true)
  .setKeyCertOptions(certificate.keyCertOptions())
  .setTrustOptions(certificate.trustOptions());

let server = vertx.createNetServer(serverOptions).connectHandler((socket) => {
  socket.write("Hello!").end();
}).listen(1234, "localhost");

let clientOptions = new NetClientOptions()
  .setSsl(true)
  .setKeyCertOptions(certificate.keyCertOptions())
  .setTrustOptions(certificate.trustOptions());

let client = vertx.createNetClient(clientOptions);
client.connect(1234, "localhost", (ar) => {
  if (ar.succeeded()) {
    ar.result().handler((buffer) => {
      console.log(buffer);
    });
  } else {
    console.error("Woops: " + ar.cause().getMessage());
  }
});
```

The client can also be configured to trust all certificates:

``` js
let clientOptions = new NetClientOptions()
  .setSsl(true)
  .setTrustAll(true);
```

Note that self-signed certificates also work for other TCP protocols
like HTTPS:

``` js
import { SelfSignedCertificate } from "@vertx/core"
let certificate = SelfSignedCertificate.create();

vertx.createHttpServer(new HttpServerOptions()
  .setSsl(true)
  .setKeyCertOptions(certificate.keyCertOptions())
  .setTrustOptions(certificate.trustOptions())).requestHandler((req) => {
  req.response().end("Hello!");
}).listen(8080);
```

### Revoking certificate authorities

Trust can be configured to use a certificate revocation list (CRL) for
revoked certificates that should no longer be trusted. The `crlPath`
configures the crl list to use:

``` js
let options = new NetClientOptions()
  .setSsl(true)
  .setTrustStoreOptions(trustOptions)
  .setCrlPaths(["/path/to/your/crl.pem"]);
let client = vertx.createNetClient(options);
```

Buffer configuration is also supported:

``` js
let myCrlAsABuffer = vertx.fileSystem().readFileBlocking("/path/to/your/crl.pem");
let options = new NetClientOptions()
  .setSsl(true)
  .setTrustStoreOptions(trustOptions)
  .setCrlValues([myCrlAsABuffer]);
let client = vertx.createNetClient(options);
```

### Configuring the Cipher suite

By default, the TLS configuration will use the Cipher suite of the JVM
running Vert.x. This Cipher suite can be configured with a suite of
enabled ciphers:

``` js
let options = new NetServerOptions()
  .setSsl(true)
  .setKeyStoreOptions(keyStoreOptions)
  .setEnabledCipherSuites(["ECDHE-RSA-AES128-GCM-SHA256", "ECDHE-ECDSA-AES128-GCM-SHA256", "ECDHE-RSA-AES256-GCM-SHA384", "CDHE-ECDSA-AES256-GCM-SHA384"]);
let server = vertx.createNetServer(options);
```

Cipher suite can be specified on the `NetServerOptions` or
`NetClientOptions` configuration.

### Configuring TLS protocol versions

By default, the TLS configuration will use the following protocol
versions: SSLv2Hello, TLSv1, TLSv1.1 and TLSv1.2. Protocol versions can
be configured by explicitly adding enabled protocols:

``` js
Code not translatable
```

Protocol versions can be specified on the `NetServerOptions` or
`NetClientOptions` configuration.

### SSL engine

The engine implementation can be configured to use
[OpenSSL](https://www.openssl.org) instead of the JDK implementation.
OpenSSL provides better performances and CPU usage than the JDK engine,
as well as JDK version independence.

The engine options to use is

  - the `getSslEngineOptions` options when it is set

  - otherwise `JdkSSLEngineOptions`

<!-- end list -->

``` js
// Use JDK SSL engine
let options = new NetServerOptions()
  .setSsl(true)
  .setKeyStoreOptions(keyStoreOptions);

// Use JDK SSL engine explicitly
options = new NetServerOptions()
  .setSsl(true)
  .setKeyStoreOptions(keyStoreOptions)
  .setJdkSslEngineOptions(new JdkSSLEngineOptions());

// Use OpenSSL engine
options = new NetServerOptions()
  .setSsl(true)
  .setKeyStoreOptions(keyStoreOptions)
  .setOpenSslEngineOptions(new OpenSSLEngineOptions());
```

### Server Name Indication (SNI)

Server Name Indication (SNI) is a TLS extension by which a client
specifies a hostname attempting to connect: during the TLS handshake the
client gives a server name and the server can use it to respond with a
specific certificate for this server name instead of the default
deployed certificate. If the server requires client authentication the
server can use a specific trusted CA certificate depending on the
indicated server name.

When SNI is active the server uses

  - the certificate CN or SAN DNS (Subject Alternative Name with DNS) to
    do an exact match, e.g `www.example.com`

  - the certificate CN or SAN DNS certificate to match a wildcard name,
    e.g `*.example.com`

  - otherwise the first certificate when the client does not present a
    server name or the presented server name cannot be matched

When the server additionally requires client authentication:

  - if `JksOptions` were used to set the trust options (`options`) then
    an exact match with the trust store alias is done

  - otherwise the available CA certificates are used in the same way as
    if no SNI is in place

You can enable SNI on the server by setting `setSni` to `true` and
configured the server with multiple key/certificate pairs.

Java KeyStore files or PKCS12 files can store multiple key/cert pairs
out of the box.

``` js
let keyCertOptions = new JksOptions()
  .setPath("keystore.jks")
  .setPassword("wibble");

let netServer = vertx.createNetServer(new NetServerOptions()
  .setKeyStoreOptions(keyCertOptions)
  .setSsl(true)
  .setSni(true));
```

`PemKeyCertOptions` can be configured to hold multiple entries:

``` js
let keyCertOptions = new PemKeyCertOptions()
  .setKeyPaths(["default-key.pem", "host1-key.pem", "etc..."])
  .setCertPaths(["default-cert.pem", "host2-key.pem", "etc..."]);

let netServer = vertx.createNetServer(new NetServerOptions()
  .setPemKeyCertOptions(keyCertOptions)
  .setSsl(true)
  .setSni(true));
```

The client implicitly sends the connecting host as an SNI server name
for Fully Qualified Domain Name (FQDN).

You can provide an explicit server name when connecting a socket

``` js
let client = vertx.createNetClient(new NetClientOptions()
  .setTrustStoreOptions(trustOptions)
  .setSsl(true));

// Connect to 'localhost' and present 'server.name' server name
client.connect(1234, "localhost", "server.name", (res) => {
  if (res.succeeded()) {
    console.log("Connected!");
    let socket = res.result();
  } else {
    console.log("Failed to connect: " + res.cause().getMessage());
  }
});
```

It can be used for different purposes:

  - present a server name different than the server host

  - present a server name while connecting to an IP

  - force to present a server name when using shortname

### Application-Layer Protocol Negotiation (ALPN)

Application-Layer Protocol Negotiation (ALPN) is a TLS extension for
application layer protocol negotiation. It is used by HTTP/2: during the
TLS handshake the client gives the list of application protocols it
accepts and the server responds with a protocol it supports.

If you are using Java 9, you are fine and you can use HTTP/2 out of the
box without extra steps.

Java 8 does not supports ALPN out of the box, so ALPN should be enabled
by other means:

  - *OpenSSL* support

  - *Jetty-ALPN* support

The engine options to use is

  - the `getSslEngineOptions` options when it is set

  - `JdkSSLEngineOptions` when ALPN is available for JDK

  - `OpenSSLEngineOptions` when ALPN is available for OpenSSL

  - otherwise it fails

#### OpenSSL ALPN support

OpenSSL provides native ALPN support.

OpenSSL requires to configure `setOpenSslEngineOptions` and use
[netty-tcnative](http://netty.io/wiki/forked-tomcat-native.html) jar on
the classpath. Using tcnative may require OpenSSL to be installed on
your OS depending on the tcnative implementation.

#### Jetty-ALPN support

Jetty-ALPN is a small jar that overrides a few classes of Java 8
distribution to support ALPN.

The JVM must be started with the *alpn-boot-${version}.jar* in its
`bootclasspath`:

    -Xbootclasspath/p:/path/to/alpn-boot${version}.jar

where ${version} depends on the JVM version, e.g. *8.1.7.v20160121* for
*OpenJDK 1.8.0u74* . The complete list is available on the [Jetty-ALPN
page](http://www.eclipse.org/jetty/documentation/current/alpn-chapter.html).

The main drawback is that the version depends on the JVM.

To solve this problem the *[Jetty ALPN
agent](https://github.com/jetty-project/jetty-alpn-agent)* can be use
instead. The agent is a JVM agent that will chose the correct ALPN
version for the JVM running it:

    -javaagent:/path/to/alpn/agent

## Using a proxy for client connections

The `NetClient` supports either a HTTP/1.x *CONNECT*, *SOCKS4a* or
*SOCKS5* proxy.

The proxy can be configured in the `NetClientOptions` by setting a
`ProxyOptions` object containing proxy type, hostname, port and
optionally username and password.

Here’s an example:

``` js
let options = new NetClientOptions()
  .setProxyOptions(new ProxyOptions()
    .setType("SOCKS5")
    .setHost("localhost")
    .setPort(1080)
    .setUsername("username")
    .setPassword("secret"));
let client = vertx.createNetClient(options);
```

The DNS resolution is always done on the proxy server, to achieve the
functionality of a SOCKS4 client, it is necessary to resolve the DNS
address locally.

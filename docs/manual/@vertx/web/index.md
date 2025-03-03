Vert.x-Web is a set of building blocks for building web applications
with Vert.x. Think of it as a Swiss Army Knife for building modern,
scalable, web apps.

Vert.x core provides a fairly low level set of functionality for
handling HTTP, and for some applications that will be sufficient.

Vert.x-Web builds on Vert.x core to provide a richer set of
functionality for building real web applications, more easily.

It’s the successor to [Yoke](http://pmlopes.github.io/yoke/) in Vert.x
2.x, and takes inspiration from projects such as
[Express](http://expressjs.com/) in the Node.js world and
[Sinatra](http://www.sinatrarb.com/) in the Ruby world.

Vert.x-Web is designed to be powerful, un-opionated and fully
embeddable. You just use the parts you want and nothing more. Vert.x-Web
is not a container.

You can use Vert.x-Web to create classic server-side web applications,
RESTful web applications, 'real-time' (server push) web applications, or
any other kind of web application you can think of. Vert.x-Web doesn’t
care. It’s up to you to chose the type of app you prefer, not
Vert.x-Web.

Vert.x-Web is a great fit for writingRESTful HTTP micro-services\*, but
we don’tforce\* you to write apps like that.

Some of the key features of Vert.x-Web include:

  - Routing (based on method, path, etc)

  - Regular expression pattern matching for paths

  - Extraction of parameters from paths

  - Content negotiation

  - Request body handling

  - Body size limits

  - Cookie parsing and handling

  - Multipart forms

  - Multipart file uploads

  - Sub routers

  - Session support - both local (for sticky sessions) and clustered
    (for non sticky)

  - CORS (Cross Origin Resource Sharing) support

  - Error page handler

  - Basic Authentication

  - Redirect based authentication

  - Authorisation handlers

  - JWT based authorization

  - User/role/permission authorisation

  - Favicon handling

  - Template support for server side rendering, including support for
    the following template engines out of the box:

      - Handlebars

      - Jade,

      - MVEL

      - Thymeleaf

      - Apache FreeMarker

      - Pebble

      - Rocker

  - Response time handler

  - Static file serving, including caching logic and directory listing.

  - Request timeout support

  - SockJS support

  - Event-bus bridge

  - CSRF Cross Site Request Forgery

  - VirtualHost

Most features in Vert.x-Web are implemented as handlers so you can
always write your own. We envisage many more being written over time.

We’ll discuss all these features in this manual.

# Using Vert.x Web

To use vert.x web, add the following dependency to the *dependencies*
section of your build descriptor:

  - Maven (in your `pom.xml`):

<!-- end list -->

``` xml
<dependency>
 <groupId>io.vertx</groupId>
 <artifactId>vertx-web</artifactId>
 <version>${maven.version}</version>
</dependency>
```

  - Gradle (in your `build.gradle` file):

<!-- end list -->

``` groovy
dependencies {
 compile 'io.vertx:vertx-web:${maven.version}'
}
```

## Development mode

Vert.x Web by default operates in production mode. You can switch the
development mode by assigning the `dev` value to either:

  - the `VERTXWEB_ENVIRONMENT` environment variable, or

  - the `vertxweb.environment` system property

In development mode:

  - template engine caches are disabled

  - the `ErrorHandler` does not display exception details

  - the `StaticHandler` does not handle cache headers

  - the GraphiQL development tool is disabled

# Re-cap on Vert.x core HTTP servers

Vert.x-Web uses and exposes the API from Vert.x core, so it’s well worth
getting familiar with the basic concepts of writing HTTP servers using
Vert.x core, if you’re not already.

The Vert.x core HTTP documentation goes into a lot of detail on this.

Here’s a hello world web server written using Vert.x core. At this point
there is no Vert.x-Web involved:

``` java
let server = vertx.createHttpServer();

server.requestHandler((request) => {

  // This handler gets called for each request that arrives on the server
  let response = request.response();
  response.putHeader("content-type", "text/plain");

  // Write to the response and end it
  response.end("Hello World!");
});

server.listen(8080);
```

We create an HTTP server instance, and we set a request handler on it.
The request handler will be called whenever a request arrives on the
server.

When that happens we are just going to set the content type to
`text/plain`, and write `Hello World!` and end the response.

We then tell the server to listen at port `8080` (default host is
`localhost`).

You can run this, and point your browser at `http://localhost:8080` to
verify that it works as expected.

# Basic Vert.x-Web concepts

Here’s the 10000 foot view:

A `Router` is one of the core concepts of Vert.x-Web. It’s an object
which maintains zero or more `Routes` .

A router takes an HTTP request and finds the first matching route for
that request, and passes the request to that route.

The route can have a *handler* associated with it, which then receives
the request. You then *do something* with the request, and then, either
end it or pass it to the next matching handler.

Here’s a simple router example:

``` js
import { Router } from "@vertx/web"
let server = vertx.createHttpServer();

let router = Router.router(vertx);

router.route().handler((routingContext) => {

  // This handler will be called for every request
  let response = routingContext.response();
  response.putHeader("content-type", "text/plain");

  // Write to the response and end it
  response.end("Hello World from Vert.x-Web!");
});

server.requestHandler(router).listen(8080);
```

It basically does the same thing as the Vert.x Core HTTP server hello
world example from the previous section, but this time using Vert.x-Web.

We create an HTTP server as before, then we create a router. Once we’ve
done that we create a simple route with no matching criteria so it will
match *all* requests that arrive on the server.

We then specify a handler for that route. That handler will be called
for all requests that arrive on the server.

The object that gets passed into the handler is a `RoutingContext` -
this contains the standard Vert.x `HttpServerRequest` and
`HttpServerResponse` but also various other useful stuff that makes
working with Vert.x-Web simpler.

For every request that is routed there is a unique routing context
instance, and the same instance is passed to all handlers for that
request.

Once we’ve set up the handler, we set the request handler of the HTTP
server to pass all incoming requests to `handle`.

So, that’s the basics. Now we’ll look at things in more detail:

# Handling requests and calling the next handler

When Vert.x-Web decides to route a request to a matching route, it calls
the handler of the route passing in an instance of `RoutingContext`. A
route can have different handlers, that you can append using `handler`

If you don’t end the response in your handler, you should call `next` so
another matching route can handle the request (if any).

You don’t have to call `next` before the handler has finished executing.
You can do this some time later, if you want:

``` js
let route = router.route("/some/path/");
route.handler((routingContext) => {

  let response = routingContext.response();
  // enable chunked responses because we will be adding data as
  // we execute over other handlers. This is only required once and
  // only if several handlers do output.
  response.setChunked(true);

  response.write("route1\n");

  // Call the next matching route after a 5 second delay
  routingContext.vertx().setTimer(5000, (tid) => {
    routingContext.next();
  });
});

route.handler((routingContext) => {

  let response = routingContext.response();
  response.write("route2\n");

  // Call the next matching route after a 5 second delay
  routingContext.vertx().setTimer(5000, (tid) => {
    routingContext.next();
  });
});

route.handler((routingContext) => {

  let response = routingContext.response();
  response.write("route3");

  // Now end the response
  routingContext.response().end();
});
```

In the above example `route1` is written to the response, then 5 seconds
later `route2` is written to the response, then 5 seconds later `route3`
is written to the response and the response is ended.

Note, all this happens without any thread blocking.

# Using blocking handlers

Sometimes, you might have to do something in a handler that might block
the event loop for some time, e.g. call a legacy blocking API or do some
intensive calculation.

You can’t do that in a normal handler, so we provide the ability to set
blocking handlers on a route.

A blocking handler looks just like a normal handler but it’s called by
Vert.x using a thread from the worker pool not using an event loop.

You set a blocking handler on a route with `blockingHandler`. Here’s an
example:

``` js
router.route().blockingHandler((routingContext) => {

  // Do something that might take some time synchronously
  service.doSomethingThatBlocks();

  // Now call the next handler
  routingContext.next();

});
```

By default, any blocking handlers executed on the same context (e.g. the
same verticle instance) are *ordered* - this means the next one won’t be
executed until the previous one has completed. If you don’t care about
orderering and don’t mind your blocking handlers executing in parallel
you can set the blocking handler specifying `ordered` as false using
`blockingHandler`.

Note, if you need to process multipart form data from a blocking
handler, you MUST use a non-blocking handler FIRST in order to call
`setExpectMultipart(true)`. Here is an example:

``` js
router.post("/some/endpoint").handler((ctx) => {
  ctx.request().setExpectMultipart(true);
  ctx.next();
}).blockingHandler((ctx) => {
  // ... Do some blocking operation
});
```

# Routing by exact path

A route can be set-up to match the path from the request URI. In this
case it will match any request which has a path that’s the same as the
specified path.

In the following example the handler will be called for a request
`/some/path/`. We also ignore trailing slashes so it will be called for
paths `/some/path` and `/some/path//` too:

``` js
let route = router.route().path("/some/path/");

route.handler((routingContext) => {
  // This handler will be called for the following request paths:

  // `/some/path`
  // `/some/path/`
  // `/some/path//`
  //
  // but not:
  // `/some/path/subdir`
});
```

# Routing by paths that begin with something

Often you want to route all requests that begin with a certain path. You
could use a regex to do this, but a simply way is to use an asterisk `*`
at the end of the path when declaring the route path.

In the following example the handler will be called for any request with
a URI path that starts with `/some/path/`.

For example `/some/path/foo.html` and `/some/path/otherdir/blah.css`
would both match.

``` js
let route = router.route().path("/some/path/*");

route.handler((routingContext) => {
  // This handler will be called for any path that starts with
  // `/some/path/`, e.g.

  // `/some/path`
  // `/some/path/`
  // `/some/path/subdir`
  // `/some/path/subdir/blah.html`
  //
  // but not:
  // `/some/bath`
});
```

With any path it can also be specified when creating the route:

``` js
let route = router.route("/some/path/*");

route.handler((routingContext) => {
  // This handler will be called same as previous example
});
```

# Capturing path parameters

It’s possible to match paths using placeholders for parameters which are
then available in the request `params`.

Here’s an example

``` js
let route = router.route(HttpMethod.POST, "/catalogue/products/:producttype/:productid/");

route.handler((routingContext) => {

  let productType = routingContext.request().getParam("producttype");
  let productID = routingContext.request().getParam("productid");

  // Do something with them...
});
```

The placeholders consist of `:` followed by the parameter name.
Parameter names consist of any alphabetic character, numeric character
or underscore.

In the above example, if a POST request is made to path:
`/catalogue/products/tools/drill123/` then the route will match and
`productType` will receive the value `tools` and productID will receive
the value `drill123`.

# Routing with regular expressions

Regular expressions can also be used to match URI paths in routes.

``` js
// Matches any path ending with 'foo'
let route = router.route().pathRegex(".*foo");

route.handler((routingContext) => {

  // This handler will be called for:

  // /some/path/foo
  // /foo
  // /foo/bar/wibble/foo
  // /bar/foo

  // But not:
  // /bar/wibble
});
```

Alternatively the regex can be specified when creating the route:

``` js
let route = router.routeWithRegex(".*foo");

route.handler((routingContext) => {

  // This handler will be called same as previous example

});
```

# Capturing path parameters with regular expressions

You can also capture path parameters when using regular expressions,
here’s an example:

``` js
let route = router.routeWithRegex(".*foo");

// This regular expression matches paths that start with something like:
// "/foo/bar" - where the "foo" is captured into param0 and the "bar" is captured into
// param1
route.pathRegex("\\/([^\\/]+)\\/([^\\/]+)").handler((routingContext) => {

  let productType = routingContext.request().getParam("param0");
  let productID = routingContext.request().getParam("param1");

  // Do something with them...
});
```

In the above example, if a request is made to path: `/tools/drill123/`
then the route will match and `productType` will receive the value
`tools` and productID will receive the value `drill123`.

Captures are denoted in regular expressions with capture groups (i.e.
surrounding the capture with round brackets)

# Using named capture groups

Using int index param names might be troublesome in some cases. It’s
possible to use named capture groups in the regex path.

``` js
// This regular expression matches paths that start with something like: "/foo/bar"
// It uses named regex groups to capture path params
let route = router.routeWithRegex("\\/(?<productType>[^\\/]+)\\/(?<productId>[^\\/]+)").handler((routingContext) => {

  let productType = routingContext.request().getParam("productType");
  let productID = routingContext.request().getParam("productId");

  // Do something with them...
});
```

In the example above, named capture groups are mapped to path parameters
of the same name as the group.

Additionally, you can still access group parameters as you would with
normal groups (i.e. `params0, params1…​`)

# Routing by HTTP method

By default a route will match all HTTP methods.

If you want a route to only match for a specific HTTP method you can use
`method`

``` js
let route = router.route().method(HttpMethod.POST);

route.handler((routingContext) => {

  // This handler will be called for any POST request

});
```

Or you can specify this with a path when creating the route:

``` js
let route = router.route(HttpMethod.POST, "/some/path/");

route.handler((routingContext) => {

  // This handler will be called for any POST request to a URI path starting with /some/path/

});
```

If you want to route for a specific HTTP method you can also use the
methods such as `get`, `post` and `put` named after the HTTP method
name. For example:

``` js
router.get().handler((routingContext) => {

  // Will be called for any GET request

});

router.get("/some/path/").handler((routingContext) => {

  // Will be called for any GET request to a path
  // starting with /some/path

});

router.getWithRegex(".*foo").handler((routingContext) => {

  // Will be called for any GET request to a path
  // ending with `foo`

});

// There are also equivalents to the above for PUT, POST, DELETE, HEAD and OPTIONS
```

If you want to specify a route will match for more than HTTP method you
can call `method` multiple times:

``` js
let route = router.route().method(HttpMethod.POST).method(HttpMethod.PUT);

route.handler((routingContext) => {

  // This handler will be called for any POST or PUT request

});
```

# Route order

By default routes are matched in the order they are added to the router.

When a request arrives the router will step through each route and check
if it matches, if it matches then the handler for that route will be
called.

If the handler subsequently calls `next` the handler for the next
matching route (if any) will be called. And so on.

Here’s an example to illustrate this:

``` js
let route1 = router.route("/some/path/").handler((routingContext) => {

  let response = routingContext.response();
  // enable chunked responses because we will be adding data as
  // we execute over other handlers. This is only required once and
  // only if several handlers do output.
  response.setChunked(true);

  response.write("route1\n");

  // Now call the next matching route
  routingContext.next();
});

let route2 = router.route("/some/path/").handler((routingContext) => {

  let response = routingContext.response();
  response.write("route2\n");

  // Now call the next matching route
  routingContext.next();
});

let route3 = router.route("/some/path/").handler((routingContext) => {

  let response = routingContext.response();
  response.write("route3");

  // Now end the response
  routingContext.response().end();
});
```

In the above example the response will contain:

    route1
    route2
    route3

As the routes have been called in that order for any request that starts
with `/some/path`.

If you want to override the default ordering for routes, you can do so
using `order`, specifying an integer value.

Routes are assigned an order at creation time corresponding to the order
in which they were added to the router, with the first route numbered
`0`, the second route numbered `1`, and so on.

By specifying an order for the route you can override the default
ordering. Order can also be negative, e.g. if you want to ensure a route
is evaluated before route number `0`.

Let’s change the ordering of route2 so it runs before route1:

``` js
let route1 = router.route("/some/path/").order(1).handler((routingContext) => {

  let response = routingContext.response();
  response.write("route1\n");

  // Now call the next matching route
  routingContext.next();
});

let route2 = router.route("/some/path/").order(0).handler((routingContext) => {

  let response = routingContext.response();
  // enable chunked responses because we will be adding data as
  // we execute over other handlers. This is only required once and
  // only if several handlers do output.
  response.setChunked(true);

  response.write("route2\n");

  // Now call the next matching route
  routingContext.next();
});

let route3 = router.route("/some/path/").order(2).handler((routingContext) => {

  let response = routingContext.response();
  response.write("route3");

  // Now end the response
  routingContext.response().end();
});
```

then the response will now contain:

    route2
    route1
    route3

If two matching routes have the same value of order, then they will be
called in the order they were added.

You can also specify that a route is handled last, with `last`

Note: Route order can be specified only before you configure an
handler\!

# Routing based on MIME type of request

You can specify that a route will match against matching request MIME
types using `consumes`.

In this case, the request will contain a `content-type` header
specifying the MIME type of the request body. This will be matched
against the value specified in `consumes`.

Basically, `consumes` is describing which MIME types the handler can
*consume*.

Matching can be done on exact MIME type matches:

``` js
// Exact match
router.route().consumes("text/html").handler((routingContext) => {

  // This handler will be called for any request with
  // content-type header set to `text/html`

});
```

Multiple exact matches can also be specified:

``` js
// Multiple exact matches
router.route().consumes("text/html").consumes("text/plain").handler((routingContext) => {

  // This handler will be called for any request with
  // content-type header set to `text/html` or `text/plain`.

});
```

Matching on wildcards for the sub-type is supported:

``` js
// Sub-type wildcard match
router.route().consumes("text/*").handler((routingContext) => {

  // This handler will be called for any request with top level type `text`
  // e.g. content-type header set to `text/html` or `text/plain` will both match

});
```

And you can also match on the top level type

``` js
// Top level type wildcard match
router.route().consumes("*/json").handler((routingContext) => {

  // This handler will be called for any request with sub-type json
  // e.g. content-type header set to `text/json` or `application/json` will both match

});
```

If you don’t specify a `/` in the consumers, it will assume you meant
the sub-type.

# Routing based on MIME types acceptable by the client

The HTTP `accept` header is used to signify which MIME types of the
response are acceptable to the client.

An `accept` header can have multiple MIME types separated by “,”.

MIME types can also have a `q` value appended to them\* which signifies
a weighting to apply if more than one response MIME type is available
matching the accept header. The q value is a number between 0 and 1.0.
If omitted it defaults to 1.0.

For example, the following `accept` header signifies the client will
accept a MIME type of only `text/plain`:

Accept: text/plain

With the following the client will accept `text/plain` or `text/html`
with no preference.

Accept: text/plain, text/html

With the following the client will accept `text/plain` or `text/html`
but prefers `text/html` as it has a higher `q` value (the default value
is q=1.0)

Accept: text/plain; q=0.9, text/html

If the server can provide both text/plain and text/html it should
provide the text/html in this case.

By using `produces` you define which MIME type(s) the route produces,
e.g. the following handler produces a response with MIME type
`application/json`.

``` java
router.route().produces("application/json").handler((routingContext) => {

  let response = routingContext.response();
  response.putHeader("content-type", "application/json");
  response.write(someJSON).end();

});
```

In this case the route will match with any request with an `accept`
header that matches `application/json`.

Here are some examples of `accept` headers that will match:

Accept: application/json Accept: application/\* Accept:
application/json, text/html Accept: application/json;q=0.7,
text/html;q=0.8, text/plain

You can also mark your route as producing more than one MIME type. If
this is the case, then you use `getAcceptableContentType` to find out
the actual MIME type that was accepted.

``` js
// This route can produce two different MIME types
router.route().produces("application/json").produces("text/html").handler((routingContext) => {

  let response = routingContext.response();

  // Get the actual MIME type acceptable
  let acceptableContentType = routingContext.getAcceptableContentType();

  response.putHeader("content-type", acceptableContentType);
  response.write(whatever).end();
});
```

In the above example, if you sent a request with the following `accept`
header:

Accept: application/json; q=0.7, text/html

Then the route would match and `acceptableContentType` would contain
`text/html` as both are acceptable but that has a higher `q` value.

# Combining routing criteria

You can combine all the above routing criteria in many different ways,
for example:

``` js
let route = router.route(HttpMethod.PUT, "myapi/orders").consumes("application/json").produces("application/json");

route.handler((routingContext) => {

  // This would be match for any PUT method to paths starting with "myapi/orders" with a
  // content-type of "application/json"
  // and an accept header matching "application/json"

});
```

# Enabling and disabling routes

You can disable a route with `disable`. A disabled route will be ignored
when matching.

You can re-enable a disabled route with `enable`

# Context data

You can use the context data in the `RoutingContext` to maintain any
data that you want to share between handlers for the lifetime of the
request.

Here’s an example where one handler sets some data in the context data
and a subsequent handler retrieves it:

You can use the `put` to put any object, and `get` to retrieve any
object from the context data.

A request sent to path `/some/path/other` will match both routes.

``` js
router.get("/some/path").handler((routingContext) => {

  routingContext.put("foo", "bar");
  routingContext.next();

});

router.get("/some/path/other").handler((routingContext) => {

  let bar = routingContext.get("foo");
  // Do something with bar
  routingContext.response().end();

});
```

# Reroute

Until now all routing mechanism allow you to handle your requests in a
sequential way, however there might be times where you will want to go
back. Since the context does not expose any information about the
previous or next handler, mostly because this information is dynamic
there is a way to restart the whole routing from the start of the
current Router.

``` js
router.get("/some/path").handler((routingContext) => {

  routingContext.put("foo", "bar");
  routingContext.next();

});

router.get("/some/path/B").handler((routingContext) => {
  routingContext.response().end();
});

router.get("/some/path").handler((routingContext) => {
  routingContext.reroute("/some/path/B");
});
```

So from the code you can see that if a request arrives at `/some/path`
if first add a value to the context, then moves to the next handler that
re routes the request to `/some/path/B` which terminates the request.

You can reroute based on a new path or based on a new path and method.
Note however that rerouting based on method might introduce security
issues since for example a usually safe GET request can become a DELETE.

Reroute is also allowed on the failure handler, however due to the
nature of re router when called the current status code and failure
reason are reset. In order the rerouted handler should generate the
correct status code if needed, for example:

``` js
router.get("/my-pretty-notfound-handler").handler((ctx) => {
  ctx.response().setStatusCode(404).end("NOT FOUND fancy html here!!!");
});

router.get().failureHandler((ctx) => {
  if (ctx.statusCode() === 404) {
    ctx.reroute("/my-pretty-notfound-handler");
  } else {
    ctx.next();
  }
});
```

It should be clear that reroute works on `paths`, so if you need to
preserve and or add state across reroutes, one should use the
`RoutingContext` object. For example you want to reroute to a new path
with a extra parameter:

``` js
router.get("/final-target").handler((ctx) => {
  // continue from here...
});

// THE WRONG WAY! (Will reroute to /final-target excluding the query string)
router.get().handler((ctx) => {
  ctx.reroute("/final-target?variable=value");
});

// THE CORRECT WAY!
router.get().handler((ctx) => {
  ctx.put("variable", "value").reroute("/final-target");
});
```

Even though the wrong reroute path will warn you that the query string
is ignored, the reroute will happen since the implementation will strip
any query string or html fragment from the path.

# Sub-routers

Sometimes if you have a lot of handlers it can make sense to split them
up into multiple routers. This is also useful if you want to reuse a set
of handlers in a different application, rooted at a different path root.

To do this you can mount a router at a *mount point* in another router.
The router that is mounted is called a *sub-router*. Sub routers can
mount other sub routers so you can have several levels of sub-routers if
you like.

Let’s look at a simple example of a sub-router mounted with another
router.

This sub-router will maintain the set of handlers that corresponds to a
simple fictional REST API. We will mount that on another router. The
full implementation of the REST API is not shown.

Here’s the sub-router:

``` js
import { Router } from "@vertx/web"

let restAPI = Router.router(vertx);

restAPI.get("/products/:productID").handler((rc) => {

  // TODO Handle the lookup of the product....
  rc.response().write(productJSON);

});

restAPI.put("/products/:productID").handler((rc) => {

  // TODO Add a new product...
  rc.response().end();

});

restAPI.delete("/products/:productID").handler((rc) => {

  // TODO delete the product...
  rc.response().end();

});
```

If this router was used as a top level router, then GET/PUT/DELETE
requests to urls like `/products/product1234` would invoke the API.

However, let’s say we already have a web-site as described by another
router:

``` js
import { Router } from "@vertx/web"
let mainRouter = Router.router(vertx);

// Handle static resources
mainRouter.route("/static/*").handler(myStaticHandler);

mainRouter.route(".*\\.templ").handler(myTemplateHandler);
```

We can now mount the sub router on the main router, against a mount
point, in this case `/productsAPI`

``` js
mainRouter.mountSubRouter("/productsAPI", restAPI);
```

This means the REST API is now accessible via paths like:
`/productsAPI/products/product1234`

# Localization

Vert.x Web parses the `Accept-Language` header and provides some helper
methods to identify which is the preferred locale for a client or the
sorted list of preferred locales by quality.

``` js
let route = router.get("/localized").handler((rc) => {
  // although it might seem strange by running a loop with a switch we
  // make sure that the locale order of preference is preserved when
  // replying in the users language.
  rc.acceptableLanguages().forEach(language => {
    return
  });
  // we do not know the user language so lets just inform that back:
  rc.response().end("Sorry we don't speak: " + rc.preferredLanguage());
});
```

The main method `acceptableLocales` will return the ordered list of
locales the user understands, if you’re only interested in the user
prefered locale then the helper: `preferredLocale` will return the 1st
element of the list or `null` if no locale was provided by the user.

# Route match failures

If no routes match for any particular request, Vert.x-Web will signal an
error depending on match failure:

  - 404 If no route matches the path

  - 405 If a route matches the path but don’t match the HTTP Method

  - 406 If a route matches the path and the method but It can’t provide
    a response with a content type matching `Accept` header

  - 415 If a route matches the path and the method but It can’t accept
    the `Content-type`

  - 400 If a route matches the path and the method but It can’t accept
    an empty body

You can manually manage those failures using `errorHandler`

# Error handling

As well as setting handlers to handle requests you can also set handlers
to handle failures in routing.

Failure handlers are used with the exact same route matching criteria
that you use with normal handlers.

For example you can provide a failure handler that will only handle
failures on certain paths, or for certain HTTP methods.

This allows you to set different failure handlers for different parts of
your application.

Here’s an example failure handler that will only be called for failure
that occur when routing to GET requests to paths that start with
`/somepath/`:

``` js
let route = router.get("/somepath/*");

route.failureHandler((frc) => {

  // This will be called for failures that occur
  // when routing requests to paths starting with
  // '/somepath/'

});
```

Failure routing will occur if a handler throws an exception, or if a
handler calls `fail` specifying an HTTP status code to deliberately
signal a failure.

If an exception is caught from a handler this will result in a failure
with status code `500` being signalled.

When handling the failure, the failure handler is passed the routing
context which also allows the failure or failure code to be retrieved so
the failure handler can use that to generate a failure response.

``` js
let route1 = router.get("/somepath/path1/");

route1.handler((routingContext) => {

  // Let's say this throws a RuntimeException
  throw "something happened!";

});

let route2 = router.get("/somepath/path2");

route2.handler((routingContext) => {

  // This one deliberately fails the request passing in the status code
  // E.g. 403 - Forbidden
  routingContext.fail(403);

});

// Define a failure handler
// This will get called for any failures in the above handlers
let route3 = router.get("/somepath/*");

route3.failureHandler((failureRoutingContext) => {

  let statusCode = failureRoutingContext.statusCode();

  // Status code will be 500 for the RuntimeException or 403 for the other failure
  let response = failureRoutingContext.response();
  response.setStatusCode(statusCode).end("Sorry! Not today");

});
```

For the eventuality that an error occurs when running the error handler
related usage of not allowed characters in status message header, then
the original status message will be changed to the default message from
the error code. This is a tradeoff to keep the semantics of the HTTP
protocol working instead of abruptly creash and close the socket without
properly completing the protocol.

# Request body handling

The `BodyHandler` allows you to retrieve request bodies, limit body
sizes and handle file uploads.

You should make sure a body handler is on a matching route for any
requests that require this functionality.

The usage of this handler requires that it is installed as soon as
possible in the router since it needs to install handlers to consume the
HTTP request body and this must be done before executing any async call.

``` js
import { BodyHandler } from "@vertx/web"

// This body handler will be called for all routes
router.route().handler(BodyHandler.create());
```

If an async call is required before, the `HttpServerRequest` should be
paused and then resumed so that the request events are not delivered
until the body handler is ready to process them.

``` js
import { BodyHandler } from "@vertx/web"

router.route().handler((routingContext) => {

  let request = routingContext.request();

  // Pause the request
  request.pause();

  someAsyncCall((result) => {

    // Resume the request
    request.resume();

    // And continue processing
    routingContext.next();
  });
});

// This body handler will be called for all routes
router.route().handler(BodyHandler.create());
```

## Getting the request body

If you know the request body is JSON, then you can use `getBodyAsJson`,
if you know it’s a string you can use `getBodyAsString`, or to retrieve
it as a buffer use `getBody`.

## Limiting body size

To limit the size of a request body, create the body handler then use
`setBodyLimit` to specifying the maximum body size, in bytes. This is
useful to avoid running out of memory with very large bodies.

If an attempt to send a body greater than the maximum size is made, an
HTTP status code of 413 - `Request Entity Too Large`, will be sent.

There is no body limit by default.

## Merging form attributes

By default, the body handler will merge any form attributes into the
request parameters. If you don’t want this behaviour you can use disable
it with `setMergeFormAttributes`.

## Handling file uploads

Body handler is also used to handle multi-part file uploads.

If a body handler is on a matching route for the request, any file
uploads will be automatically streamed to the uploads directory, which
is `file-uploads` by default.

Each file will be given an automatically generated file name, and the
file uploads will be available on the routing context with
`fileUploads`.

Here’s an example:

``` js
import { BodyHandler } from "@vertx/web"

router.route().handler(BodyHandler.create());

router.post("/some/path/uploads").handler((routingContext) => {

  let uploads = routingContext.fileUploads();
  // Do something with uploads....

});
```

Each file upload is described by a `FileUpload` instance, which allows
various properties such as the name, file-name and size to be accessed.

# Handling cookies

Vert.x-Web has out of the box cookies support.

Before 3.8.1, cookie required to use the `CookieHandler` to active this
functionality.

Since 3.8.1, the `CookieHandler` has been deprecated and should not be
used anymore.

## Manipulating cookies

You use `getCookie` to retrieve a cookie by name, or use `cookieMap` to
retrieve the entire set.

To remove a cookie, use `removeCookie`.

To add a cookie use `addCookie`.

The set of cookies will be written back in the response automatically
when the response headers are written so the browser can store them.

Cookies are described by instances of `Cookie`. This allows you to
retrieve the name, value, domain, path and other normal cookie
properties.

Here’s an example of querying and adding cookies:

``` js
import { Cookie } from "@vertx/core"

let someCookie = routingContext.getCookie("mycookie");
let cookieValue = someCookie.getValue();

// Do something with cookie...

// Add a cookie - this will get written back in the response automatically
routingContext.addCookie(Cookie.cookie("othercookie", "somevalue"));
```

# Handling sessions

Vert.x-Web provides out of the box support for sessions.

Sessions last between HTTP requests for the length of a browser session
and give you a place where you can add session-scope information, such
as a shopping basket.

Vert.x-Web uses session cookies to identify a session. The session
cookie is temporary and will be deleted by your browser when it’s
closed.

We don’t put the actual data of your session in the session cookie - the
cookie simply uses an identifier to look-up the actual session on the
server. The identifier is a random UUID generated using a secure random,
so it should be effectively unguessable.

Cookies are passed across the wire in HTTP requests and responses so
it’s always wise to make sure you are using HTTPS when sessions are
being used. Vert.x will warn you if you attempt to use sessions over
straight HTTP.

To enable sessions in your application you must have a `SessionHandler`
on a matching route before your application logic.

The session handler handles the creation of session cookies and the
lookup of the session so you don’t have to do that yourself.

Sessions data is saved to a session store automatically after the
response headers have been sent to the client. But note that, with this
mechanism, there is no guarantee the data is fully persisted before the
client receives the response. There are occasions though when this
guarantee is needed. In this case you can force a flush. This will
disable the automatic saving process, unless the flushing operation
failed. This allows to control the state before completing the response
like:

``` js
router.route().handler((ctx) => {
  sessionHandler.flush(ctx, (flush) => {
    if (flush.succeeded()) {
      ctx.response().end("Success!");
    } else {
      // session wasn't saved...
      // go for plan B
    }
  });
});
```

## Session stores

To create a session handler you need to have a session store instance.
The session store is the object that holds the actual sessions for your
application.

The session store is responsible for holding a secure pseudo random
number generator in order to guarantee secure session ids. This PRNG is
independent of the store which means that given a session id from store
A one cannot derive the session id of store B since they have different
seeds and states.

By default this PRNG uses a mixed mode, blocking for seeding, non
blocking for generating. The PRNG will also reseed every 5 minutes with
64bits of new entropy. However this can all be configured using the
system properties:

  - io.vertx.ext.auth.prng.algorithm e.g.: SHA1PRNG

  - io.vertx.ext.auth.prng.seed.interval e.g.: 1000 (every second)

  - io.vertx.ext.auth.prng.seed.bits e.g.: 128

Most users should not need to configure these values unless if you
notice that the performance of your application is being affected by the
PRNG algorithm.

Vert.x-Web comes with two session store implementations out of the box,
and you can also write your own if you prefer.

The implementations are expected to follow the `ServiceLoader`
conventions and all stores that are available at runtime from the
classpath will be exposed. When more than 1 implementations are
available the first one that can be instantiated and configured with
success becomes the default. If none is available, then the default
depends on the mode Vert.x was created. If cluster mode is available the
the clustered session store is the default otherwise the local storage
is the default.

### Local session store

With this store, sessions are stored locally in memory and only
available in this instance.

This store is appropriate if you have just a single Vert.x instance of
you are using sticky sessions in your application and have configured
your load balancer to always route HTTP requests to the same Vert.x
instance.

If you can’t ensure your requests will all terminate on the same server
then don’t use this store as your requests might end up on a server
which doesn’t know about your session.

Local session stores are implemented by using a shared local map, and
have a reaper which clears out expired sessions.

The reaper interval can be configured with a json message with the key:
`reaperInterval`.

Here are some examples of creating a local `SessionStore`

``` js
import { LocalSessionStore } from "@vertx/web"

// Create a local session store using defaults
let store1 = LocalSessionStore.create(vertx);

// Create a local session store specifying the local shared map name to use
// This might be useful if you have more than one application in the same
// Vert.x instance and want to use different maps for different applications
let store2 = LocalSessionStore.create(vertx, "myapp3.sessionmap");

// Create a local session store specifying the local shared map name to use and
// setting the reaper interval for expired sessions to 10 seconds
let store3 = LocalSessionStore.create(vertx, "myapp3.sessionmap", 10000);
```

### Clustered session store

With this store, sessions are stored in a distributed map which is
accessible across the Vert.x cluster.

This store is appropriate if you’re *not* using sticky sessions, i.e.
your load balancer is distributing different requests from the same
browser to different servers.

Your session is accessible from any node in the cluster using this
store.

To you use a clustered session store you should make sure your Vert.x
instance is clustered.

Here are some examples of creating a clustered `SessionStore`

``` js
import { ClusteredSessionStore } from "@vertx/web"
import { Vertx } from "@vertx/core"

// a clustered Vert.x
Vertx.clusteredVertx(new VertxOptions()
  .setClustered(true), (res) => {

  let vertx = res.result();

  // Create a clustered session store using defaults
  let store1 = ClusteredSessionStore.create(vertx);

  // Create a clustered session store specifying the distributed map name to use
  // This might be useful if you have more than one application in the cluster
  // and want to use different maps for different applications
  let store2 = ClusteredSessionStore.create(vertx, "myclusteredapp3.sessionmap");
});
```

## Creating the session handler

Once you’ve created a session store you can create a session handler,
and add it to a route. You should make sure your session handler is
routed to before your application handlers.

Here’s an example:

``` js
import { Router } from "@vertx/web"
import { ClusteredSessionStore } from "@vertx/web"
import { SessionHandler } from "@vertx/web"

let router = Router.router(vertx);

// Create a clustered session store using defaults
let store = ClusteredSessionStore.create(vertx);

let sessionHandler = SessionHandler.create(store);

// the session handler controls the cookie used for the session
// this includes configuring, for example, the same site policy
// like this, for strict same site policy.
sessionHandler.setCookieSameSite(CookieSameSite.STRICT);

// Make sure all requests are routed through the session handler too
router.route().handler(sessionHandler);

// Now your application handlers
router.route("/somepath/blah/").handler((routingContext) => {

  let session = routingContext.session();
  session.put("foo", "bar");
  // etc

});
```

The session handler will ensure that your session is automatically
looked up (or created if no session exists) from the session store and
set on the routing context before it gets to your application handlers.

## Using the session

In your handlers you can access the session instance with `session`.

You put data into the session with `put`, you get data from the session
with `get`, and you remove data from the session with `remove`.

The keys for items in the session are always strings. The values can be
any type for a local session store, and for a clustered session store
they can be any basic type, or `Buffer`, `JsonObject`, `JsonArray` or a
serializable object, as the values have to serialized across the
cluster.

Here’s an example of manipulating session data:

``` js
router.route().handler(sessionHandler);

// Now your application handlers
router.route("/somepath/blah").handler((routingContext) => {

  let session = routingContext.session();

  // Put some data from the session
  session.put("foo", "bar");

  // Retrieve some data from a session
  let age = session.get("age");

  // Remove some data from a session
  let obj = session.remove("myobj");

});
```

Sessions are automatically written back to the store after after
responses are complete.

You can manually destroy a session using `destroy`. This will remove the
session from the context and the session store. Note that if there is no
session a new one will be automatically created for the next request
from the browser that’s routed through the session handler.

## Session timeout

Sessions will be automatically timed out if they are not accessed for a
time greater than the timeout period. When a session is timed out, it is
removed from the store.

Sessions are automatically marked as accessed when a request arrives and
the session is looked up and and when the response is complete and the
session is stored back in the store.

You can also use `setAccessed` to manually mark a session as accessed.

The session timeout can be configured when creating the session handler.
Default timeout is 30 minutes.

# Authentication / authorisation

Vert.x comes with some out-of-the-box handlers for handling both
authentication and authorisation.

## Creating an auth handler

To create an auth handler you need an instance of `AuthProvider`. Auth
provider is used for authentication and authorisation of users. Vert.x
provides several auth provider instances out of the box in the
vertx-auth project. For full information on auth providers and how to
use and configure them please consult the auth documentation.

Here’s a simple example of creating a basic auth handler given an auth
provider.

``` js
import { LocalSessionStore } from "@vertx/web"
import { SessionHandler } from "@vertx/web"
import { BasicAuthHandler } from "@vertx/web"

router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));

let basicAuthHandler = BasicAuthHandler.create(authProvider);
```

## Handling auth in your application

Let’s say you want all requests to paths that start with `/private/` to
be subject to auth. To do that you make sure your auth handler is before
your application handlers on those paths:

``` js
import { LocalSessionStore } from "@vertx/web"
import { SessionHandler } from "@vertx/web"
import { BasicAuthHandler } from "@vertx/web"

router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)).setAuthProvider(authProvider));

let basicAuthHandler = BasicAuthHandler.create(authProvider);

// All requests to paths starting with '/private/' will be protected
router.route("/private/*").handler(basicAuthHandler);

router.route("/someotherpath").handler((routingContext) => {

  // This will be public access - no login required

});

router.route("/private/somepath").handler((routingContext) => {

  // This will require a login

  // This will have the value true
  let isAuthenticated = (routingContext.user() !== null && routingContext.user() !== undefined);

});
```

If the auth handler has successfully authenticated and authorised the
user it will inject a `User` object into the `RoutingContext` so it’s
available in your handlers with: `user`.

If you want your User object to be stored in the session so it’s
available between requests so you don’t have to authenticate on each
request, then you should make sure you have a session handler and a user
session handler on matching routes before the auth handler.

Once you have your user object you can also programmatically use the
methods on it to authorise the user.

If you want to cause the user to be logged out you can call `clearUser`
on the routing context.

## HTTP Basic Authentication

[HTTP Basic
Authentication](http://en.wikipedia.org/wiki/Basic_access_authentication)
is a simple means of authentication that can be appropriate for simple
applications.

With basic auth, credentials are sent unencrypted across the wire in
HTTP headers so it’s essential that you serve your application using
HTTPS not HTTP.

With basic auth, if a user requests a resource that requires
authorisation, the basic auth handler will send back a `401` response
with the header `WWW-Authenticate` set. This prompts the browser to show
a log-in dialogue and prompt the user to enter their username and
password.

The request is made to the resource again, this time with the
`Authorization` header set, containing the username and password encoded
in Base64.

When the basic auth handler receives this information, it calls the
configured `AuthProvider` with the username and password to authenticate
the user. If the authentication is successful the handler attempts to
authorise the user. If that is successful then the routing of the
request is allowed to continue to the application handlers, otherwise a
`403` response is returned to signify that access is denied.

The auth handler can be set-up with a set of authorities that are
required for access to the resources to be granted.

## Redirect auth handler

With redirect auth handling the user is redirected to towards a login
page in the case they are trying to access a protected resource and they
are not logged in.

The user then fills in the login form and submits it. This is handled by
the server which authenticates the user and, if authenticated redirects
the user back to the original resource.

To use redirect auth you configure an instance of `RedirectAuthHandler`
instead of a basic auth handler.

You will also need to setup handlers to serve your actual login page,
and a handler to handle the actual login itself. To handle the login we
provide a prebuilt handler `FormLoginHandler` for the purpose.

Here’s an example of a simple app, using a redirect auth handler on the
default redirect url `/loginpage`.

``` js
import { LocalSessionStore } from "@vertx/web"
import { SessionHandler } from "@vertx/web"
import { RedirectAuthHandler } from "@vertx/web"
import { FormLoginHandler } from "@vertx/web"
import { StaticHandler } from "@vertx/web"

router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)).setAuthProvider(authProvider));

let redirectAuthHandler = RedirectAuthHandler.create(authProvider);

// All requests to paths starting with '/private/' will be protected
router.route("/private/*").handler(redirectAuthHandler);

// Handle the actual login
// One of your pages must POST form login data
router.post("/login").handler(FormLoginHandler.create(authProvider));

// Set a static server to serve static resources, e.g. the login page
router.route().handler(StaticHandler.create());

router.route("/someotherpath").handler((routingContext) => {
  // This will be public access - no login required
});

router.route("/private/somepath").handler((routingContext) => {

  // This will require a login

  // This will have the value true
  let isAuthenticated = (routingContext.user() !== null && routingContext.user() !== undefined);

});
```

## JWT authorisation

With JWT authorisation resources can be protected by means of
permissions and users without enough rights are denied access. You need
to add the `io.vertx:vertx-auth-jwt:${maven.version}` dependency to use
`JWTAuthProvider`

To use this handler there are 2 steps involved:

  - Setup an handler to issue tokens (or rely on a 3rd party)

  - Setup the handler to filter the requests

Please note that these 2 handlers should be only available on HTTPS, not
doing so allows sniffing the tokens in transit which leads to session
hijacking attacks.

Here’s an example on how to issue tokens:

``` js
import { Router } from "@vertx/web"
import { JWTAuth } from "@vertx/auth-jwt"

let router = Router.router(vertx);

let authConfig = new JWTAuthOptions()
  .setKeyStore(new KeyStoreOptions()
    .setType("jceks")
    .setPath("keystore.jceks")
    .setPassword("secret"));

let authProvider = JWTAuth.create(vertx, authConfig);

router.route("/login").handler((ctx) => {
  // this is an example, authentication should be done with another provider...
  if ("paulo" == ctx.request().getParam("username") && "secret" == ctx.request().getParam("password")) {
    ctx.response().end(authProvider.generateToken({
      "sub" : "paulo"
    }, new JWTOptions()));
  } else {
    ctx.fail(401);
  }
});
```

Now that your client has a token all it is required is that forall\*
consequent request the HTTP header `Authorization` is filled with:
`Bearer <token>` e.g.:

``` js
import { Router } from "@vertx/web"
import { JWTAuth } from "@vertx/auth-jwt"
import { JWTAuthHandler } from "@vertx/web"

let router = Router.router(vertx);

let authConfig = new JWTAuthOptions()
  .setKeyStore(new KeyStoreOptions()
    .setType("jceks")
    .setPath("keystore.jceks")
    .setPassword("secret"));

let authProvider = JWTAuth.create(vertx, authConfig);

router.route("/protected/*").handler(JWTAuthHandler.create(authProvider));

router.route("/protected/somepage").handler((ctx) => {
  // some handle code...
});
```

JWT allows you to add any information you like to the token itself. By
doing this there is no state in the server which allows you to scale
your applications without need for clustered session data. In order to
add data to the token, during the creation of the token just add data to
the JsonObject parameter:

``` js
import { JWTAuth } from "@vertx/auth-jwt"

let authConfig = new JWTAuthOptions()
  .setKeyStore(new KeyStoreOptions()
    .setType("jceks")
    .setPath("keystore.jceks")
    .setPassword("secret"));

let authProvider = JWTAuth.create(vertx, authConfig);

authProvider.generateToken({
  "sub" : "paulo",
  "someKey" : "some value"
}, new JWTOptions());
```

And the same when consuming:

``` js
let handler = (rc) => {
  let theSubject = rc.user().principal().sub;
  let someKey = rc.user().principal().someKey;
};
```

## Configuring required authorities

With any auth handler you can also configure required authorities to
access the resource.

By default, if no authorities are configured then it is sufficient to be
logged in to access the resource, otherwise the user must be both logged
in (authenticated) and have the required authorities.

Here’s an example of configuring an app so that different authorities
are required for different parts of the app. Note that the meaning of
the authorities is determined by the underlying auth provider that you
use. E.g. some may support a role/permission based model but others
might use another model.

``` js
import { RedirectAuthHandler } from "@vertx/web"

let listProductsAuthHandler = RedirectAuthHandler.create(authProvider);
listProductsAuthHandler.addAuthority("list_products");

// Need "list_products" authority to list products
router.route("/listproducts/*").handler(listProductsAuthHandler);

let settingsAuthHandler = RedirectAuthHandler.create(authProvider);
settingsAuthHandler.addAuthority("role:admin");

// Only "admin" has access to /private/settings
router.route("/private/settings/*").handler(settingsAuthHandler);
```

## Chaining multiple auth handlers

There are times when you want to support multiple authN/authZ mechanisms
in a single application. For this you can use the `ChainAuthHandler`.
The chain auth handler will attempt to perform authentication on a chain
of handlers. The chain works both for AuthN and AuthZ, so if the
authentication is valid at a given handler of the chain, then that same
handler will be used to perform authorization (if requested).

It is important to know that some handlers require specific providers,
for example:

  - The `JWTAuthHandler` requires `JWTAuth`.

  - The `DigestAuthHandler` requires `HtdigestAuth`.

  - The `OAuth2AuthHandler` requires `OAuth2Auth`.

So it is not expected that the providers will be shared across all
handlers. There are cases where one can share the provider across
handlers, for example:

  - The `BasicAuthHandler` can take any provider.

  - The `RedirectAuthHandler` can take any provider.

So say that you want to create an application that accepts both `HTTP
Basic Authentication` and `Form Redirect`. You would start configuring
your chain as:

``` js
import { ChainAuthHandler } from "@vertx/web"
import { BasicAuthHandler } from "@vertx/web"
import { RedirectAuthHandler } from "@vertx/web"

let chain = ChainAuthHandler.create();

// add http basic auth handler to the chain
chain.append(BasicAuthHandler.create(provider));
// add form redirect auth handler to the chain
chain.append(RedirectAuthHandler.create(provider));

// secure your route
router.route("/secure/resource").handler(chain);
// your app
router.route("/secure/resource").handler((ctx) => {
  // do something...
});
```

So when a user makes a request without a `Authorization` header, this
means that the chain will fail to authenticate with the basic auth
handler and will attempt to authenticate with the redirect handler.
Since the redirect handler always redirects you will be sent to the
login form that you configured in that handler.

Like the normal routing in vertx-web, auth chaning is a sequence, so if
you would prefer to fallback to your browser asking for the user
credentials using HTTP Basic authentication instead of the redirect all
you need to to is reverse the order of appending to the chain.

Now assume that you make a request where you provide the header
`Authorization` with the value `Basic [token]`. In this case the basic
auth handler will attempt to authenticate and if it is sucessful the
chain will stop and vertx-web will continue to process your handlers. If
the token is not valid, for example bad username/password, then the
chain will continue to the following entry. In this specific case the
redirect auth handler.

# Serving static resources

Vert.x-Web comes with an out of the box handler for serving static web
resources so you can write static web servers very easily.

To serve static resources such as `.html`, `.css`, `.js` or any other
static resource, you use an instance of `StaticHandler`.

Any requests to paths handled by the static handler will result in files
being served from a directory on the file system or from the classpath.
The default static file directory is `webroot` but this can be
configured.

In the following example all requests to paths starting with `/static/`
will get served from the directory `webroot`:

``` js
import { StaticHandler } from "@vertx/web"

router.route("/static/*").handler(StaticHandler.create());
```

For example, if there was a request with path `/static/css/mystyles.css`
the static serve will look for a file in the directory
`webroot/css/mystyle.css`.

It will also look for a file on the classpath called
`webroot/css/mystyle.css`. This means you can package up all your static
resources into a jar file (or fatjar) and distribute them like that.

When Vert.x finds a resource on the classpath for the first time it
extracts it and caches it in a temporary directory on disk so it doesn’t
have to do this each time.

The handler will handle range aware requests. When a client makes a
request to a static resource, the handler will notify that it can handle
range aware request by stating the unit on the `Accept-Ranges` header.
Further requests that contain the `Range` header with the correct unit
and start and end indexes will then receive partial responses with the
correct `Content-Range` header.

## Configuring caching

By default the static handler will set cache headers to enable browsers
to effectively cache files.

Vert.x-Web sets the headers `cache-control`,`last-modified`, and `date`.

`cache-control` is set to `max-age=86400` by default. This corresponds
to one day. This can be configured with `setMaxAgeSeconds` if required.

If a browser sends a GET or a HEAD request with an `if-modified-since`
header and the resource has not been modified since that date, a `304`
status is returned which tells the browser to use its locally cached
resource.

If handling of cache headers is not required, it can be disabled with
`setCachingEnabled`.

When cache handling is enabled Vert.x-Web will cache the last modified
date of resources in memory, this avoids a disk hit to check the actual
last modified date every time.

Entries in the cache have an expiry time, and after that time, the file
on disk will be checked again and the cache entry updated.

If you know that your files never change on disk, then the cache entry
will effectively never expire. This is the default.

If you know that your files might change on disk when the server is
running then you can set files read only to false with
`setFilesReadOnly`.

To enable the maximum number of entries that can be cached in memory at
any one time you can use `setMaxCacheSize`.

To configure the expiry time of cache entries you can use
`setCacheEntryTimeout`.

## Configuring the index page

Any requests to the root path `/` will cause the index page to be
served. By default the index page is `index.html`. This can be
configured with `setIndexPage`.

## Changing the web root

By default static resources will be served from the directory `webroot`.
To configure this use `setWebRoot`.

## Serving hidden files

By default the serve will serve hidden files (files starting with `.`).

If you do not want hidden files to be served you can configure it with
`setIncludeHidden`.

## Directory listing

The server can also perform directory listing. By default directory
listing is disabled. To enabled it use `setDirectoryListing`.

When directory listing is enabled the content returned depends on the
content type in the `accept` header.

For `text/html` directory listing, the template used to render the
directory listing page can be configured with `setDirectoryTemplate`.

## Disabling file caching on disk

By default, Vert.x will cache files that are served from the classpath
into a file on disk in a sub-directory of a directory called `.vertx` in
the current working directory. This is mainly useful when deploying
services as fatjars in production where serving a file from the
classpath every time can be slow.

In development this can cause a problem, as if you update your static
content while the server is running, the cached file will be served not
the updated file.

To disable file caching you can provide your vert.x options the property
`fileResolverCachingEnabled` to `false`. For backwards compatibility it
will also default that value to the system property
`vertx.disableFileCaching`. E.g. you could set up a run configuration in
your IDE to set this when running your main class.

# CORS handling

[Cross Origin Resource
Sharing](http://en.wikipedia.org/wiki/Cross-origin_resource_sharing) is
a safe mechanism for allowing resources to be requested from one domain
and served from another.

Vert.x-Web includes a handler `CorsHandler` that handles the CORS
protocol for you.

Here’s an example:

``` js
import { CorsHandler } from "@vertx/web"

// Will only accept GET requests from origin "vertx.io"
router.route().handler(CorsHandler.create("vertx\\.io").allowedMethod(HttpMethod.GET));

router.route().handler((routingContext) => {

  // Your app handlers

});
```

# Multi Tenant

There are cases where your application needs to handle more than just 1
tenant. In this case a helper handler is provided that simplifies
setting up the application.

In the case the tenant is identified by a HTTP header, say for example
`X-Tenant`, then creating the handler is as simple as:

``` js
import { MultiTenantHandler } from "@vertx/web"
router.route().handler(MultiTenantHandler.create("X-Tenant"));
```

You now should register what handler should be executed for the given
tenant:

``` js
import { MultiTenantHandler } from "@vertx/web"
MultiTenantHandler.create("X-Tenant").addTenantHandler("tenant-A", (ctx) => {
  // do something for tenant A...
}).addTenantHandler("tenant-B", (ctx) => {
  // do something for tenant B...
}).addDefaultHandler((ctx) => {
  // do something when no tenant matches...
});
```

This is useful for security situations:

``` js
import { GithubAuth } from "@vertx/auth-oauth2"
import { OAuth2AuthHandler } from "@vertx/web"
import { OAuth2Auth } from "@vertx/auth-oauth2"
import { MultiTenantHandler } from "@vertx/web"
// create an OAuth2 provider, clientID and clientSecret should be requested to github
let gitHubAuthProvider = GithubAuth.create(vertx, "CLIENT_ID", "CLIENT_SECRET");
// create a oauth2 handler on our running server
// the second argument is the full url to the callback as you entered in your provider management console.
let githubOAuth2 = OAuth2AuthHandler.create(gitHubAuthProvider, "https://myserver.com/github-callback");
// setup the callback handler for receiving the GitHub callback
githubOAuth2.setupCallback(router.route());

// create an OAuth2 provider, clientID and clientSecret should be requested to Google
let googleAuthProvider = OAuth2Auth.create(vertx, new OAuth2ClientOptions()
  .setClientID("CLIENT_ID")
  .setClientSecret("CLIENT_SECRET")
  .setFlow("AUTH_CODE")
  .setSite("https://accounts.google.com")
  .setTokenPath("https://www.googleapis.com/oauth2/v3/token")
  .setAuthorizationPath("/o/oauth2/auth"));

// create a oauth2 handler on our domain: "http://localhost:8080"
let googleOAuth2 = OAuth2AuthHandler.create(googleAuthProvider, "http://localhost:8080");


MultiTenantHandler.create("X-Tenant").addTenantHandler("tenant-github", githubOAuth2).addTenantHandler("tenant-google", googleOAuth2).addDefaultHandler((ctx) => {
  ctx.fail(401);
});
```

# Templates

Vert.x-Web includes dynamic page generation capabilities by including
out of the box support for several popular template engines. You can
also easily add your own.

Template engines are described by `TemplateEngine`. In order to render a
template `render` is used.

The simplest way to use templates is not to call the template engine
directly but to use the `TemplateHandler`. This handler calls the
template engine for you based on the path in the HTTP request.

By default the template handler will look for templates in a directory
called `templates`. This can be configured.

The handler will return the results of rendering with a content type of
`text/html` by default. This can also be configured.

When you create the template handler you pass in an instance of the
template engine you want. Template engines are not embedded in vertx-web
so, you need to configure your project to access them. Configuration is
provided for each template engine.

Here are some examples:

``` javascript
var HandlebarsTemplateEngine = require("vertx-web-js/handlebars_template_engine");
var TemplateHandler = require("vertx-web-js/template_handler");

var engine = HandlebarsTemplateEngine.create();
var handler = TemplateHandler.create(engine);

// This will route all GET requests starting with /dynamic/ to the template handler
// E.g. /dynamic/graph.hbs will look for a template in /templates/graph.hbs
router.get("/dynamic/*").handler(handler.handle);

// Route all GET requests for resource ending in .hbs to the template handler
router.getWithRegex(".+\\.hbs").handler(handler.handle);
```

## MVEL template engine

To use MVEL, you need to add the following *dependency* to your project:
`${maven.groupId}:vertx-web-templ-mvel:${maven.version}`. Create an
instance of the MVEL template engine using:
`io.vertx.ext.web.templ.MVELTemplateEngine#create()`

When using the MVEL template engine, it will by default look for
templates with the `.templ` extension if no extension is specified in
the file name.

The routing context `RoutingContext` is available in the MVEL template
as the `context` variable, this means you can render the template based
on anything in the context including the request, response, session or
context data.

Here are some examples:

    The request path is @{context.request().path()}

    The variable 'foo' from the session is @{context.session().get('foo')}

    The value 'bar' from the context data is @{context.get('bar')}

Please consult the [MVEL templates
documentation](http://mvel.codehaus.org/MVEL+2.0+Templating+Guide) for
how to write MVEL templates.

## Jade template engine

To use the Jade template engine, you need to add the following
*dependency* to your project:
`${maven.groupId}:vertx-web-templ-jade:${maven.version}`. Create an
instance of the Jade template engine using:
`io.vertx.ext.web.templ.JadeTemplateEngine#create()`.

When using the Jade template engine, it will by default look for
templates with the `.jade` extension if no extension is specified in the
file name.

The routing context `RoutingContext` is available in the Jade template
as the `context` variable, this means you can render the template based
on anything in the context including the request, response, session or
context data.

Here are some examples:

    !!! 5
    html
     head
       title= context.get('foo') + context.request().path()
     body

Please consult the [Jade4j
documentation](https://github.com/neuland/jade4j) for how to write Jade
templates.

## Handlebars template engine

To use Handlebars, you need to add the following *dependency* to your
project: `${maven.groupId}:vertx-web-templ-handlebars:${maven.version}`.
Create an instance of the Handlebars template engine using:
`io.vertx.ext.web.templ.HandlebarsTemplateEngine#create()`.

When using the Handlebars template engine, it will by default look for
templates with the `.hbs` extension if no extension is specified in the
file name.

Handlebars templates are not able to call arbitrary methods in objects
so we can’t just pass the routing context into the template and let the
template introspect it like we can with other template engines.

Instead, the context `data` is available in the template.

If you want to have access to other data like the request path, request
params or session data you should add it the context data in a handler
before the template handler. For example:

``` js
import { TemplateHandler } from "@vertx/web"

let handler = TemplateHandler.create(engine);

router.get("/dynamic").handler((routingContext) => {

  routingContext.put("request_path", routingContext.request().path());
  routingContext.put("session_data", routingContext.session().data());

  routingContext.next();
});

router.get("/dynamic/").handler(handler);
```

Please consult the [Handlebars Java port
documentation](https://github.com/jknack/handlebars.java) for how to
write handlebars templates.

## Thymeleaf template engine

To use Thymeleaf, you need to add the following *dependency* to your
project: `${maven.groupId}:vertx-web-templ-thymeleaf:${maven.version}`.
Create an instance of the Thymeleaf template engine using:
`io.vertx.ext.web.templ.ThymeleafTemplateEngine#create()`.

When using the Thymeleaf template engine, it will by default look for
templates with the `.html` extension if no extension is specified in the
file name.

The routing context `RoutingContext` is available in the Thymeleaf
template as the `context` variable, this means you can render the
template based on anything in the context including the request,
response, session or context data.

Here are some examples:

``` html
    <p th:text="${context.get('foo')}"></p>
    <p th:text="${context.get('bar')}"></p>
    <p th:text="${context.normalisedPath()}"></p>
    <p th:text="${context.request().params().get('param1')}"></p>
    <p th:text="${context.request().params().get('param2')}"></p>
```

Please consult the [Thymeleaf documentation](http://www.thymeleaf.org/)
for how to write Thymeleaf templates.

## Apache FreeMarker template engine

To use Apache FreeMarker, you need to add the following *dependency* to
your project:
`${maven.groupId}:vertx-web-templ-freemarker:${maven.version}`. Create
an instance of the Apache FreeMarker template engine using:
`io.vertx.ext.web.templ.Engine#create()`.

When using the Apache FreeMarker template engine, it will by default
look for templates with the `.ftl` extension if no extension is
specified in the file name.

The routing context `RoutingContext` is available in the Apache
FreeMarker template as the `context` variable, this means you can render
the template based on anything in the context including the request,
response, session or context data.

Here are some examples:

```html
    <p th:text="${context.foo}"></p>
    <p th:text="${context.bar}"></p>
    <p th:text="${context.normalisedPath()}"></p>
    <p th:text="${context.request().params().param1}"></p>
    <p th:text="${context.request().params().param2}"></p>
```

Please consult the [Apache FreeMarker
documentation](http://www.freemarker.org/) for how to write Apache
FreeMarker templates.

## Pebble template engine

To use Pebble, you need to add the following *dependency* to your
project: `io.vertx:vertx-web-templ-pebble:${maven.version}`. Create an
instance of the Pebble template engine using:
`io.vertx.ext.web.templ.PebbleTemplateEngine#create(vertx)`.

When using the Pebble template engine, it will by default look for
templates with the `.peb` extension if no extension is specified in the
file name.

The routing context `RoutingContext` is available in the Pebble template
as the `context` variable, this means you can render the template based
on anything in the context including the request, response, session or
context data.

Here are some examples:

``` html
    <p th:text="{{context.foo}}"></p>
    <p th:text="{{context.bar}}"></p>
    <p th:text="{{context.normalisedPath()}}"></p>
    <p th:text="{{context.request().params().param1}}"></p>
    <p th:text="{{context.request().params().param2}}"></p>
```

Please consult the [Pebble
documentation](http://www.mitchellbosecke.com/pebble/home/) for how to
write Pebble templates.

## Rocker template engine

To use Rocker, then add
`io.vertx:vertx-web-templ-rocker:${maven.version}` as a dependency to
your project. You can then create a Rocker template engine instance with
`io.vertx.ext.web.templ.rocker#create()`.

The values of the JSON context object passed to the `render` method are
then exposed as template parameters. Given:

``` java
    final JsonObject context = new JsonObject()
     .put("foo", "badger")
     .put("bar", "fox")
     .put("context", new JsonObject().put("path", "/foo/bar"));

    engine.render(context, "somedir/TestRockerTemplate2", render -> {
     // (...)
    });
```

then the template can be as the following
`somedir/TestRockerTemplate2.rocker.html` resource file:

    @import io.vertx.core.json.JsonObject
    @args (JsonObject context, String foo, String bar)
    Hello @foo and @bar
    Request path is @context.getString("path")

## Disabling caching

During development you might want to disable template caching so that
the template gets reevaluated on each request. In order to do this you
need to set the system property:
`io.vertx.ext.web.TemplateEngine.disableCache` to `true`.

By default it will be false. So caching is always enabled.

# Error handler

You can render your own errors using a template handler or otherwise but
Vert.x-Web also includes an out of the boxy "pretty" error handler that
can render error pages for you.

The handler is `ErrorHandler`. To use the error handler just set it as a
failure handler for any paths that you want covered.

# Request logger

Vert.x-Web includes a handler `LoggerHandler` that you can use to log
HTTP requests. You should mount this handler before any handler that
could fail the `RoutingContext`

By default requests are logged to the Vert.x logger which can be
configured to use JUL logging, log4j or SLF4J.

See `LoggerFormat`.

# Serving favicons

Vert.x-Web includes the handler `FaviconHandler` especially for serving
favicons.

Favicons can be specified using a path to the filesystem, or by default
Vert.x-Web will look for a file on the classpath with the name
`favicon.ico`. This means you bundle the favicon in the jar of your
application.

# Timeout handler

Vert.x-Web includes a timeout handler that you can use to timeout
requests if they take too long to process.

This is configured using an instance of `TimeoutHandler`.

If a request times out before the response is written a `503` response
will be returned to the client.

Here’s an example of using a timeout handler which will timeout all
requests to paths starting with `/foo` after 5 seconds:

``` js
import { TimeoutHandler } from "@vertx/web"

router.route("/foo/").handler(TimeoutHandler.create(5000));
```

# Response time handler

This handler sets the header `x-response-time` response header
containing the time from when the request was received to when the
response headers were written, in ms., e.g.:

x-response-time: 1456ms

# Content type handler

The `ResponseContentTypeHandler` can set the `Content-Type` header
automatically. Suppose we are building a RESTful web application. We
need to set the content type in all our handlers:

``` js
router.get("/api/books").produces("application/json").handler((rc) => {
  findBooks((ar) => {
    if (ar.succeeded()) {
      rc.response().putHeader("Content-Type", "application/json").end(toJson(ar.result()));
    } else {
      rc.fail(ar.cause());
    }
  });
});
```

If the API surface becomes pretty large, setting the content type can
become cumbersome. To avoid this situation, add the
`ResponseContentTypeHandler` to the corresponding routes:

``` js
import { ResponseContentTypeHandler } from "@vertx/web"
router.route("/api/*").handler(ResponseContentTypeHandler.create());
router.get("/api/books").produces("application/json").handler((rc) => {
  findBooks((ar) => {
    if (ar.succeeded()) {
      rc.response().end(toJson(ar.result()));
    } else {
      rc.fail(ar.cause());
    }
  });
});
```

The handler gets the approriate content type from
`getAcceptableContentType`. As a consequence, you can easily share the
same handler to produce data of different types:

``` js
import { ResponseContentTypeHandler } from "@vertx/web"
router.route("/api/*").handler(ResponseContentTypeHandler.create());
router.get("/api/books").produces("text/xml").produces("application/json").handler((rc) => {
  findBooks((ar) => {
    if (ar.succeeded()) {
      if (rc.getAcceptableContentType() == "text/xml") {
        rc.response().end(toXML(ar.result()));
      } else {
        rc.response().end(toJson(ar.result()));
      }
    } else {
      rc.fail(ar.cause());
    }
  });
});
```

# SockJS

SockJS is a client side JavaScript library and protocol which provides a
simple WebSocket-like interface allowing you to make connections to
SockJS servers irrespective of whether the actual browser or network
will allow real WebSockets.

It does this by supporting various different transports between browser
and server, and choosing one at run-time according to browser and
network capabilities.

All this is transparent to you - you are simply presented with the
WebSocket-like interface which *just works*.

Please see the [SockJS website](https://github.com/sockjs/sockjs-client)
for more information on SockJS.

## SockJS handler

Vert.x provides an out of the box handler called `SockJSHandler` for
using SockJS in your Vert.x-Web applications.

You should create one handler per SockJS application using
`SockJSHandler.create`. You can also specify configuration options when
creating the instance. The configuration options are described with an
instance of `SockJSHandlerOptions`.

``` js
import { Router } from "@vertx/web"
import { SockJSHandler } from "@vertx/web"

let router = Router.router(vertx);

let options = new SockJSHandlerOptions()
  .setHeartbeatInterval(2000);

let sockJSHandler = SockJSHandler.create(vertx, options);

router.route("/myapp/*").handler(sockJSHandler);
```

## Handling SockJS sockets

On the server-side you set a handler on the SockJS handler, and this
will be called every time a SockJS connection is made from a client:

The object passed into the handler is a `SockJSSocket`. This has a
familiar socket-like interface which you can read and write to similarly
to a `NetSocket` or a `WebSocket`. It also implements `ReadStream` and
`WriteStream` so you can pump it to and from other read and write
streams.

Here’s an example of a simple SockJS handler that simply echoes back any
back any data that it reads:

``` js
import { Router } from "@vertx/web"
import { SockJSHandler } from "@vertx/web"

let router = Router.router(vertx);

let options = new SockJSHandlerOptions()
  .setHeartbeatInterval(2000);

let sockJSHandler = SockJSHandler.create(vertx, options);

sockJSHandler.socketHandler((sockJSSocket) => {

  // Just echo the data back
  sockJSSocket.handler(sockJSSocket.write);
});

router.route("/myapp/*").handler(sockJSHandler);
```

## The client side

In client side JavaScript you use the SockJS client side library to make
connections.

You can find that
[here](http://cdn.jsdelivr.net/sockjs/0.3.4/sockjs.min.js).

Full details for using the SockJS JavaScript client are on the [SockJS
website](https://github.com/sockjs/sockjs-client), but in summary you
use it something like this:

    var sock = new SockJS('http://mydomain.com/myapp');

    sock.onopen = function() {
     console.log('open');
    };

    sock.onmessage = function(e) {
     console.log('message', e.data);
    };

    sock.onclose = function() {
     console.log('close');
    };

    sock.send('test');

    sock.close();

## Configuring the SockJS handler

The handler can be configured with various options using
`SockJSHandlerOptions`.

  - `insertJSESSIONID`
    Insert a JSESSIONID cookie so load-balancers ensure requests for a
    specific SockJS session are always routed to the correct server.
    Default is `true`.

  - `sessionTimeout`
    The server sends a `close` event when a client receiving connection
    have not been seen for a while. This delay is configured by this
    setting. By default the `close` event will be emitted when a
    receiving connection wasn’t seen for 5 seconds.

  - `heartbeatInterval`
    In order to keep proxies and load balancers from closing long
    running http requests we need to pretend that the connection is
    active and send a heartbeat packet once in a while. This setting
    controls how often this is done. By default a heartbeat packet is
    sent every 25 seconds.

  - `maxBytesStreaming`
    Most streaming transports save responses on the client side and
    don’t free memory used by delivered messages. Such transports need
    to be garbage-collected once in a while. `max_bytes_streaming` sets
    a minimum number of bytes that can be send over a single http
    streaming request before it will be closed. After that client needs
    to open new request. Setting this value to one effectively disables
    streaming and will make streaming transports to behave like polling
    transports. The default value is 128K.

  - `libraryURL`
    Transports which don’t support cross-domain communication natively
    ('eventsource' to name one) use an iframe trick. A simple page is
    served from the SockJS server (using its foreign domain) and is
    placed in an invisible iframe. Code run from this iframe doesn’t
    need to worry about cross-domain issues, as it’s being run from
    domain local to the SockJS server. This iframe also does need to
    load SockJS javascript client library, and this option lets you
    specify its url (if you’re unsure, point it to the latest minified
    SockJS client release, this is the default). The default value is
    `http://cdn.jsdelivr.net/sockjs/0.3.4/sockjs.min.js`

  - `disabledTransports`
    This is a list of transports that you want to disable. Possible
    values are WEBSOCKET, EVENT\_SOURCE, HTML\_FILE, JSON\_P, XHR.

# SockJS event bus bridge

Vert.x-Web comes with a built-in SockJS socket handler called the event
bus bridge which effectively extends the server-side Vert.x event bus
into client side JavaScript.

This creates a distributed event bus which not only spans multiple
Vert.x instances on the server side, but includes client side JavaScript
running in browsers.

We can therefore create a huge distributed bus encompassing many
browsers and servers. The browsers don’t have to be connected to the
same server as long as the servers are connected.

This is done by providing a simple client side JavaScript library called
`vertx-eventbus.js` which provides an API very similar to the
server-side Vert.x event-bus API, which allows you to send and publish
messages to the event bus and register handlers to receive messages.

This JavaScript library uses the JavaScript SockJS client to tunnel the
event bus traffic over SockJS connections terminating at at a
`SockJSHandler` on the server-side.

A special SockJS socket handler is then installed on the `SockJSHandler`
which handles the SockJS data and bridges it to and from the server side
event bus.

To activate the bridge you simply call `bridge` on the SockJS handler.

``` js
import { Router } from "@vertx/web"
import { SockJSHandler } from "@vertx/web"

let router = Router.router(vertx);

let sockJSHandler = SockJSHandler.create(vertx);
let options = new SockJSBridgeOptions();
// mount the bridge on the router
router.mountSubRouter("/eventbus", sockJSHandler.bridge(options));
```

In client side JavaScript you use the 'vertx-eventbus.js\` library to
create connections to the event bus and to send and receive messages:

``` html
<script src="http://cdn.jsdelivr.net/sockjs/0.3.4/sockjs.min.js"></script>
<script src='vertx-eventbus.js'></script>

<script>

var eb = new EventBus('http://localhost:8080/eventbus');

eb.onopen = function() {

 // set a handler to receive a message
 eb.registerHandler('some-address', function(error, message) {
   console.log('received a message: ' + JSON.stringify(message));
 });

 // send a message
 eb.send('some-address', {name: 'tim', age: 587});

}

</script>
```

The first thing the example does is to create a instance of the event
bus

``` javascript
var eb = new EventBus('http://localhost:8080/eventbus');
```

The parameter to the constructor is the URI where to connect to the
event bus. Since we create our bridge with the prefix `eventbus` we will
connect there.

You can’t actually do anything with the connection until it is opened.
When it is open the `onopen` handler will be called.

The bridge supports automatic reconnection, with configurable delay and
backoff options.

``` javascript
var eb = new EventBus('http://localhost:8080/eventbus');
eb.enableReconnect(true);
eb.onopen = function() {}; // Set up handlers here, will be called on initial connection and all reconnections
eb.onreconnect = function() {}; // Optional, will only be called on reconnections

// Alternatively, pass in an options object
var options = {
   vertxbus_reconnect_attempts_max: Infinity, // Max reconnect attempts
   vertxbus_reconnect_delay_min: 1000, // Initial delay (in ms) before first reconnect attempt
   vertxbus_reconnect_delay_max: 5000, // Max delay (in ms) between reconnect attempts
   vertxbus_reconnect_exponent: 2, // Exponential backoff factor
   vertxbus_randomization_factor: 0.5 // Randomization factor between 0 and 1
};

var eb2 = new EventBus('http://localhost:8080/eventbus', options);
eb2.enableReconnect(true);
// Set up handlers...
```

You can retrieve the client library using a dependency manager:

  - Maven (in your `pom.xml`):

<!-- end list -->

``` xml
<dependency>
 <groupId>${maven.groupId}</groupId>
 <artifactId>${maven.artifactId}</artifactId>
 <version>${maven.version}</version>
 <classifier>client</classifier>
 <type>js</type>
</dependency>
```

  - Gradle (in your `build.gradle` file):

<!-- end list -->

``` groovy
compile '${maven.groupId}:${maven.artifactId}:${maven.version}:client'
```

The library is also available on:

  - [NPM](https://www.npmjs.com/package/vertx3-eventbus-client)

  - [Bower](https://github.com/vert-x3/vertx-bus-bower)

  - [cdnjs](https://cdnjs.com/libraries/vertx)

Notice that the API has changed between the 3.0.0 and 3.1.0 version.
Please check the changelog. The previous client is still compatible and
can still be used, but the new client offers more feature and is closer
to the vert.x event bus API.

## Securing the Bridge

If you started a bridge like in the above example without securing it,
and attempted to send messages through it you’d find that the messages
mysteriously disappeared. What happened to them?

For most applications you probably don’t want client side JavaScript
being able to send just any message to any handlers on the server side
or to all other browsers.

For example, you may have a service on the event bus which allows data
to be accessed or deleted. We don’t want badly behaved or malicious
clients being able to delete all the data in your database\!

Also, we don’t necessarily want any client to be able to listen in on
any event bus address.

To deal with this, a SockJS bridge will by default refuse to let through
any messages. It’s up to you to tell the bridge what messages are ok for
it to pass through. (There is an exception for reply messages which are
always allowed through).

In other words the bridge acts like a kind of firewall which has a
default *deny-all* policy.

Configuring the bridge to tell it what messages it should pass through
is easy.

You can specify which *matches* you want to allow for inbound and
outbound traffic using the `SockJSBridgeOptions` that you pass in when
calling bridge.

Each match is a `PermittedOptions` object:

  - `setAddress`
    This represents the exact address the message is being sent to. If
    you want to allow messages based on an exact address you use this
    field.

  - `setAddressRegex`
    This is a regular expression that will be matched against the
    address. If you want to allow messages based on a regular expression
    you use this field. If the `address` field is specified this field
    will be ignored.

  - `setMatch`
    This allows you to allow messages based on their structure. Any
    fields in the match must exist in the message with the same values
    for them to be allowed. This currently only works with JSON
    messages.

If a message is *in-bound* (i.e. being sent from client side JavaScript
to the server) when it is received Vert.x-Web will look through any
inbound permitted matches. If any match, it will be allowed through.

If a message is *out-bound* (i.e. being sent from the server to client
side JavaScript) before it is sent to the client Vert.x-Web will look
through any outbound permitted matches. If any match, it will be allowed
through.

The actual matching works as follows:

If an `address` field has been specified then the `address` must match
*exactly* with the address of the message for it to be considered
matched.

If an `address` field has not been specified and an `addressRegex` field
has been specified then the regular expression in `address_re` must
match with the address of the message for it to be considered matched.

If a `match` field has been specified, then also the structure of the
message must match. Structuring matching works by looking at all the
fields and values in the match object and checking they all exist in the
actual message body.

Here’s an example:

``` js
import { Router } from "@vertx/web"
import { SockJSHandler } from "@vertx/web"

let router = Router.router(vertx);

let sockJSHandler = SockJSHandler.create(vertx);


// Let through any messages sent to 'demo.orderMgr' from the client
let inboundPermitted1 = new PermittedOptions()
  .setAddress("demo.orderMgr");

// Allow calls to the address 'demo.persistor' from the client as long as the messages
// have an action field with value 'find' and a collection field with value
// 'albums'
let inboundPermitted2 = new PermittedOptions()
  .setAddress("demo.persistor")
  .setMatch({
    "action" : "find",
    "collection" : "albums"
  });

// Allow through any message with a field `wibble` with value `foo`.
let inboundPermitted3 = new PermittedOptions()
  .setMatch({
    "wibble" : "foo"
  });

// First let's define what we're going to allow from server -> client

// Let through any messages coming from address 'ticker.mystock'
let outboundPermitted1 = new PermittedOptions()
  .setAddress("ticker.mystock");

// Let through any messages from addresses starting with "news." (e.g. news.europe, news.usa, etc)
let outboundPermitted2 = new PermittedOptions()
  .setAddressRegex("news\\..+");

// Let's define what we're going to allow from client -> server
let options = new SockJSBridgeOptions()
  .setInboundPermitteds([inboundPermitted1, inboundPermitted1, inboundPermitted3])
  .setOutboundPermitteds([outboundPermitted1, outboundPermitted2]);

// mount the bridge on the router
router.mountSubRouter("/eventbus", sockJSHandler.bridge(options));
```

## Requiring authorisation for messages

The event bus bridge can also be configured to use the Vert.x-Web
authorisation functionality to require authorisation for messages,
either in-bound or out-bound on the bridge.

To do this, you can add extra fields to the match described in the
previous section that determine what authority is required for the
match.

To declare that a specific authority for the logged-in user is required
in order to access allow the messages you use the `setRequiredAuthority`
field.

Here’s an example:

``` js
// Let through any messages sent to 'demo.orderService' from the client
let inboundPermitted = new PermittedOptions()
  .setAddress("demo.orderService");

// But only if the user is logged in and has the authority "place_orders"
inboundPermitted.requiredAuthority = "place_orders";

let options = new SockJSBridgeOptions()
  .setInboundPermitteds([inboundPermitted]);
```

For the user to be authorised they must be first logged in and secondly
have the required authority.

To handle the login and actually auth you can configure the normal
Vert.x auth handlers. For example:

``` js
import { Router } from "@vertx/web"
import { SockJSHandler } from "@vertx/web"
import { LocalSessionStore } from "@vertx/web"
import { SessionHandler } from "@vertx/web"
import { BasicAuthHandler } from "@vertx/web"

let router = Router.router(vertx);

// Let through any messages sent to 'demo.orderService' from the client
let inboundPermitted = new PermittedOptions()
  .setAddress("demo.orderService");

// But only if the user is logged in and has the authority "place_orders"
inboundPermitted.requiredAuthority = "place_orders";

let sockJSHandler = SockJSHandler.create(vertx);

// Now set up some basic auth handling:

router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));

let basicAuthHandler = BasicAuthHandler.create(authProvider);

router.route("/eventbus/*").handler(basicAuthHandler);

// mount the bridge on the router
router.mountSubRouter("/eventbus", sockJSHandler.bridge(new SockJSBridgeOptions()
  .setInboundPermitteds([inboundPermitted])));
```

## Handling event bus bridge events

If you want to be notified when an event occurs on the bridge you can
provide a handler when calling `bridge`.

Whenever an event occurs on the bridge it will be passed to the handler.
The event is described by an instance of `BridgeEvent`.

The event can be one of the following types:

  - SOCKET\_CREATED
    This event will occur when a new SockJS socket is created.

  - SOCKET\_IDLE
    This event will occur when SockJS socket is on idle for longer
    period of time than initially configured.

  - SOCKET\_PING
    This event will occur when the last ping timestamp is updated for
    the SockJS socket.

  - SOCKET\_CLOSED
    This event will occur when a SockJS socket is closed.

  - SEND
    This event will occur when a message is attempted to be sent from
    the client to the server.

  - PUBLISH
    This event will occur when a message is attempted to be published
    from the client to the server.

  - RECEIVE
    This event will occur when a message is attempted to be delivered
    from the server to the client.

  - REGISTER
    This event will occur when a client attempts to register a handler.

  - UNREGISTER
    This event will occur when a client attempts to unregister a
    handler.

The event enables you to retrieve the type using `type` and inspect the
raw message of the event using `getRawMessage`.

The raw message is a JSON object with the following structure:

    {
     "type": "send"|"publish"|"receive"|"register"|"unregister",
     "address": the event bus address being sent/published/registered/unregistered
     "body": the body of the message
    }

The event is also an instance of `Future`. When you are finished
handling the event you can complete the future with `true` to enable
further processing.

If you don’t want the event to be processed you can complete the future
with `false`. This is a useful feature that enables you to do your own
filtering on messages passing through the bridge, or perhaps apply some
fine grained authorisation or metrics.

Here’s an example where we reject all messages flowing through the
bridge if they contain the word "Armadillos".

``` js
import { Router } from "@vertx/web"
import { SockJSHandler } from "@vertx/web"

let router = Router.router(vertx);

// Let through any messages sent to 'demo.orderMgr' from the client
let inboundPermitted = new PermittedOptions()
  .setAddress("demo.someService");

let sockJSHandler = SockJSHandler.create(vertx);
let options = new SockJSBridgeOptions()
  .setInboundPermitteds([inboundPermitted]);

// mount the bridge on the router
router.mountSubRouter("/eventbus", sockJSHandler.bridge(options, (be) => {
  if (be.type() === BridgeEventType.PUBLISH || be.type() === BridgeEventType.RECEIVE) {
    if (be.getRawMessage().body == "armadillos") {
      // Reject it
      be.complete(false);
      return
    }
  }
  be.complete(true);
}));
```

Here’s an example how to configure and handle SOCKET\_IDLE bridge event
type. Notice `setPingTimeout(5000)` which says that if ping message
doesn’t arrive from client within 5 seconds then the SOCKET\_IDLE bridge
event would be triggered.

``` js
import { Router } from "@vertx/web"
import { SockJSHandler } from "@vertx/web"
let router = Router.router(vertx);

// Initialize SockJS handler
let sockJSHandler = SockJSHandler.create(vertx);
let options = new SockJSBridgeOptions()
  .setInboundPermitteds([inboundPermitted])
  .setPingTimeout(5000);

// mount the bridge on the router
router.mountSubRouter("/eventbus", sockJSHandler.bridge(options, (be) => {
  if (be.type() === BridgeEventType.SOCKET_IDLE) {
    // Do some custom handling...
  }

  be.complete(true);
}));
```

In client side JavaScript you use the 'vertx-eventbus.js\` library to
create connections to the event bus and to send and receive messages:

``` html
<script src="http://cdn.jsdelivr.net/sockjs/0.3.4/sockjs.min.js"></script>
<script src='vertx-eventbus.js'></script>

<script>

var eb = new EventBus('http://localhost:8080/eventbus', {"vertxbus_ping_interval": 300000}); // sends ping every 5 minutes.

eb.onopen = function() {

// set a handler to receive a message
eb.registerHandler('some-address', function(error, message) {
  console.log('received a message: ' + JSON.stringify(message));
});

// send a message
eb.send('some-address', {name: 'tim', age: 587});
}

</script>
```

The first thing the example does is to create a instance of the event
bus

``` javascript
var eb = new EventBus('http://localhost:8080/eventbus', {"vertxbus_ping_interval": 300000});
```

The 2nd parameter to the constructor tells the sockjs library to send
ping message every 5 minutes. since the server was configured to expect
ping every 5 seconds → `SOCKET_IDLE` would be triggered on the server.

You can also amend the raw message, e.g. change the body. For messages
that are flowing in from the client you can also add headers to the
message, here’s an example:

``` js
import { Router } from "@vertx/web"
import { SockJSHandler } from "@vertx/web"

let router = Router.router(vertx);

// Let through any messages sent to 'demo.orderService' from the client
let inboundPermitted = new PermittedOptions()
  .setAddress("demo.orderService");

let sockJSHandler = SockJSHandler.create(vertx);
let options = new SockJSBridgeOptions()
  .setInboundPermitteds([inboundPermitted]);

// mount the bridge on the router
router.mountSubRouter("/eventbus", sockJSHandler.bridge(options, (be) => {
  if (be.type() === BridgeEventType.PUBLISH || be.type() === BridgeEventType.SEND) {
    // Add some headers
    let headers = {
      "header1" : "val",
      "header2" : "val2"
    };
    let rawMessage = be.getRawMessage();
    rawMessage.headers = headers;
    be.setRawMessage(rawMessage);
  }
  be.complete(true);
}));
```

# CSRF Cross Site Request Forgery

CSRF or sometimes also known as XSRF is a technique by which an
unauthorized site can gain your user’s private data. Vert.x-Web includes
a handler `CSRFHandler` that you can use to prevent cross site request
forgery requests.

On each get request under this handler a cookie is added to the response
with a unique token. Clients are then expected to return this token back
in a header. Since cookies are sent it is required that the cookie
handler is also present on the router.

When developing non single page applications that rely on the User-Agent
to perform the `POST` action, Headers cannot be specified on HTML Forms.
In order to solve this problem the header value will also be checked if
and only if no header was present in the Form attributes under the same
name as the header, e.g.:

``` html
---
<form action="/submit" method="POST">
<input type="hidden" name="X-XSRF-TOKEN" value="abracadabra">
</form>
---
```

It is the responsibility of the user to fill in the right value for the
form field. Users who prefer to use an HTML only solution can fill this
value by fetching the the token value from the routing context under the
key `X-XSRF-TOKEN` or the header name they have chosen during the
instantiation of the `CSRFHandler` object.

``` js
import { CSRFHandler } from "@vertx/web"

router.route().handler(CSRFHandler.create("abracadabra"));
router.route().handler((rc) => {

});
```

## Using AJAX

When accessing protected routes via ajax both the csrf token will need
to be passed in the request. Typically this is done using a request
header, as adding a request header can typically be done at a central
location easily without payload modification.

The CSRF token is obtained from the server side context under the key
`X-XSRF-TOKEN` (unless you specified a different name). This token needs
to be exposed to the client-side, typically by including it in the
initial page content. One possibility is to store it in an HTML \<meta\>
tag, where value can then be retrieved at the time of the request by
JavaScript.

The following can be included in your view (handlebar example below):

``` html
<meta name="csrf-token" content="${X-XSRF-TOKEN}">
```

The following is an example of using the Fetch API to post to the
/process route with the CSRF token from the \<meta\> tag on the page:

``` js
// Read the CSRF token from the <meta> tag
var token = document.querySelector('meta[name="csrf-token"]').getAttribute('content')

// Make a request using the Fetch API
fetch('/process', {
 credentials: 'same-origin', // <-- includes cookies in the request
 headers: {
   'X-XSRF-TOKEN': token // <-- is the csrf token as a header
 },
 method: 'POST',
 body: {
   key: 'value'
 }
})
```

# VirtualHost Handler

The Virtual Host Handler will verify the request hostname and if it
matches it will send the request to the registered handler, otherwise
will continue inside the normal handlers chain.

Request are checked against the `Host` header to a match and patterns
allow the usage of `  ` wildcards, as for example `</emphasis>.vertx.io`
or fully domain names as `www.vertx.io`.

``` js
import { VirtualHostHandler } from "@vertx/web"
router.route().handler(VirtualHostHandler.create("*.vertx.io", (routingContext) => {
  // do something if the request is for *.vertx.io
}));
```

# OAuth2AuthHandler Handler

The `OAuth2AuthHandler` allows quick setup of secure routes using the
OAuth2 protocol. This handler simplifies the authCode flow. An example
of using it to protect some resource and authenticate with GitHub can be
implemented as:

``` js
import { GithubAuth } from "@vertx/auth-oauth2"
import { OAuth2AuthHandler } from "@vertx/web"

// create an OAuth2 provider, clientID and clientSecret should be requested to github
let authProvider = GithubAuth.create(vertx, "CLIENT_ID", "CLIENT_SECRET");

// create a oauth2 handler on our running server
// the second argument is the full url to the callback as you entered in your provider management console.
let oauth2 = OAuth2AuthHandler.create(authProvider, "https://myserver.com/callback");

// setup the callback handler for receiving the GitHub callback
oauth2.setupCallback(router.route());

// protect everything under /protected
router.route("/protected/*").handler(oauth2);
// mount some handler under the protected zone
router.route("/protected/somepage").handler((rc) => {
  rc.response().end("Welcome to the protected resource!");
});

// welcome page
router.get("/").handler((ctx) => {
  ctx.response().putHeader("content-type", "text/html").end("Hello<br><a href=\"/protected/somepage\">Protected by Github</a>");
});
```

The OAuth2AuthHandler will setup a proper callback OAuth2 handler so the
user does not need to deal with validation of the authority server
response. It is quite important to know that authority server responses
are only valid once, this means that if a client issues a reload of the
callback URL it will be asserted as a invalid request since the
validation will fail.

A rule of thumb is once a valid callback is executed issue a client side
redirect to a protected resource. This redirect should also create a
session cookie (or other session mechanism) so the user is not required
to authenticate for every request.

Due to the nature of OAuth2 spec there are slight changes required in
order to use other OAuth2 providers but vertx-auth provides you with
many out of the box implementations:

  - Azure Active Directory `AzureADAuth`

  - Box.com `BoxAuth`

  - Dropbox `DropboxAuth`

  - Facebook `FacebookAuth`

  - Foursquare `FoursquareAuth`

  - Github `GithubAuth`

  - Google `GoogleAuth`

  - Instagram `InstagramAuth`

  - Keycloak `KeycloakAuth`

  - LinkedIn `LinkedInAuth`

  - Mailchimp `MailchimpAuth`

  - Salesforce `SalesforceAuth`

  - Shopify `ShopifyAuth`

  - Soundcloud `SoundcloudAuth`

  - Stripe `StripeAuth`

  - Twitter `TwitterAuth`

However if you’re using an unlisted provider you can still do it using
the base API like this:

``` js
import { OAuth2Auth } from "@vertx/auth-oauth2"
import { OAuth2AuthHandler } from "@vertx/web"

// create an OAuth2 provider, clientID and clientSecret should be requested to Google
let authProvider = OAuth2Auth.create(vertx, OAuth2FlowType.AUTH_CODE, new OAuth2ClientOptions()
  .setClientID("CLIENT_ID")
  .setClientSecret("CLIENT_SECRET")
  .setSite("https://accounts.google.com")
  .setTokenPath("https://www.googleapis.com/oauth2/v3/token")
  .setAuthorizationPath("/o/oauth2/auth"));

// create a oauth2 handler on our domain: "http://localhost:8080"
let oauth2 = OAuth2AuthHandler.create(authProvider, "http://localhost:8080");

// these are the scopes
oauth2.addAuthority("profile");

// setup the callback handler for receiving the Google callback
oauth2.setupCallback(router.get("/callback"));

// protect everything under /protected
router.route("/protected/*").handler(oauth2);
// mount some handler under the protected zone
router.route("/protected/somepage").handler((rc) => {
  rc.response().end("Welcome to the protected resource!");
});

// welcome page
router.get("/").handler((ctx) => {
  ctx.response().putHeader("content-type", "text/html").end("Hello<br><a href=\"/protected/somepage\">Protected by Google</a>");
});
```

You will need to provide all the details of your provider manually but
the end result is the same.

The handler will pin your application the the configured callback url.
The usage is simple as providing the handler a route instance and all
setup will be done for you. In a typical use case your provider will ask
you what is the callback url to your application, your then enter a url
like: `https://myserver.com/callback`. This is the second argument to
the handler now you just need to set it up. To make it easier to the end
user all you need to do is call the setupCallback method.

This is how you pin your handler to the server
`https://myserver.com:8447/callback`. Note that the port number is not
mandatory for the default values, 80 for http, 443 for https.

``` js
import { OAuth2AuthHandler } from "@vertx/web"
// create a oauth2 handler pinned to myserver.com: "https://myserver.com:8447/callback"
let oauth2 = OAuth2AuthHandler.create(provider, "https://myserver.com:8447/callback");
// now allow the handler to setup the callback url for you
oauth2.setupCallback(router.route());
```

In the example the route object is created inline by `Router.route()`
however if you want to have full control of the order the handler is
called (for example you want it to be called as soon as possible in the
chain) you can always create the route object before and pass it as a
reference to this method.

## A real world example

Up to now you have learned how to use the Oauth2 Handler however you
will notice that for each request you will need to authenticate. This is
because the handler has no state and there was no state management
applied in the examples.

Although having no state is recommended for API facing endpoints, for
example, using JWT (we will cover those later) for user facing endpoinst
we can keep the authentication result stored in the session. For this to
work we would need an application like the following snippet:

``` js
import { GithubAuth } from "@vertx/auth-oauth2"
import { LocalSessionStore } from "@vertx/web"
import { SessionHandler } from "@vertx/web"
import { OAuth2AuthHandler } from "@vertx/web"
// To simplify the development of the web components
// we use a Router to route all HTTP requests
// to organize our code in a reusable way.

// Simple auth service which uses a GitHub to
// authenticate the user
let authProvider = GithubAuth.create(vertx, "YOUR PROVIDER CLIENTID", "YOUR PROVIDER CLIENT SECRET");
// We need a user session handler too to make sure
// the user is stored in the session between requests
router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)).setAuthProvider(authProvider));
// we now protect the resource under the path "/protected"
router.route("/protected").handler(OAuth2AuthHandler.create(authProvider).setupCallback(router.route("/callback")).addAuthority("user:email"));
// Entry point to the application, this will render
// a custom template.
router.get("/").handler((ctx) => {
  ctx.response().putHeader("Content-Type", "text/html").end("<html>\n  <body>\n    <p>\n      Well, hello there!\n    </p>\n    <p>\n      We're going to the protected resource, if there is no\n      user in the session we will talk to the GitHub API. Ready?\n      <a href=\"/protected\">Click here</a> to begin!</a>\n    </p>\n    <p>\n      <b>If that link doesn't work</b>, remember to provide\n      your own <a href=\"https://github.com/settings/applications/new\">\n      Client ID</a>!\n    </p>\n  </body>\n</html>");
});
// The protected resource
router.get("/protected").handler((ctx) => {
  // at this moment your user object should contain the info
  // from the Oauth2 response, since this is a protected resource
  // as specified above in the handler config the user object is never null
  let user = ctx.user();
  // just dump it to the client for demo purposes
  ctx.response().end(user.toString());
});
```

## Mixing OAuth2 and JWT

Some providers use JWT tokens as access tokens, this is a feature of
[RFC6750](https://tools.ietf.org/html/rfc6750) and can be quite useful
when one wants to mix client based authentication and API authorization.
For example say that you have a application that provides some protected
HTML documents but you also want it to be available for API’s to
consume. In this case an API cannot easily perform the redirect
handshake required by OAuth2 but can use a Token provided before hand.

This is handled automatically by the handler as long as the provider is
configured to support JWTs.

In real life this means that your API’s can access your protected
resources using the header `Authorization` with the value `Bearer
BASE64_ACCESS_TOKEN`.

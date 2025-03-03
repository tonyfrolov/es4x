# MqttAuth

MQTT authentication information

|             |          |             |
| ----------- | -------- | ----------- |
| Name        | Type     | Description |
| `@password` | `String` |             |
| `@username` | `String` |             |

# MqttClientOptions

Represents options used by the MQTT client.

|                                    |                        |                                                                               |
| ---------------------------------- | ---------------------- | ----------------------------------------------------------------------------- |
| Name                               | Type                   | Description                                                                   |
| `@autoGeneratedClientId`           | `Boolean`              | Set if the MQTT client must generate clientId automatically (default is true) |
| `@autoKeepAlive`                   | `Boolean`              | Set if the MQTT client must handle PINGREQ automatically (default is true)    |
| `@cleanSession`                    | `Boolean`              | Set to start with a clean session (or not)                                    |
| `@clientId`                        | `String`               | Set the client identifier                                                     |
| `@connectTimeout`                  | `Number (int)`         | \-                                                                            |
| `@crlPaths`                        | `Array of String`      | \-                                                                            |
| `@crlValues`                       | `Array of Buffer`      | \-                                                                            |
| `@enabledCipherSuites`             | `Array of String`      | \-                                                                            |
| `@enabledSecureTransportProtocols` | `Array of String`      | \-                                                                            |
| `@hostnameVerificationAlgorithm`   | `String`               | \-                                                                            |
| `@idleTimeout`                     | `Number (int)`         | Do the same thing as link. Use it instead.                                    |
| `@idleTimeoutUnit`                 | `TimeUnit`             | \-                                                                            |
| `@jdkSslEngineOptions`             | `JdkSSLEngineOptions`  | \-                                                                            |
| `@keepAliveTimeSeconds`            | `Number (int)`         | Set the keep alive timeout in seconds                                         |
| `@keyStoreOptions`                 | `JksOptions`           | \-                                                                            |
| `@localAddress`                    | `String`               | \-                                                                            |
| `@logActivity`                     | `Boolean`              | \-                                                                            |
| `@maxInflightQueue`                | `Number (int)`         | Set max count of unacknowledged messages                                      |
| `@maxMessageSize`                  | `Number (int)`         | Set max MQTT message size                                                     |
| `@metricsName`                     | `String`               | \-                                                                            |
| `@openSslEngineOptions`            | `OpenSSLEngineOptions` | \-                                                                            |
| `@password`                        | `String`               | Set the password                                                              |
| `@pemKeyCertOptions`               | `PemKeyCertOptions`    | \-                                                                            |
| `@pemTrustOptions`                 | `PemTrustOptions`      | \-                                                                            |
| `@pfxKeyCertOptions`               | `PfxOptions`           | \-                                                                            |
| `@pfxTrustOptions`                 | `PfxOptions`           | \-                                                                            |
| `@proxyOptions`                    | `ProxyOptions`         | \-                                                                            |
| `@receiveBufferSize`               | `Number (int)`         | \-                                                                            |
| `@reconnectAttempts`               | `Number (int)`         | \-                                                                            |
| `@reconnectInterval`               | `Number (long)`        | \-                                                                            |
| `@reuseAddress`                    | `Boolean`              | \-                                                                            |
| `@reusePort`                       | `Boolean`              | \-                                                                            |
| `@sendBufferSize`                  | `Number (int)`         | \-                                                                            |
| `@soLinger`                        | `Number (int)`         | \-                                                                            |
| `@ssl`                             | `Boolean`              | \-                                                                            |
| `@sslHandshakeTimeout`             | `Number (long)`        | \-                                                                            |
| `@sslHandshakeTimeoutUnit`         | `TimeUnit`             | \-                                                                            |
| `@tcpCork`                         | `Boolean`              | \-                                                                            |
| `@tcpFastOpen`                     | `Boolean`              | \-                                                                            |
| `@tcpKeepAlive`                    | `Boolean`              | \-                                                                            |
| `@tcpNoDelay`                      | `Boolean`              | \-                                                                            |
| `@tcpQuickAck`                     | `Boolean`              | \-                                                                            |
| `@trafficClass`                    | `Number (int)`         | \-                                                                            |
| `@trustAll`                        | `Boolean`              | \-                                                                            |
| `@trustStoreOptions`               | `JksOptions`           | \-                                                                            |
| `@useAlpn`                         | `Boolean`              | \-                                                                            |
| `@usePooledBuffers`                | `Boolean`              | \-                                                                            |
| `@username`                        | `String`               | Set the username                                                              |
| `@willFlag`                        | `Boolean`              | Set if will information are provided on connection                            |
| `@willMessage`                     | `String`               | Set the content of the will message                                           |
| `@willQoS`                         | `Number (int)`         | Set the QoS level for the will message                                        |
| `@willRetain`                      | `Boolean`              | Set if the will message must be retained                                      |
| `@willTopic`                       | `String`               | Set the topic on which the will message will be published                     |

# MqttServerOptions

Represents options used by the MQTT server

|                                    |                        |                                                                 |
| ---------------------------------- | ---------------------- | --------------------------------------------------------------- |
| Name                               | Type                   | Description                                                     |
| `@acceptBacklog`                   | `Number (int)`         | \-                                                              |
| `@autoClientId`                    | `Boolean`              | Set if clientid should be auto-generated when it's "zero-bytes" |
| `@clientAuth`                      | `ClientAuth`           | \-                                                              |
| `@clientAuthRequired`              | `Boolean`              | \-                                                              |
| `@crlPaths`                        | `Array of String`      | \-                                                              |
| `@crlValues`                       | `Array of Buffer`      | \-                                                              |
| `@enabledCipherSuites`             | `Array of String`      | \-                                                              |
| `@enabledSecureTransportProtocols` | `Array of String`      | \-                                                              |
| `@host`                            | `String`               | \-                                                              |
| `@idleTimeout`                     | `Number (int)`         | \-                                                              |
| `@idleTimeoutUnit`                 | `TimeUnit`             | \-                                                              |
| `@jdkSslEngineOptions`             | `JdkSSLEngineOptions`  | \-                                                              |
| `@keyStoreOptions`                 | `JksOptions`           | \-                                                              |
| `@logActivity`                     | `Boolean`              | \-                                                              |
| `@maxMessageSize`                  | `Number (int)`         | Set max MQTT message size                                       |
| `@openSslEngineOptions`            | `OpenSSLEngineOptions` | \-                                                              |
| `@pemKeyCertOptions`               | `PemKeyCertOptions`    | \-                                                              |
| `@pemTrustOptions`                 | `PemTrustOptions`      | \-                                                              |
| `@pfxKeyCertOptions`               | `PfxOptions`           | \-                                                              |
| `@pfxTrustOptions`                 | `PfxOptions`           | \-                                                              |
| `@port`                            | `Number (int)`         | \-                                                              |
| `@receiveBufferSize`               | `Number (int)`         | \-                                                              |
| `@reuseAddress`                    | `Boolean`              | \-                                                              |
| `@reusePort`                       | `Boolean`              | \-                                                              |
| `@sendBufferSize`                  | `Number (int)`         | \-                                                              |
| `@sni`                             | `Boolean`              | \-                                                              |
| `@soLinger`                        | `Number (int)`         | \-                                                              |
| `@ssl`                             | `Boolean`              | \-                                                              |
| `@sslHandshakeTimeout`             | `Number (long)`        | \-                                                              |
| `@sslHandshakeTimeoutUnit`         | `TimeUnit`             | \-                                                              |
| `@tcpCork`                         | `Boolean`              | \-                                                              |
| `@tcpFastOpen`                     | `Boolean`              | \-                                                              |
| `@tcpKeepAlive`                    | `Boolean`              | \-                                                              |
| `@tcpNoDelay`                      | `Boolean`              | \-                                                              |
| `@tcpQuickAck`                     | `Boolean`              | \-                                                              |
| `@timeoutOnConnect`                | `Number (int)`         | Set the timeout on CONNECT packet                               |
| `@trafficClass`                    | `Number (int)`         | \-                                                              |
| `@trustStoreOptions`               | `JksOptions`           | \-                                                              |
| `@useAlpn`                         | `Boolean`              | \-                                                              |
| `@usePooledBuffers`                | `Boolean`              | \-                                                              |

# MqttWill

Will information from the remote MQTT client

|                |                |             |
| -------------- | -------------- | ----------- |
| Name           | Type           | Description |
| `@willFlag`    | `Boolean`      |             |
| `@willMessage` | `String`       |             |
| `@willQos`     | `Number (int)` |             |
| `@willRetain`  | `Boolean`      |             |
| `@willTopic`   | `String`       |             |

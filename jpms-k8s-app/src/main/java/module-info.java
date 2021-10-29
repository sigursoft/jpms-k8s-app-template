module jpms.k8s.app {
    // http server
    requires jdk.httpserver;
    // http client
    requires java.net.http;
    // for https
    requires jdk.crypto.ec;
    // slf4j
    requires org.slf4j.jdk.platform.logging;
    requires com.fasterxml.jackson.core;
}